package com.alps.sample.sensorModule;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.alps.sample.log.Logg;
import com.alps.sample.sensorModule.command.Commander;
import com.alps.sample.sensorModule.command.control.*;
import com.alps.sample.sensorModule.command.notify.NotifyBLEConnectionParametersAdjustment;
import com.alps.sample.sensorModule.command.notify.NotifySequencerError;
import com.alps.sample.sensorModule.command.sensorData.SensorDataPacket;
import com.alps.sample.sensorModule.command.sensorData.SensorDataPacketMagAcc;
import com.alps.sample.sensorModule.command.sensorData.SensorDataPacketPreHumTemUVAmLight;
import com.alps.sample.sensorModule.enums.AccelerationSensorRange;
import com.alps.sample.sensorModule.enums.MeasuringMode;
import com.alps.sample.sensorModule.enums.MeasuringState;
import com.alps.sample.sensorModule.enums.Sensor;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;



/**
 * [JP] {@link BLEConnect}を利用してセンサモジュールと通信し、発生した様々なイベントを解釈してモジュールの状態を管理します。
 * <p>
 * {@link SensorModule#activate()}が実行されることにより、接続処理が開始されます。
 * この場合、{@link com.alps.sample.sensorModule.SensorModule.InnerSequence}に定義された順序で処理を開始し、
 * 最終的に{@link com.alps.sample.sensorModule.SensorModule.ISensorModule#onReadyCommunication(int, boolean)}を呼び出します。
 * <p>
 * 接続中は受信バイト列を解析して{@link SensorModule#latestData}を更新するとともに、
 * {@link com.alps.sample.sensorModule.SensorModule.ISensorModule}を利用してイベント通知を行います。
 */
public class SensorModule {
    private static final String TAG = "SensorModule";

    /**
     * [JP] {@link com.alps.sample.sensorModule.SensorModule.ISensorModule#onReadyCommunication(int, boolean)}を通知するまでの
     * 内部シーケンスを管理するための列挙型です。
     */
    private enum InnerSequence {
        /**
         * [JP] 初期状態であり、非活性化されていることを示します。
         * {@link SensorModule#activate()}によって、{@link com.alps.sample.sensorModule.SensorModule.InnerSequence#ActivatedConnecting}へ遷移します。
         */
        Deactivated,

        /**
         * [JP] {@link BLEConnect}がBLE接続を行っていることを示します。
         * {@link com.alps.sample.sensorModule.BLEConnect.IBLEConnect#onCharacteristicEnabled(BLEConnect.NotifierType)}で
         * {@link BLEConnect#CHAR_UUID_RX_GETTING_RESULTS}のNotificationが許可された際、
         * {@link com.alps.sample.sensorModule.SensorModule.InnerSequence#ActivatedAnalyzingCurrentSettings}へ遷移します。
         */
        ActivatedConnecting,

        /**
         * [JP] 接続完了時点のセンサモジュールの状態を、{@link SensorModule#readSettingsAll(boolean, Commander.ICommander)}によって読み出していることを示します。
         * 全ての読み出しが完了した場合、{@link com.alps.sample.sensorModule.SensorModule.InnerSequence#ActivatedEnablingSensorDataNotification}へ遷移します。
         */
        ActivatedAnalyzingCurrentSettings,

        /**
         * [JP] {@link BLEConnect#CHAR_UUID_RX_SENSOR_DATA_AND_STATUS}のNotificationを許可する処理を行っていることを示します。
         * 処理が成功した場合、{@link com.alps.sample.sensorModule.SensorModule.InnerSequence#ActivatedReady}へ遷移します。
         */
        ActivatedEnablingSensorDataNotification,

        /**
         * [JP] 通信準備が完了していることを示します。
         * この状態に遷移した際、{@link com.alps.sample.sensorModule.SensorModule.ISensorModule#onReadyCommunication(int, boolean)}が呼び出されます。
         */
        ActivatedReady,
    }

    private InnerSequence innerSequence = InnerSequence.Deactivated;

