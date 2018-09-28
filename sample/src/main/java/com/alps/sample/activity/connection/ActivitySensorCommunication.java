
package com.alps.sample.activity.connection;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import com.alps.sample.R;
import com.alps.sample.activity.base.usingBluetooth.ActivityUsingBluetooth;
import com.alps.sample.activity.base.view.LinearLayoutDetectableSoftKey;
import com.alps.sample.constants.Constants;
import com.alps.sample.log.Logg;
import com.alps.sample.sensorModule.command.Commander;
import com.alps.sample.sensorModule.SensorModule;
import com.alps.sample.sensorModule.command.control.*;
import com.alps.sample.sensorModule.enums.MeasuringMode;
import com.alps.sample.sensorModule.enums.MeasuringState;
import com.alps.sample.sensorModule.enums.Sensor;
import com.alps.sample.sensorModule.enums.AwakeMode;
import com.example.google.nodejsmanager.nodejsmanager.ConnectionManager;
import com.example.google.nodejsmanager.nodejsmanager.SocketManager;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

import static com.alps.sample.R.id.setting_item_switch_ambient_light;
import static java.util.concurrent.Executors.newScheduledThreadPool;

/**
 * [JP] 渡された{@code BluetoothDevice}オブジェクトで{@link SensorModule}オブジェクトを生成し、
 * このオブジェクトから通知される様々な受信イベントを表示します。
 * <p>
 * また、様々なボタン押下をトリガーに、センサモジュールに対してBLE通信コマンドを発行したり、
 * 受信したセンサデータのログを記録させます。
 *
 * @see SensorModule
 * @see com.alps.sample.sensorModule.SensorModule.ISensorModule
 * @see com.alps.sample.sensorModule.LatestData
 */
@SuppressLint("NewApi")
public class ActivitySensorCommunication extends ActivityUsingBluetooth implements ConnectionManager.EventCallbackListener, SensorModule.onReadBLESensorListener {
    public static final int INDEX_MEASURING_MODE_SLOW = 0;
    public static final int INDEX_MEASURING_MODE_FAST = 1;
    public static final int INDEX_MEASURING_MODE_HYBRID = 2;
    public static final int INDEX_MEASURING_MODE_FORCE = 3;

    private final String TAG = getClass().getSimpleName();

    public static final String EXTRAS_DEVICES = "DEVICES";

    private LinearLayoutDetectableSoftKey linearLayoutDetectableSoftKey;
    private RelativeLayout layoutMain;
    private LinearLayout layoutMask;

    private List<SensorModule> sensorModules;

    private Spinner spinnerTargetNode;

    private Button buttonSettingsRead;
    private Button buttonSettingsWrite;
    private Button buttonSleep;
    private Button buttonSyncTimestamp;
    private Button buttonMeasure;
    private Button buttonLog;

    private ScrollView wrapperTextInfoData;
    private TextView textInfoData;
    private ImageView iconBattery;

    private SocketManager socketManager;
    private int connectionIntents = 0;
    private ScheduledExecutorService schedulePingViot;
    private  SharedPreferences preferences;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logg.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // get extra value
        int tagCount = 0;
        final Intent intent = getIntent();
        Parcelable[] parcelables = intent.getParcelableArrayExtra(EXTRAS_DEVICES);
        sensorModules = new ArrayList<SensorModule>();
        if (parcelables != null) {
            for (Parcelable parcelable : parcelables) {
                if (parcelable instanceof BluetoothDevice) {
                    SensorModule sensorModule = new SensorModule(this, (BluetoothDevice) parcelable, tagCount, iSensorModule);
                    sensorModules.add(sensorModule);
                    ++tagCount;
                }
            }
        }
        if (tagCount == 0) {
            finish();
        }

        setContentView(R.layout.activity_sensor_communication);

        // set the view
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

        // set the tool-bar to this activity
        Toolbar mToolbar = (Toolbar) findViewById(R.id.header);
        mToolbar.setTitle("Communication Activity");
        setSupportActionBar(mToolbar);

        linearLayoutDetectableSoftKey = (LinearLayoutDetectableSoftKey) findViewById(R.id.detectable_layout);
        linearLayoutDetectableSoftKey.setOnDetectSoftKeyboard(new LinearLayoutDetectableSoftKey.OnSoftKeyShownListener() {
            @Override
            public void onSoftKeyShown(boolean isShown) {
                if (isShown) {
                    wrapperTextInfoData.setVisibility(View.GONE);
                    buttonLog.setVisibility(View.GONE);
                } else {
                    wrapperTextInfoData.setVisibility(View.VISIBLE);
                    buttonLog.setVisibility(View.GONE);
                }
            }
        });

        layoutMain = (RelativeLayout) findViewById(R.id.main_view);

        layoutMask = (LinearLayout) findViewById(R.id.mask_view);
        layoutMask.setVisibility(View.VISIBLE);
        layoutMask.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        wrapperTextInfoData = (ScrollView) findViewById(R.id.wrapper_text_info_data);
        textInfoData = (TextView) findViewById(R.id.text_info_data);

        buttonSettingsRead = (Button) findViewById(R.id.button_setting_read);
        buttonSettingsRead.setOnClickListener(onClickListenerButtonRead);
        buttonSettingsWrite = (Button) findViewById(R.id.button_setting_write);
        buttonSettingsWrite.setOnClickListener(onClickListenerButtonWrite);
        buttonSyncTimestamp = (Button) findViewById(R.id.button_sync_timestamp);
        buttonSyncTimestamp.setOnClickListener(onClickListenerButtonSyncTimestamp);
        buttonSleep = (Button) findViewById(R.id.button_sleep);
        buttonSleep.setOnClickListener(onClickListenerButtonSleep);
        buttonMeasure = (Button) findViewById(R.id.button_measure);
        buttonMeasure.setOnClickListener(onClickListenerButtonMeasure);
        buttonLog = (Button) findViewById(R.id.button_logging);
        buttonLog.setOnClickListener(onClickListenerButtonLog);

        ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (SensorModule sensorModule : sensorModules) {
            adapter.add(sensorModule.getName());
        }
        spinnerTargetNode = (Spinner) findViewById(R.id.spinner_device);
        spinnerTargetNode.setAdapter(adapter);
        spinnerTargetNode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAllViews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Spinner spinnerMeasuringMode = (Spinner) findViewById(R.id.setting_item_edit_measuring_mode);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.insert(getString(R.string.measuring_mode_slow), INDEX_MEASURING_MODE_SLOW);
        adapter.insert(getString(R.string.measuring_mode_fast), INDEX_MEASURING_MODE_FAST);
        adapter.insert(getString(R.string.measuring_mode_hybrid), INDEX_MEASURING_MODE_HYBRID);
        adapter.insert(getString(R.string.measuring_mode_force), INDEX_MEASURING_MODE_FORCE);
        spinnerMeasuringMode.setAdapter(adapter);