    /**
     * [JP] センサモジュールからBLEイベントを受け、
     * そのイベントを受け終わった後に通知するためのインタフェースです。
     */
    public interface ISensorModule {
        /**
         * [JP] センサモジュールとの通信準備が整ったかどうかを通知します。
         *
         * @param tag   オブジェクト生成時に指定した固有番号です。
         * @param ready trueの場合、通信要求に応じられることを意味します。
         *              falseだった場合、再度trueで通知されるまでしばらく待つ必要があります。
         * @see SensorModule#readSettingsAll(Commander.ICommander)
         * @see SensorModule#writeSettings(ArrayList, boolean, Commander.ICommander)
         */
        void onReadyCommunication(int tag, boolean ready);

        /**
         * [JP] センサデータを受け、{@link LatestData}を更新し終わった際に通知します。
         *
         * @param tag オブジェクト生成時に指定した固有番号です。
         * @see LatestData
         * @see SensorDataPacket
         */
        void onReceiveNotificationSensorData(int tag);

        /**
         * [JP] 読み込みコマンドの応答バイト列を受け、自身の各プロパティを更新し終わった際に通知します。
         *
         * @param tag オブジェクト生成時に指定した固有番号です。
         * @see SensorModule#readSettingsAll(Commander.ICommander)
         */
        void onReceiveNotificationReadingResult(int tag);

        /**
         * [JP] ステータス通知コマンドを受け、自身の各プロパティを更新し終わった際に通知します。
         *
         * @param tag オブジェクト生成時に指定した固有番号です。
         * @see SensorModule#writeSettings(ArrayList, boolean, Commander.ICommander)
         * @see CtrlCmdRequestStatus
         */
        void onReceiveNotificationStatus(int tag);

        /**
         * [JP] ステータス通知コマンドを受け、その結果にNACKが含まれていた場合に通知します。
         *
         * @param tag オブジェクト生成時に指定した固有番号です。
         */
        void onReceiveNack(int tag);
    }


    /*
     * Sensor Module Properties
     */
    public Set<Sensor> enabledSensors = new HashSet<Sensor>();
    public MeasuringState measuringState = MeasuringState.Stopped;
    public int intervalMeasuringOnModeSlow = 0;
    public int intervalMeasuringOnModeFast = 0;
    public MeasuringMode measuringMode = MeasuringMode.Slow;
    public AccelerationSensorRange accelerationSensorRange = AccelerationSensorRange.G2;
    public int intervalTimerAwakeLimit = 0;

    private static onReadBLESensorListener onReadBLESensorListener;

    public interface onReadBLESensorListener {
        public void onReadDataBLESensor(JsonObject eventLog);
    }

    public static void subscribeToListener(onReadBLESensorListener listener) {
        onReadBLESensorListener = listener;
    }

    public static void unSubscribeToListener() {
        onReadBLESensorListener = null;
    }


    /*
     * Sensor Module Sensor Data
     */
    public LatestData latestData = new LatestData();


    /*
     * Logger
     */

    private DataLogger logger;

    public boolean isLogging() {
        return logger.isLogging();
    }

    public boolean toggleLogging() {
        return logger.toggleLogging();
    }


    /*
     * Name
     */

    private String name;

    public String getName() {
        return name;
    }

    private String macAddress;

    public String getMacAdress(){
        return macAddress;
    }


    public SensorModule(Context context, BluetoothDevice bluetoothDevice, int tag, final ISensorModule iSensorModule) {
        this.tag = tag;
        this.iSensorModule = iSensorModule;
        this.context = context;

        name = String.format("%s (%s)", bluetoothDevice.getName(), bluetoothDevice.getAddress());

        macAddress = bluetoothDevice.getAddress();

        Logg.d(TAG, "new SensorModule(%s)", name);

        bleConnect = new BLEConnect(context, bluetoothDevice, iBLEConnect);
        logger = new DataLogger(context, name);

    }


    private int tag;
    private ISensorModule iSensorModule;
    private Context context;

    private BLEConnect bleConnect;
    private Commander commander;


    /**
     * [JP] 通信可能かどうかを返します。
     *
     * @return 接続完了後であり、かつ、{@link Commander}が動作していない場合にtrueとなります。
     */
    public boolean canCommunicate() {
        return (commander == null) && (innerSequence == InnerSequence.ActivatedReady);
    }

    /**
     * [JP] 全ての読み出しコマンドを実行します。
     * ただし、{@link SensorModule#canCommunicate()}がtrueでない場合、何も実行されません。
     *
     * @param iCommander {@code Commander}の処理が終わった際のコールバック先です。
     * @see com.alps.sample.sensorModule.SensorModule.ISensorModule#onReadyCommunication(int, boolean)
     * @see Commander
     * @see CtrlCmdRequestStatus
     * @see CtrlCmdMeasuringState
     * @see CtrlCmdMeasuringMode
     * @see CtrlCmdMeasuringIntervalOnModeSlow
     * @see CtrlCmdMeasuringIntervalOnModeFast
     * @see CtrlCmdSamplingSensors
     * @see CtrlCmdWakeUpConfigModeTimerWake
     * @see CtrlCmdAccelerationSensorRange
     */
    public void readSettingsAll(Commander.ICommander iCommander) {
        if (!canCommunicate()) {
            iCommander.onBatchFinish();
            return;
        }

        readSettingsAll(true, iCommander);
    }

    private void readSettingsAll(boolean addRequestStatus, Commander.ICommander iCommander) {
        bridgeICommander = iCommander;
        commander = new Commander();
        if (addRequestStatus) {
            commander.addCommand(new CtrlCmdRequestStatus());
        }
        commander.addCommand(new CtrlCmdMeasuringState());
        commander.addCommand(new CtrlCmdMeasuringMode());
        commander.addCommand(new CtrlCmdMeasuringIntervalOnModeSlow());
        commander.addCommand(new CtrlCmdMeasuringIntervalOnModeFast());
        commander.addCommand(new CtrlCmdSamplingSensors());
        commander.addCommand(new CtrlCmdWakeUpConfigModeTimerWake());
        commander.addCommand(new CtrlCmdAccelerationSensorRange());
        commander.run(Commander.RunMode.WaitResponse, this, new Commander.ICommanderCareful() {
            @Override
            public void onReceiveResponse(CtrlCmd cmdSent, CtrlCmd cmdResp) {
                // NOP
            }

            @Override
            public void onBatchFinish() {
                commander = null;
                if (bridgeICommander != null) {
                    bridgeICommander.onBatchFinish();
                }
            }
        });

        logger.requestToScanMediaForUpdatingLogFile();
    }


    private Commander.ICommander bridgeICommander = null;