        iconBattery = (ImageView) findViewById(R.id.icon_status_battery);
        iconBattery.setImageResource(R.drawable.battery_unknown);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        for (SensorModule sensorModule : sensorModules) {
            sensorModule.activate();
        }
        connectSocketViot();
    }

    @Override
    protected void onStart() {
        Logg.d(TAG, "onStart");
        super.onStart();
        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        updateAllViews();
    }

    @Override
    protected void onResume() {
        Logg.d(TAG, "onResume");
        super.onResume();
        SensorModule.subscribeToListener(this);
        ConnectionManager.subscribeToListener(this);
    }

    @Override
    protected void onPause() {
        Logg.d(TAG, "onPause");
        super.onPause();
        SensorModule.unSubscribeToListener();
        ConnectionManager.unSubscribeToListener();
    }

    @Override
    protected void onStop() {
        Logg.d(TAG, "onStop");
        super.onStop();
        // clear screen on flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (socketManager.getSocket() != null)
            socketManager.getSocket().disconnect();
    }

    @Override
    protected void onDestroy() {
        Logg.d(TAG, "onDestroy");
        super.onDestroy();

        if (sensorModules != null) {
            for (SensorModule sensorModule : sensorModules) {
                sensorModule.deactivate();
            }
            sensorModules.clear();
            sensorModules = null;
        }
    }

    @Override
    protected void onChangeBluetoothState(boolean on) {
        // NOP
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.msg_finish_ble_connection));
            builder.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivitySensorCommunication.this.finish();
                }
            });
            builder.setNegativeButton(R.string.dialog_button_cancel, null);
            builder.setCancelable(false);
            builder.show();
        }
        return super.dispatchKeyEvent(event);
    }

    private View.OnClickListener onClickListenerButtonRead = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final SensorModule sensorModule = getCurrentSensorModule();
            if (sensorModule == null) {
                return;
            }
            if (!sensorModule.canCommunicate()) {
                return;
            }

            layoutMask.requestFocus();
            layoutMask.setVisibility(View.VISIBLE);

            sensorModule.readSettingsAll(new Commander.ICommander() {
                @Override
                public void onBatchFinish() {
                    updateParameters(sensorModule);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            layoutMask.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            });
        }
    };

    private View.OnClickListener onClickListenerButtonWrite = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final SensorModule sensorModule = getCurrentSensorModule();

            if (sensorModule == null) {
                return;
            }

            if (!sensorModule.canCommunicate()) {
                return;
            }

            layoutMask.requestFocus();
            layoutMask.setVisibility(View.VISIBLE);

            /*
             * Gather all settings from textViews, switches and spinners.
             */

            Spinner spinnerMeasuringMode = (Spinner) findViewById(R.id.setting_item_edit_measuring_mode);
            int position = spinnerMeasuringMode.getSelectedItemPosition();
            MeasuringMode measuringMode;
            switch (position) {
                case INDEX_MEASURING_MODE_SLOW:
                    measuringMode = MeasuringMode.Slow;
                    break;
                case INDEX_MEASURING_MODE_FAST:
                    measuringMode = MeasuringMode.Fast;
                    break;
                case INDEX_MEASURING_MODE_FORCE:
                    measuringMode = MeasuringMode.Force;
                    break;
                case INDEX_MEASURING_MODE_HYBRID:
                    measuringMode = MeasuringMode.Hybrid;
                    break;
                default:
                    measuringMode = MeasuringMode.Slow;
                    break;
            }

            int intervalMeasuringOnModeSlow = getValueFromTextViewResource(R.id.setting_item_edit_interval_on_mode_slow, CtrlCmdMeasuringIntervalOnModeSlow.MIN, CtrlCmdMeasuringIntervalOnModeSlow.MAX, CtrlCmdMeasuringIntervalOnModeSlow.DEFAULT);
            int intervalMeasuringOnModeFast = getValueFromTextViewResource(R.id.setting_item_edit_interval_on_mode_fast, CtrlCmdMeasuringIntervalOnModeFast.MIN, CtrlCmdMeasuringIntervalOnModeFast.MAX, CtrlCmdMeasuringIntervalOnModeFast.DEFAULT);

            Switch sw;
            Set<Sensor> enabledSensors = new LinkedHashSet<Sensor>();
            sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_magnetic);
            if (sw.isChecked()) enabledSensors.add(Sensor.Magnetic);
            sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_acceleration);
            if (sw.isChecked()) enabledSensors.add(Sensor.Acceleration);
            sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_pressure);
            if (sw.isChecked()) enabledSensors.add(Sensor.Pressure);
            sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_humidity);
            if (sw.isChecked()) enabledSensors.add(Sensor.Humidity);
            sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_temperature);
            if (sw.isChecked()) enabledSensors.add(Sensor.Temperature);
            sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_uv);
            if (sw.isChecked()) enabledSensors.add(Sensor.UV);
            sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_ambient_light);
            if (sw.isChecked()) enabledSensors.add(Sensor.AmbientLight);

            int intervalSleepOnTimerMode = getValueFromTextViewResource(R.id.setting_item_sleep_interval_on_timer_mode, CtrlCmdWakeUpConfigModeTimerWake.MIN, CtrlCmdWakeUpConfigModeTimerWake.MAX, CtrlCmdWakeUpConfigModeTimerWake.MIN);

            ArrayList<CtrlCmd> commands = new ArrayList<CtrlCmd>();
            commands.add(new CtrlCmdMeasuringMode(measuringMode));
            if (intervalMeasuringOnModeFast <= 100) {
                commands.add(new CtrlCmdSamplingSensors(enabledSensors));
                commands.add(new CtrlCmdMeasuringIntervalOnModeFast(intervalMeasuringOnModeFast));
            } else {
                commands.add(new CtrlCmdMeasuringIntervalOnModeFast(intervalMeasuringOnModeFast));
                commands.add(new CtrlCmdSamplingSensors(enabledSensors));
            }

            commands.add(new CtrlCmdMeasuringIntervalOnModeSlow(intervalMeasuringOnModeSlow));
            commands.add(new CtrlCmdWakeUpConfigModeTimerWake(intervalSleepOnTimerMode));

            sensorModule.writeSettings(commands, true, new Commander.ICommander() {
                @Override
                public void onBatchFinish() {
                    updateParameters(sensorModule);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            layoutMask.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            });
        }
    };

    private View.OnClickListener onClickListenerButtonSyncTimestamp = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final SensorModule sensorModule = getCurrentSensorModule();

            if (sensorModule == null) {
                return;
            }

            if (!sensorModule.canCommunicate()) {
                return;
            }

            layoutMask.requestFocus();
            layoutMask.setVisibility(View.VISIBLE);

            ArrayList<CtrlCmd> commands = new ArrayList<CtrlCmd>();
            commands.add(new CtrlCmdTimeStamp(new Date()));
            sensorModule.writeSettings(commands, true, new Commander.ICommander() {
                @Override
                public void onBatchFinish() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            layoutMask.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            });
        }
    };

    private void execSleep(AwakeMode awakeMode) {
        final SensorModule sensorModule = getCurrentSensorModule();
        if (sensorModule != null) {
            layoutMask.requestFocus();
            layoutMask.setVisibility(View.VISIBLE);

            ArrayList<CtrlCmd> commands = new ArrayList<CtrlCmd>();
            if (sensorModule.measuringState == MeasuringState.Started) {
                commands.add(new CtrlCmdMeasuringState(MeasuringState.Stopped));
            }
            commands.add(new CtrlCmdSleep(awakeMode));
            sensorModule.writeSettings(commands, false, null);
        }
    }

    private View.OnClickListener onClickListenerButtonSleep = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ActivitySensorCommunication.this);
            builder.setMessage(getString(R.string.dialog_sleep_explain));
            builder.setNeutralButton(getString(R.string.dialog_button_cancel), null);
            builder.setNegativeButton(getString(R.string.awake_mode_timer), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    execSleep(AwakeMode.Timer);
                }
            });
            builder.setPositiveButton(getString(R.string.awake_mode_accel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    execSleep(AwakeMode.Accel);
                }
            });
            builder.setCancelable(false);
            builder.show();
        }
    };

    private View.OnClickListener onClickListenerButtonMeasure = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final SensorModule sensorModule = getCurrentSensorModule();
            if (sensorModule != null) {
                MeasuringState nextMeasuringState = MeasuringState.Started;
                switch (sensorModule.measuringMode) {
                    case Slow:
                    case Fast:
                    case Hybrid:
                        nextMeasuringState = (sensorModule.measuringState == MeasuringState.Started) ? MeasuringState.Stopped : MeasuringState.Started;
                        break;
                    case Force:
                        nextMeasuringState = MeasuringState.Started;
                        break;
                }

                layoutMask.requestFocus();
                layoutMask.setVisibility(View.VISIBLE);

                ArrayList<CtrlCmd> commands = new ArrayList<CtrlCmd>();
                commands.add(new CtrlCmdMeasuringState(nextMeasuringState));
                sensorModule.writeSettings(commands, false, new Commander.ICommander() {
                    @Override
                    public void onBatchFinish() {
                        updateParameters(sensorModule);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                layoutMask.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                });
            }
        }
    };

    private View.OnClickListener onClickListenerButtonLog = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SensorModule sensorModule = getCurrentSensorModule();
            sensorModule.toggleLogging();
            updateButtonLogging(sensorModule);
        }
    };

    private int getValueFromTextViewResource(int resource, int min, int max, int defaultValue) {
        String text;
        int value = defaultValue;
        TextView tv = (TextView) layoutMain.findViewById(resource);
        if (tv != null) {
            text = tv.getText().toString().trim();
            try {
                value = Integer.parseInt(text);
                if (value < min) {
                    value = defaultValue;
                } else if (value > max) {
                    value = defaultValue;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    private SensorModule getCurrentSensorModule() {
        int position = spinnerTargetNode.getSelectedItemPosition();
        SensorModule target;
        if (sensorModules == null) {
            return null;
        }
        if (position < sensorModules.size()) {
            target = sensorModules.get(position);
        } else {
            target = null;
        }
        return target;
    }

    private void updateAllViews() {
        Logg.d(TAG, "updateAllViews");
        SensorModule sensorModule = getCurrentSensorModule();
        updateButtonLogging(sensorModule);
        updateConnectionStatus(sensorModule);
        updateParameters(sensorModule);
        updateSensorData(sensorModule);
        updateStatus(sensorModule);
    }

    private void updateButtonLogging(final SensorModule sensorModule) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (sensorModule != null) {
                    if (!sensorModule.isLogging()) {
                        buttonLog.setText(getString(R.string.button_logging_on));
                    } else {
                        buttonLog.setText(getString(R.string.button_logging_off));
                    }
                }
            }
        });
    }

    private void updateConnectionStatus(final SensorModule sensorModule) {
        Logg.d(TAG, "updateConnectionStatus");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isConnected = false;
                MeasuringState measuringState = MeasuringState.Stopped;
                MeasuringMode mode = MeasuringMode.Slow;

                if (sensorModule != null) {
                    isConnected = sensorModule.isConnected();
                    measuringState = sensorModule.measuringState;
                    mode = sensorModule.measuringMode;
                } else {
                    Logg.d(TAG, "[ERROR] sensorModule == null");
                }

                layoutMask.setVisibility(isConnected ? View.INVISIBLE : View.VISIBLE);
                switch (mode) {
                    case Slow:
                    case Fast:
                    case Hybrid:
                        buttonMeasure.setText((measuringState == MeasuringState.Started) ? getString(R.string.button_measure_off) : getString(R.string.button_measure_on));
                        break;
                    case Force:
                        buttonMeasure.setText(getString(R.string.button_measure_force));
                        break;
                }

                layoutMask.requestFocus();
            }
        });
    }

    private void updateParameters(final SensorModule sensorModule) {
        Logg.d(TAG, "updateParameters");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv;
                Switch sw;
                Spinner spinner;

                if (sensorModule == null) {
                    Logg.d(TAG, "[ERROR] sensorModule == null");
                    return;
                }

                spinner = (Spinner) findViewById(R.id.setting_item_edit_measuring_mode);
                int selectionIndex;
                switch (sensorModule.measuringMode) {
                    case Slow:
                        selectionIndex = INDEX_MEASURING_MODE_SLOW;
                        break;
                    case Fast:
                        selectionIndex = INDEX_MEASURING_MODE_FAST;
                        break;
                    case Force:
                        selectionIndex = INDEX_MEASURING_MODE_FORCE;
                        break;
                    case Hybrid:
                        selectionIndex = INDEX_MEASURING_MODE_HYBRID;
                        break;
                    default:
                        selectionIndex = INDEX_MEASURING_MODE_SLOW;
                        break;
                }
                spinner.setSelection(selectionIndex);

                tv = (TextView) layoutMain.findViewById(R.id.setting_item_edit_interval_on_mode_slow);
                tv.setText(String.format("%d", sensorModule.intervalMeasuringOnModeSlow));

                tv = (TextView) layoutMain.findViewById(R.id.setting_item_edit_interval_on_mode_fast);
                tv.setText(String.format("%d", sensorModule.intervalMeasuringOnModeFast));

                sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_magnetic);
                sw.setChecked(sensorModule.enabledSensors.contains(Sensor.Magnetic));
                sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_acceleration);
                sw.setChecked(sensorModule.enabledSensors.contains(Sensor.Acceleration));
                sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_pressure);
                sw.setChecked(sensorModule.enabledSensors.contains(Sensor.Pressure));
                sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_humidity);
                sw.setChecked(sensorModule.enabledSensors.contains(Sensor.Humidity));
                sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_temperature);
                sw.setChecked(sensorModule.enabledSensors.contains(Sensor.Temperature));
                sw = (Switch) layoutMain.findViewById(R.id.setting_item_switch_uv);
                sw.setChecked(sensorModule.enabledSensors.contains(Sensor.UV));
                sw = (Switch) layoutMain.findViewById(setting_item_switch_ambient_light);
                sw.setChecked(sensorModule.enabledSensors.contains(Sensor.AmbientLight));

                tv = (TextView) layoutMain.findViewById(R.id.setting_item_sleep_interval_on_timer_mode);
                tv.setText(String.format("%d", sensorModule.intervalTimerAwakeLimit));

                int buttonMeasureTitleResource = 0;
                switch (sensorModule.measuringMode) {
                    case Slow:
                    case Fast:
                    case Hybrid: {
                        buttonMeasureTitleResource = (sensorModule.measuringState == MeasuringState.Started) ? R.string.button_measure_off : R.string.button_measure_on;
                        break;
                    }
                    case Force:
                        buttonMeasureTitleResource = R.string.button_measure_force;
                        break;
                }
                buttonMeasure.setText(getResources().getText(buttonMeasureTitleResource));

                layoutMask.requestFocus();
            }
        });
    }

    private void updateStatus(final SensorModule sensorModule) {
        Logg.d(TAG, "updateStatus : batteryVoltage = %f", sensorModule.latestData.batteryVoltage);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.battery_value);

                float batteryVoltage = sensorModule.latestData.batteryVoltage;
                if (Float.compare(batteryVoltage, 0) > 0) {
                    tv.setText(String.format("%.2f", batteryVoltage));

                    if (batteryVoltage >= CtrlCmdRequestStatus.DOUBLE_BATTERY_VOLTAGE_LEVEL_5) {
                        Logg.d(TAG, "level = 5");
                        iconBattery.setImageResource(R.drawable.battery_5);
                    } else if (batteryVoltage >= CtrlCmdRequestStatus.DOUBLE_BATTERY_VOLTAGE_LEVEL_4) {
                        Logg.d(TAG, "level = 4");
                        iconBattery.setImageResource(R.drawable.battery_4);
                    } else if (batteryVoltage >= CtrlCmdRequestStatus.DOUBLE_BATTERY_VOLTAGE_LEVEL_3) {
                        Logg.d(TAG, "level = 3");
                        iconBattery.setImageResource(R.drawable.battery_3);
                    } else if (batteryVoltage >= CtrlCmdRequestStatus.DOUBLE_BATTERY_VOLTAGE_LEVEL_2) {
                        Logg.d(TAG, "level = 2");
                        iconBattery.setImageResource(R.drawable.battery_2);
                    } else {
                        Logg.d(TAG, "level = 1");
                        iconBattery.setImageResource(R.drawable.battery_1);
                    }
                } else {
                    Logg.d(TAG, "level = unknown");
                    tv.setText(getString(R.string.unknown_battery_value));
                    iconBattery.setImageResource(R.drawable.battery_unknown);
                }
            }
        });
    }

    private void updateSensorData(final SensorModule sensorModule) {
        Logg.d(TAG, "updateSensorData");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (sensorModule == null) {
                    Logg.d(TAG, "[ERROR] sensorModule == null");
                    return;
                }

                textInfoData.setText(sensorModule.latestData.makeTextForGUI(sensorModule.enabledSensors));
            }
        });
    }

    private SensorModule.ISensorModule iSensorModule = new SensorModule.ISensorModule() {
        @Override
        public void onReadyCommunication(final int tag, final boolean ready) {
            SensorModule sensorModule = getCurrentSensorModule();
            if (sensorModule == null) {
                return;
            }

            int currentTag = sensorModule.getTag();
            if (currentTag == tag) {
                updateConnectionStatus(sensorModule);

                if (ready) {
                    // If ready is true,
                    // the properties of sensorModule are the newest values.
                    updateParameters(sensorModule);
                    updateSensorData(sensorModule);
                    updateStatus(sensorModule);
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (sensorModules == null) {
                        return;
                    }

                    try {
                        SensorModule sensorModuleFiredEvent = sensorModules.get(tag);
                        String text = (ready ? "READY : " : "DISCONNECTED : ") + sensorModuleFiredEvent.getName();
                        Toast.makeText(ActivitySensorCommunication.this, text, Toast.LENGTH_SHORT).show();
                    } catch (IndexOutOfBoundsException e) {
                        Logg.d(TAG, "[ERROR] tag = %d, sensorModules.size = %d", tag, sensorModules.size());
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onReceiveNotificationReadingResult(int tag) {
            final SensorModule sensorModule = getCurrentSensorModule();
            int currentTag = sensorModule.getTag();
            if (currentTag == tag) {
                updateParameters(sensorModule);
            }
        }

        @Override
        public void onReceiveNotificationStatus(int tag) {
            final SensorModule sensorModule = getCurrentSensorModule();
            int currentTag = sensorModule.getTag();
            if (currentTag == tag) {
                updateStatus(sensorModule);
            }
        }

        @Override
        public void onReceiveNack(final int tag) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (sensorModules == null) {
                        return;
                    }

                    try {
                        SensorModule sensorModuleFiredEvent = sensorModules.get(tag);
                        String text = sensorModuleFiredEvent.getName() + " get a NACK!\nSome changes were rejected.";
                        Toast.makeText(ActivitySensorCommunication.this, text, Toast.LENGTH_SHORT).show();
                    } catch (IndexOutOfBoundsException e) {
                        Logg.d(TAG, "[ERROR] tag = %d, sensorModules.size = %d", tag, sensorModules.size());
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onReceiveNotificationSensorData(int tag) {
            final SensorModule sensorModule = getCurrentSensorModule();
            int currentTag = sensorModule.getTag();
            if (currentTag == tag) {
                updateSensorData(sensorModule);
            }
        }
    };

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Emitter.Listener onAuthenticated = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "VIoT - onAuthenticated");
            socketManager.getSocket().emit("lb-ping");
            if (schedulePingViot == null) {
                schedulePingViot = newScheduledThreadPool(5);
            }
            schedulePingViot.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try {
                        if (socketManager.getSocket() != null
                                && socketManager.getSocket().connected()) {
                            socketManager.getSocket().emit("lb-ping");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 15, TimeUnit.SECONDS);
        }
    };

    private Emitter.Listener onAndroidPongVIOT = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (socketManager.getSocket() != null) {
                Log.d(TAG, "VIoT - pong received ...");
            }
        }
    };

    private void connectSocketViot() {
        if (connectionIntents > 3) {
            showErrorMessage();
            return;
        }
        connectionIntents++;
        socketManager = new SocketManager(this);
        IO.Options opts = new IO.Options();
        opts.transports = new String[]{WebSocket.NAME};
        //opts.forceNew = true;
        socketManager.createSocket(Constants.VIOT_BASE_URL, opts);


        socketManager.getSocket().on("authenticated", onAuthenticated);
        socketManager.getSocket().on("lb-pong", onAndroidPongVIOT);


        if (socketManager.getSocket().connected()) {
            socketManager.getSocket().disconnect();
        }
        socketManager.getSocket().connect();
    }

    private void showErrorMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ActivitySensorCommunication.this, "Socket disconnected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    JSONObject getCredentials() {
        try {

            String API_KEY = preferences.getString("API_KEY", null);
            String API_SECRET = preferences.getString("API_SECRET", null);

            String path = "/api/connections/generateToken?api_key=%s&api_secret=%s";
            String[] APIs = new String[]{API_KEY, API_SECRET};
            String generateTokenApi = Constants.VIOT_BASE_URL + path;
            URL url = new URL(String.format(generateTokenApi, APIs[0],
                    APIs[1]));
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder json = new StringBuilder(1024);
            String tmp;
            while ((tmp = reader.readLine()) != null)
                json.append(tmp).append("\n");
            reader.close();
            return new JSONObject(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onEventCallbackReceived(String event, String socketIdentifier) {
        switch (event) {
            case ConnectionManager.EVENT_CONNECT: {
                Log.d(TAG, "VIoT - onConnectEvent");
                if (socketManager.getSocket() != null) {
                    JSONObject json = getCredentials();
                    try {
                        if (json != null) {
                            JSONObject requestJSONObject = new JSONObject();
                            requestJSONObject.put("id", json.getString("id"));
                            requestJSONObject.put("connectionId", json.getString("connectionId"));
                            requestJSONObject.put("agent", "hub");
                            requestJSONObject.put("uuid", "AndroidALPSSensorPOC");
                            socketManager.getSocket().emit("webee-auth-strategy", requestJSONObject);
                            Log.i(TAG, "json: " + requestJSONObject);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case ConnectionManager.EVENT_DISCONNECT: {
                Log.d(TAG, "VIoT - onDisconnectEvent");
                connectSocketViot();
                break;
            }
        }
    }


    @Override
    public void onReadDataBLESensor(JsonObject message) {
        Log.v(TAG, "message" + message);
        socketManager.getSocket().emit("webee-hub-logger", message);
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////7
}