    /**
     * [JP] 渡されたコマンドリストを順次実行します。
     * ただし、{@link SensorModule#canCommunicate()}がtrueでない場合、何も実行されません。
     *
     * @param commands       コマンドリストです。
     * @param breakMeasuring trueの場合、実行コマンドリストの前後に計測停止・計測再開を自動的に挿入します。
     *                       モジュールが計測動作中の場合はtrueにしてください。
     * @param iCommander     {@code Commander}の処理が終わった際のコールバック先です。
     * @see Commander
     */
    public void writeSettings(ArrayList<CtrlCmd> commands, final boolean breakMeasuring, Commander.ICommander iCommander) {
        if (!canCommunicate()) {
            iCommander.onBatchFinish();
            return;
        }

        logger.clear(this);

        bridgeICommander = iCommander;
        commander = new Commander();
        if ((breakMeasuring) && (measuringState == MeasuringState.Started)) {
            commander.addCommand(new CtrlCmdMeasuringState(MeasuringState.Stopped));
        }
        for (CtrlCmd command : commands) {
            commander.addCommand(command);
        }
        if ((breakMeasuring) && (measuringState == MeasuringState.Started)) {
            commander.addCommand(new CtrlCmdMeasuringState(MeasuringState.Started));
        }
        commander.run(Commander.RunMode.WaitResponse, this, new Commander.ICommanderCareful() {
            @Override
            public void onReceiveResponse(CtrlCmd cmdSent, CtrlCmd cmdResp) {
				/*
				  If cmdResp is CtrlCmdStatusRequest, cmdSent is a setting command from myself.
				  So I should synchronize some properties.
				 */
                if (cmdResp.eventCode() == CtrlCmdRequestStatus.EVENT_CODE_RESULT) {
                    CtrlCmdRequestStatus result = (CtrlCmdRequestStatus) cmdResp;
                    if (!result.ack) {
                        Logg.d(TAG, "[ERROR] NACK!");

                        switch (cmdSent.eventCode()) {
                            case CtrlCmdMeasuringIntervalOnModeFast.EVENT_CODE_SETTING_VALUE: {
                                intervalMeasuringOnModeFast = CtrlCmdMeasuringIntervalOnModeFast.DEFAULT;
                                break;
                            }
                            default:
                                break;
                        }

                        if (iSensorModule != null) {
                            iSensorModule.onReceiveNack(tag);
                        }
                        return;
                    } else {
                        Logg.d(TAG, "[SYNC] sentCmd : %s", cmdSent);
                    }

                    switch (cmdSent.eventCode()) {
                        case CtrlCmdSamplingSensors.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdSamplingSensors temp = (CtrlCmdSamplingSensors) cmdSent;
                            enabledSensors = temp.enabledSensors;
                            break;
                        }
                        case CtrlCmdMeasuringMode.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdMeasuringMode temp = (CtrlCmdMeasuringMode) cmdSent;
                            measuringMode = temp.measuringMode;
                            break;
                        }
                        case CtrlCmdMeasuringIntervalOnModeSlow.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdMeasuringIntervalOnModeSlow temp = (CtrlCmdMeasuringIntervalOnModeSlow) cmdSent;
                            intervalMeasuringOnModeSlow = temp.interval;
                            break;
                        }
                        case CtrlCmdMeasuringIntervalOnModeFast.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdMeasuringIntervalOnModeFast temp = (CtrlCmdMeasuringIntervalOnModeFast) cmdSent;
                            intervalMeasuringOnModeFast = temp.interval;
                            break;
                        }
                        case CtrlCmdMeasuringState.EVENT_CODE_SETTING_VALUE: {
                            if (measuringMode == MeasuringMode.Force) {
                                // NOP
                            } else {
                                CtrlCmdMeasuringState temp = (CtrlCmdMeasuringState) cmdSent;
                                measuringState = temp.measuringState;
                            }
                            break;
                        }
                        case CtrlCmdWakeUpConfigModeTimerWake.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdWakeUpConfigModeTimerWake temp = (CtrlCmdWakeUpConfigModeTimerWake) cmdSent;
                            intervalTimerAwakeLimit = temp.interval;
                            break;
                        }
                        case CtrlCmdAccelerationSensorRange.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdAccelerationSensorRange temp = (CtrlCmdAccelerationSensorRange) cmdSent;
                            accelerationSensorRange = temp.range;
                            break;
                        }
                        case CtrlCmdTimeStamp.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdTimeStamp temp = (CtrlCmdTimeStamp) cmdSent;
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(temp.date);

                            int day = calendar.get(Calendar.DAY_OF_MONTH);
                            int month = calendar.get(Calendar.MONTH) + 1;
                            int year = calendar.get(Calendar.YEAR);

                            latestData.day = day;
                            latestData.month = month;
                            latestData.year = year;
                            break;
                        }
                        default:
                            break;
                    }
                }
            }

            @Override
            public void onBatchFinish() {
                commander = null;
                if (bridgeICommander != null) {
                    bridgeICommander.onBatchFinish();
                }
            }
        });

        logger.requestToScanMediaForUpdatingLogFile();
    }


    private BLEConnect.ConnectionState lastState = null;
    private BLEConnect.IBLEConnect iBLEConnect = new BLEConnect.IBLEConnect() {
        @Override
        public void onConnectionStateChange(BLEConnect.ConnectionState connectionState) {
            boolean shouldNotify = false;
            switch (connectionState) {
                case Disconnected:
                    if (commander != null) {
                        commander.abort();
                    }
                    innerSequence = InnerSequence.ActivatedConnecting;
                    shouldNotify = true;
                    break;
                case Connected:
                    break;
                case FatalError:
                    if (commander != null) {
                        commander.abort();
                    }
                    innerSequence = InnerSequence.Deactivated;
                    shouldNotify = true;
                    break;
            }

            if ((!connectionState.equals(lastState)) && (shouldNotify) && (iSensorModule != null)) {
                iSensorModule.onReadyCommunication(tag, (innerSequence == InnerSequence.ActivatedReady));
            }

            lastState = connectionState;
        }

        @Override
        public void onCharacteristicEnabled(BLEConnect.NotifierType type) {
            Logg.d(TAG, "onCharacteristicEnabled=%s, innerSequence=%s", type, innerSequence);

            switch (innerSequence) {
                case ActivatedConnecting:
                    if (type == BLEConnect.NotifierType.GettingResults) {
                        innerSequence = InnerSequence.ActivatedAnalyzingCurrentSettings;
                        readSettingsAll(false, new Commander.ICommander() {
                            @Override
                            public void onBatchFinish() {
                                innerSequence = InnerSequence.ActivatedEnablingSensorDataNotification;
                                bleConnect.enableCharacteristicNotification(BLEConnect.NotifierType.SensorDataAndStatus);
                            }
                        });
                    } else {
                        Logg.d(TAG, "[ERROR] unexpected sequence");
                        disconnect();
                    }
                    break;
                case ActivatedEnablingSensorDataNotification:
                    if (type == BLEConnect.NotifierType.SensorDataAndStatus) {
                        commander = new Commander();
                        commander.addCommand(new CtrlCmdRequestStatus());
                        commander.run(Commander.RunMode.WaitResponse, SensorModule.this, new Commander.ICommanderCareful() {
                            @Override
                            public void onReceiveResponse(CtrlCmd cmdSent, CtrlCmd cmdResp) {
                                // NOP
                            }

                            @Override
                            public void onBatchFinish() {
                                commander = null;
                            }
                        });

                        /**
                         * Ready for user-communication
                         */
                        innerSequence = InnerSequence.ActivatedReady;
                        if (iSensorModule != null) {
                            iSensorModule.onReadyCommunication(tag, true);
                        }
                    } else {
                        Logg.d(TAG, "[ERROR] unexpected sequence");
                        disconnect();
                    }
                    break;
                default:
                    Logg.d(TAG, "[ERROR] onCharacteristicEnabled when unexpected innerSequence = %s", innerSequence);
                    disconnect();
                    break;
            }
        }

        @Override
        public void onReceiveNotification(BLEConnect.NotifierType type, byte[] bytes) {
            switch (type) {
                case SensorDataAndStatus: {
                    boolean shouldNotify = true;
                    SensorDataPacket sensorData = null;

                    switch (bytes[SensorDataPacket.INDEX_EVENT_CODE]) {
                        case SensorDataPacketMagAcc.EVENT_CODE: {
                            SensorDataPacketMagAcc temp = new SensorDataPacketMagAcc(bytes);
                            latestData.magX = Formula.calc(Sensor.Magnetic, temp.getMagX());
                            latestData.magY = Formula.calc(Sensor.Magnetic, temp.getMagY());
                            latestData.magZ = Formula.calc(Sensor.Magnetic, temp.getMagZ());
                            latestData.accX = Formula.calcAcc(temp.getAccX(), accelerationSensorRange);
                            latestData.accY = Formula.calcAcc(temp.getAccY(), accelerationSensorRange);
                            latestData.accZ = Formula.calcAcc(temp.getAccZ(), accelerationSensorRange);
                            latestData.ms = temp.getMs();
                            latestData.second = temp.getSecond();
                            latestData.minute = temp.getMinute();
                            latestData.hour = temp.getHour();
                            sensorData = temp;
                            break;
                        }
                        case SensorDataPacketPreHumTemUVAmLight.EVENT_CODE: {
                            SensorDataPacketPreHumTemUVAmLight temp = new SensorDataPacketPreHumTemUVAmLight(bytes);
                            latestData.pressure = Formula.calc(Sensor.Pressure, temp.getPressure());
                            latestData.humidity = Formula.calc(Sensor.Humidity, temp.getHumidity());
                            latestData.temperature = Formula.calc(Sensor.Temperature, temp.getTemperature());
                            latestData.uv = Formula.calc(Sensor.UV, temp.getUv());
                            latestData.ambientLight = Formula.calc(Sensor.AmbientLight, temp.getAmbientLight());
                            latestData.day = temp.getDay();
                            latestData.month = temp.getMonth();
                            latestData.year = temp.getYear();
                            sensorData = temp;
                            break;
                        }
                        case CtrlCmdRequestStatus.EVENT_CODE_RESULT: {
                            CtrlCmdRequestStatus status = new CtrlCmdRequestStatus(bytes);
                            Logg.d(TAG, "recvStatus : %s", status);
                            latestData.batteryVoltage = status.batteryVoltage;

                            if (!status.autoNotify) {
                                bleConnect.cancelTimer();
                                if (commander != null) {
                                    commander.receiveResponse(status);
                                }
                            }
                            if ((innerSequence == InnerSequence.ActivatedReady) && (shouldNotify) && (iSensorModule != null)) {
                                iSensorModule.onReceiveNotificationStatus(tag);
                            }
                            break;
                        }
                        default: {
                            Logg.d(TAG, "[ERROR] unknown packet : 0x%02X", bytes[SensorDataPacket.INDEX_EVENT_CODE]);
                            shouldNotify = false;
                            break;
                        }
                    }

                    if (sensorData != null) {
                        Logg.d(TAG, "recvSensorData : %s", sensorData);
                        Log.v(TAG, "Sensor DATA PUNTO DE EXTRRACCION");

                        latestData.index = sensorData.getIndex();
                        logger.writeSensorData(SensorModule.this);

                        Log.v(TAG, String.valueOf(latestData.accX));
                        Log.v(TAG, String.valueOf(latestData.accY));
                        Log.v(TAG, String.valueOf(latestData.accZ));
                        Log.v("SENSOR",getMacAdress().toString());

                        JsonObject message = new JsonObject();
                        try {
                            message.addProperty("deviceId", getMacAdress().toString());
                            message.addProperty("deviceName", "ALPS");
                            //En devices ficticios no es necesario enviar protocolo pero si se crea uno podría ser Bluetooth
                            //message.addProperty("protocol", "bluetooth");
                            message.addProperty("AccX", latestData.accX);
                            message.addProperty("AccY" , latestData.accY);
                            message.addProperty("AccZ", latestData.accZ);
                            message.addProperty("MagX" , latestData.magX);
                            message.addProperty("MagY", latestData.magY);
                            message.addProperty("MagZ", latestData.magZ);
                            message.addProperty("Battery", latestData.batteryVoltage);
                            message.addProperty("AmbientLight", latestData.ambientLight);
                            message.addProperty("temperature", latestData.temperature);
                            message.addProperty("humidity" , latestData.humidity);
                            message.addProperty("pressure", latestData.pressure);
                            message.addProperty("UV", latestData.uv);

                        } catch (JsonIOException e) {
                            e.printStackTrace();
                        }


                        if (onReadBLESensorListener != null)
                            onReadBLESensorListener.onReadDataBLESensor(message);


                        if (shouldNotify) {
                            if (iSensorModule != null) {
                                iSensorModule.onReceiveNotificationSensorData(tag);
                            }
                        }
                    }
                    break;
                }
                case GettingResults: {
                    boolean shouldNotifyToCommander = true;
                    boolean shouldNotifyToCallback = true;
                    boolean isReadingResult = true;
                    CtrlCmd ctrlCmd = null;

                    switch (bytes[CtrlCmd.INDEX_EVENT_CODE]) {
                        case CtrlCmdSamplingSensors.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdSamplingSensors temp = new CtrlCmdSamplingSensors(bytes);
                            enabledSensors = temp.enabledSensors;
                            ctrlCmd = temp;
                            break;
                        }
                        case CtrlCmdMeasuringMode.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdMeasuringMode temp = new CtrlCmdMeasuringMode(bytes);
                            measuringMode = temp.measuringMode;
                            ctrlCmd = temp;
                            break;
                        }
                        case CtrlCmdMeasuringIntervalOnModeSlow.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdMeasuringIntervalOnModeSlow temp = new CtrlCmdMeasuringIntervalOnModeSlow(bytes);
                            intervalMeasuringOnModeSlow = temp.interval;
                            ctrlCmd = temp;
                            break;
                        }
                        case CtrlCmdMeasuringIntervalOnModeFast.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdMeasuringIntervalOnModeFast temp = new CtrlCmdMeasuringIntervalOnModeFast(bytes);
                            intervalMeasuringOnModeFast = temp.interval;
                            ctrlCmd = temp;
                            break;
                        }
                        case CtrlCmdMeasuringState.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdMeasuringState temp = new CtrlCmdMeasuringState(bytes);
                            measuringState = temp.measuringState;
                            ctrlCmd = temp;
                            break;
                        }
                        case CtrlCmdAccelerationSensorRange.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdAccelerationSensorRange temp = new CtrlCmdAccelerationSensorRange(bytes);
                            accelerationSensorRange = temp.range;
                            ctrlCmd = temp;
                            break;
                        }
                        case CtrlCmdWakeUpConfigModeTimerWake.EVENT_CODE_SETTING_VALUE: {
                            CtrlCmdWakeUpConfigModeTimerWake temp = new CtrlCmdWakeUpConfigModeTimerWake(bytes);
                            intervalTimerAwakeLimit = temp.interval;
                            ctrlCmd = temp;
                            break;
                        }
                        case NotifyBLEConnectionParametersAdjustment.EVENT_CODE_NOTIFY_VALUE: {
                            NotifyBLEConnectionParametersAdjustment temp = new NotifyBLEConnectionParametersAdjustment(bytes);
                            ctrlCmd = temp;
                            shouldNotifyToCommander = false;
                            break;
                        }
                        case NotifySequencerError.EVENT_CODE_NOTIFY_VALUE: {
                            NotifySequencerError temp = new NotifySequencerError(bytes);
                            ctrlCmd = temp;
                            shouldNotifyToCommander = false;
                            break;
                        }
                        default: {
                            Logg.d(TAG, "[ERROR] unknown command : 0x%02X", bytes[CtrlCmd.INDEX_EVENT_CODE]);
                            shouldNotifyToCommander = false;
                            shouldNotifyToCallback = false;
                            break;
                        }
                    }

                    Logg.d(TAG, "recvCmd : %s", ctrlCmd);

                    if ((commander != null) && (shouldNotifyToCommander)) {
                        commander.receiveResponse(ctrlCmd);
                    }

                    if ((innerSequence == InnerSequence.ActivatedReady) && (shouldNotifyToCallback) && (iSensorModule != null)) {
                        if (isReadingResult) {
                            iSensorModule.onReceiveNotificationReadingResult(tag);
                        } else {
                            iSensorModule.onReceiveNotificationStatus(tag);
                        }
                    }
                    break;
                }
            }
        }
    };


    public int getTag() {
        return tag;
    }

    public boolean isConnected() {
        return (innerSequence == InnerSequence.ActivatedReady);
    }

    /**
     * [JP] センサモジュールとの通信を活性化します。
     */
    public void activate() {
        Logg.d(TAG, "activate (%s)", name);
        if (innerSequence == InnerSequence.Deactivated) {
            innerSequence = InnerSequence.ActivatedConnecting;

            bleConnect.connect();
        }

        logger.requestToScanMediaForUpdatingLogFile();
    }

    /**
     * [JP] センサモジュールとの通信を非活性化します。
     * <p>
     * 通信は切断されます。
     */
    public void deactivate() {
        Logg.d(TAG, "deactivate (%s)", name);
        innerSequence = InnerSequence.Deactivated;
        logger.release(this);

        bridgeICommander = null;

        bleConnect.stop();

        if (commander != null) {
            commander.abort();
        }

        logger.requestToScanMediaForUpdatingLogFile();
    }

    /**
     * [JP] 意図的に切断を実行します。
     * <p>
     * ただし、{@link BLEConnect}が再接続を許可する状態になっている場合、すぐに接続動作に入ってしまいます。
     * 永久に切断状態を保ちたい場合は、{@link SensorModule#deactivate()}を使用してください。
     */
    public void disconnect() {
        bleConnect.disconnect();
        logger.requestToScanMediaForUpdatingLogFile();
    }


    /**
     * [JP] 与えられたコマンドオブジェクトをバイト列化し、BLEで送信します。
     *
     * @param ctrlCmd 送信するコマンドです。
     * @see CtrlCmd#getBytes()
     * @see CtrlCmd#getShouldFireTimeoutTimer()
     * @see BLEConnect#writeCharacteristic(byte[], boolean)
     */
    public void sendCommand(CtrlCmd ctrlCmd) {
        if (ctrlCmd.isValid()) {
            byte[] bytes = ctrlCmd.getBytes();
            Logg.d(TAG, "[SEND] sendCommand : %s", ctrlCmd);
            bleConnect.writeCharacteristic(bytes, ctrlCmd.getShouldFireTimeoutTimer());
        } else {
            Logg.d(TAG, "[ERROR] command is not valid!");
        }
    }




}
