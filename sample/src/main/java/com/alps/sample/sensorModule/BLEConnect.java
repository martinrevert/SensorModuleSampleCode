package com.alps.sample.sensorModule;

import android.bluetooth.*;
import android.content.Context;
import android.os.Handler;
import com.alps.sample.log.Logg;
import com.alps.sample.sensorModule.command.control.CtrlCmdWakeUpConfigModeTimerWake;

import java.lang.reflect.Method;
import java.util.UUID;


/**
 * [JP] 対象の{@code android.bluetooth.BluetoothDevice}をセンサモジュールと想定し、BLE接続を行います。
 * BLE接続や通信結果のイベントは、{@link com.alps.sample.sensorModule.BLEConnect.IBLEConnect}を経由して提供します。
 *
 * {@link BLEConnect#connect()}を呼び出すと、
 *
 * 1. Connect
 *
 * 2. Discover the service of sensor module<br>
 *     {@link BLEConnect#SERVICE_UUID}
 *
 * 3. Discover characteristics<br>
 *     {@link BLEConnect#CHAR_UUID_RX_GETTING_RESULTS}
 *     {@link BLEConnect#CHAR_UUID_RX_SENSOR_DATA_AND_STATUS}
 *     {@link BLEConnect#CHAR_UUID_TX}
 *
 * 4. Enable Notification for {@link BLEConnect#CHAR_UUID_RX_GETTING_RESULTS}
 *
 * の順に実行します。
 *
 * センサモジュール主導で大量のパケットが送られ始める可能性があるため、
 * {@link BLEConnect#CHAR_UUID_RX_SENSOR_DATA_AND_STATUS}のNotification許可は自動で行いません。
 * データを受ける準備が整った後に、{@link BLEConnect#enableCharacteristicNotification(NotifierType)}
 * を呼び出してください。
 *
 * 
 * なお、AndroidのBLE APIは信頼性が低いため、様々な例外に対する対処を実装していることに注意してください。
 *
 */
public class BLEConnect {
	private final String TAG = getClass().getSimpleName();

	/**
	 * [JP] 発生したBLEイベントを通知するためのインタフェースです。
	 */
	public interface IBLEConnect {
		/**
		 * [JP] 接続状況が変化したことを通知します。
		 *
		 * @param connectionState 変化後の接続状況です。
		 */
		void onConnectionStateChange(ConnectionState connectionState);

		/**
		 * [JP] {@link BLEConnect#enableCharacteristicNotification(NotifierType)}が成功したことを通知します。
		 * 
		 * @see NotifierType
		 * 
		 * @param type イベントが起きたキャラクタリスティックのタイプです。
		 */
		void onCharacteristicEnabled(NotifierType type);
		
		/**
		 * [JP] Notificationが届いたことを通知します。 
		 * 
		 * @see NotifierType
		 * 
		 * @param type イベントが起きたキャラクタリスティックのタイプです。
		 * @param bytes イベントによって受信したバイト列です。
		 */
		void onReceiveNotification(NotifierType type, byte[] bytes);
	}

	/**
	 * [JP] センサモジュールサービスが提供する2種類の通知用キャラクタリスティックを表現します。
	 */
	public enum NotifierType {
		/**
		 * [JP] センサデータパケットとステータス通知を届けるキャラクタリスティックです。
		 * 
		 * @see com.alps.sample.sensorModule.command.sensorData.SensorDataPacket 
		 * @see com.alps.sample.sensorModule.command.control.CtrlCmdRequestStatus
		 */
		SensorDataAndStatus,

		/**
		 * [JP] 読み出しコマンドに対する応答や一部の通知を届けるキャラクタリスティックです。
		 *
		 * @see com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringState
		 * @see com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringMode
		 * @see com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringIntervalOnModeSlow
		 * @see com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringIntervalOnModeFast
		 * @see com.alps.sample.sensorModule.command.control.CtrlCmdSamplingSensors
		 * @see CtrlCmdWakeUpConfigModeTimerWake
		 * @see com.alps.sample.sensorModule.command.control.CtrlCmdAccelerationSensorRange
		 * @see com.alps.sample.sensorModule.command.notify.NotifyBLEConnectionParametersAdjustment
		 * @see com.alps.sample.sensorModule.command.notify.NotifySequencerError
		 */
		GettingResults,
	}

	public final UUID SERVICE_UUID                  = UUID.fromString("47FE55D8-447F-43ef-9AD9-FE6325E17C47");
	public final UUID CHAR_UUID_RX_SENSOR_DATA_AND_STATUS = UUID.fromString("686A9A3B-4C2C-4231-B871-9CFE92CC6B1E");
	public final UUID CHAR_UUID_RX_GETTING_RESULTS = UUID.fromString("078FF5D6-3C93-47f5-A30C-05563B8D831E");
	public final UUID CHAR_UUID_TX                  = UUID.fromString("B962BDD1-5A77-4797-93A1-EDE8D0FF74BD");
	public final UUID CLIENT_CHARACTERISTIC_CONFIG  = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	public static final int DELAY_MILLIS_TIMEOUT_DISCOVER_SERVICES = 10000;
	public static final int DELAY_MILLIS_TIMEOUT_ENABLE_NOTIFICATION = 10000;
	public static final int DELAY_MILLIS_TIMEOUT_CONNECT = 10000;
	public static final int DELAY_MILLIS_TIMEOUT_COMMAND_IF = 10000;
	public static final int DELAY_MILLIS_TIMEOUT_NOTIFY_DISCONNECT_EVENT = 10000;
	public static final int DELAY_MILLIS_RECONNECT = 1000;

	private BluetoothGatt btGatt;
	private BluetoothGattService targetService;
	private BluetoothGattCharacteristic targetCharacteristicSensorValueAndStatus;
	private BluetoothGattCharacteristic targetCharacteristicGettingResults;
	private BluetoothGattCharacteristic targetCharacteristicControl;

	private BluetoothDevice bluetoothDevice;

	private Handler handlerDelay = new Handler();

	private boolean sticky = true;

	private NotifierType enablingTargetType;
	private Context context;

	private IBLEConnect ibleConnect = null;

	/**
	 * [JP] 接続状況を表現します。
	 */
	public enum ConnectionState {
		Disconnected,
		Connected,

		/**
		 * [JP] ユーザ操作によって停止された状態です。
		 */
		Stopped,

		/**
		 * [JP] 再接続できない状態です。
		 */
		FatalError,
	};
	private void changeState(ConnectionState newState) {
		changeState(newState, true);
	}
	private void changeState(ConnectionState newState, boolean shouldNotify) {
		switch (connectionState) {
			case FatalError:
			case Stopped:
				Logg.d(TAG, "[ERROR] changeState is ignored(current=%s, newState=%s)", connectionState, newState);
				break;
			case Disconnected:
			case Connected:
				Logg.d(TAG, "changeState : [%s] ---> [%s]", connectionState, newState);
				connectionState = newState;

				if ((shouldNotify) && (ibleConnect != null)) {
					ibleConnect.onConnectionStateChange(connectionState);
				}
				break;
		}
	}
	private ConnectionState connectionState = ConnectionState.Disconnected;

	private boolean callbackIsAlive() {
		boolean isAlive = true;
		if (ibleConnect == null) {
			isAlive = false;
			Logg.d(TAG, "[ERROR] ibleConnect is null!");
			changeState(ConnectionState.FatalError);
			disconnect();
		}
		return isAlive;
	}

	public BLEConnect(Context context, BluetoothDevice bluetoothDevice, IBLEConnect ibleConnect) {
		this.context = context;
		this.ibleConnect = ibleConnect;
		this.bluetoothDevice = bluetoothDevice;
	}


	private enum TimerTag {
		Connect,
		DiscoverService,
		EnableNotification,
		CommandInterface,
		ExpectDisconnectEvent,
	}
	private TimerTag timerTag;
	private Runnable onTimeout = new Runnable() {
		@Override
		public void run() {
			Logg.d(TAG, "[ERROR] timeout!! : %s (%s)", timerTag, bluetoothDevice);
			switch (timerTag) {
				case Connect:
					if (sticky) {
						if (btGatt != null) {
							btGatt.close();
						}
						Logg.d(TAG, "connect triggered by normal retry");
						connect();
					}
					break;
				case ExpectDisconnectEvent: {
					Logg.d(TAG, "Shut forcibly!! %s", btGatt);
					if (btGatt != null) {
						btGatt.close();
					}
					changeState(ConnectionState.Disconnected);
					if (sticky) {
						handlerDelay.removeCallbacksAndMessages(null);
						handlerDelay.postDelayed(new Runnable() {
							@Override
							public void run() {
								Logg.d(TAG, "connect triggered by ExpectDisconnectEvent");
								connect();
							}
						}, DELAY_MILLIS_RECONNECT);
					}
					break;
				}
				default: {
					switch (connectionState) {
						case FatalError:
							break;
						case Disconnected:
							if (sticky) {
								final TimerTag tag = timerTag;
								handlerDelay.removeCallbacksAndMessages(null);
								handlerDelay.postDelayed(new Runnable() {
									@Override
									public void run() {
										Logg.d(TAG, "connect triggered by %s on state Disconnected", tag);
										connect();
									}
								}, DELAY_MILLIS_RECONNECT);
							}
							break;
						case Connected:
							if (sticky) {
								disconnect();
							}
							break;
					}
					break;
				}
			}
		}
	};
	private Handler handlerTimeout = new Handler();

	private void startTimer(int ms, TimerTag tag) {
		cancelTimer();
		timerTag = tag;
		Logg.d(TAG, "startTimer : %s (%s)", timerTag, bluetoothDevice);
		handlerTimeout.postDelayed(onTimeout, ms);
	}
	
	public void cancelTimer() {
		Logg.d(TAG, "cancelTimer : %s (%s)", timerTag, bluetoothDevice);
		handlerTimeout.removeCallbacksAndMessages(null);
	}

	private boolean isConnecting = false;

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (btGatt != gatt) {
				Logg.d(TAG, "onConnectionStateChange: reject other gatt:%s", gatt);
				gatt.close();
				return;
			}

			Logg.d(TAG, "onConnectionStateChange(gatt=%s, status=%d, newState=%d)", gatt, status, newState);

			if (connectionState == ConnectionState.Stopped) {
				close(false);
				return;
			}

			switch (newState) {
				case BluetoothProfile.STATE_CONNECTED: {
					if (isConnecting) {
						btGatt = gatt;
						isConnecting = false;

						cancelTimer();
						handlerDelay.removeCallbacksAndMessages(null);
						Logg.i(TAG, "[%s] Connected to GATT server(%s)", gatt, bluetoothDevice);
						changeState(ConnectionState.Connected);

						if (!sticky) {
							disconnect();
						}
						else if (connectionState != ConnectionState.FatalError) {
							startTimer(DELAY_MILLIS_TIMEOUT_DISCOVER_SERVICES, TimerTag.DiscoverService);
							btGatt.discoverServices();
						}
					}
					break;
				}
				case BluetoothProfile.STATE_CONNECTING: {
					Logg.i(TAG, "[%s] Connecting...", gatt);
					break;
				}
				case BluetoothProfile.STATE_DISCONNECTED: {
					Logg.i(TAG, "[%s] Disconnected from GATT server.", gatt);

					if (sticky) {
						if (connectionState == ConnectionState.Disconnected) {
							// This is a Android-BLE's bug.
							// Treat as a connection error.
							clearBluetoothDeviceCache(gatt);

							changeState(ConnectionState.Disconnected, false);

							cancelTimer();
							handlerDelay.removeCallbacksAndMessages(null);
							handlerDelay.postDelayed(new Runnable() {
								@Override
								public void run() {
									Logg.d(TAG, "connect triggered by connection error");
									connect();
								}
							}, DELAY_MILLIS_RECONNECT);
						}
						else {
							changeState(ConnectionState.Disconnected);

							if (connectionState != ConnectionState.FatalError) {
								cancelTimer();
								handlerDelay.removeCallbacksAndMessages(null);
								handlerDelay.postDelayed(new Runnable() {
									@Override
									public void run() {
										Logg.d(TAG, "connect triggered by normal disconnection");
										connect();
									}
								}, DELAY_MILLIS_RECONNECT);
							}
						}
					}
					break;
				}
				case BluetoothProfile.STATE_DISCONNECTING: {
					Logg.i(TAG, "[%s] Disconnecting...", gatt);
					break;
				}
				default: {
					Logg.w(TAG, "[%s] [ERROR] Unknown status = %d", gatt, newState);
					disconnect();
					break;
				}
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (btGatt != gatt) {
				Logg.d(TAG, "onServicesDiscovered: reject other gatt:%s", gatt);
				gatt.close();
				return;
			}

			cancelTimer();

			if (connectionState == ConnectionState.Stopped) {
				close(false);
				return;
			}

			Logg.d(TAG, "onServicesDiscovered received: 0x%02X", status);

			if (status == BluetoothGatt.GATT_SUCCESS) {
				targetService = btGatt.getService(SERVICE_UUID);
				if (targetService == null) {
					Logg.d(TAG, "[ERROR] targetService is null!");
					changeState(ConnectionState.FatalError);
					disconnect();
				}
				else {
					targetCharacteristicSensorValueAndStatus = targetService.getCharacteristic(CHAR_UUID_RX_SENSOR_DATA_AND_STATUS);
					targetCharacteristicGettingResults = targetService.getCharacteristic(CHAR_UUID_RX_GETTING_RESULTS);
					targetCharacteristicControl     = targetService.getCharacteristic(CHAR_UUID_TX);

					if ( (targetCharacteristicSensorValueAndStatus == null) || (targetCharacteristicGettingResults == null) || (targetCharacteristicControl == null) ) {
						Logg.d(TAG, "[ERROR] targetCharacteristic is null!");
						changeState(ConnectionState.FatalError);
						disconnect();
					}
					else {
						enableCharacteristicNotification(NotifierType.GettingResults);
					}
				}
			}
			else {
				Logg.d(TAG, "[ERROR] invalid status");
				disconnect();
			}
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,  int status) {
			if (btGatt != gatt) {
				Logg.d(TAG, "onDescriptorRead: reject other gatt:%s", gatt);
				gatt.close();
				return;
			}

			Logg.d(TAG, "onDescriptorRead received: 0x%02X", status);

			if (status != BluetoothGatt.GATT_SUCCESS) {
				Logg.d(TAG, "[ERROR] invalid status");
				disconnect();
			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			if (btGatt != gatt) {
				Logg.d(TAG, "onDescriptorWrite: reject other gatt:%s", gatt);
				gatt.close();
				return;
			}

			cancelTimer();

			if (connectionState == ConnectionState.Stopped) {
				close(false);
				return;
			}

			Logg.d(TAG, "onDescriptorWrite received: 0x%02X", status);

			if (!callbackIsAlive()) {
				return;
			}

			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (sticky) {
					ibleConnect.onCharacteristicEnabled(enablingTargetType);
				}
				else {
					disconnect();
				}
			}
			else {
				Logg.d(TAG, "[ERROR] invalid status");
				disconnect();
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (btGatt != gatt) {
				Logg.d(TAG, "onCharacteristicRead: reject other gatt:%s", gatt);
				gatt.close();
				return;
			}

			Logg.d(TAG, "onCharacteristicRead received: 0x%02X", status);

			if (status != BluetoothGatt.GATT_SUCCESS) {
				Logg.d(TAG, "[ERROR] invalid status");
				disconnect();
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (btGatt != gatt) {
				Logg.d(TAG, "onCharacteristicWrite: reject other gatt:%s", gatt);
				gatt.close();
				return;
			}

			Logg.d(TAG, "onCharacteristicWrite received: 0x%02X", status);

			if (status == BluetoothGatt.GATT_SUCCESS) {
				Logg.d(TAG, "[SENT] %s", Logg.hexString(characteristic.getValue()));
			}
			else {
				Logg.d(TAG, "[ERROR] invalid status");
				disconnect();
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			if (btGatt != gatt) {
				Logg.d(TAG, "onCharacteristicChanged: reject other gatt:%s", gatt);
				gatt.close();
				return;
			}

			if (connectionState == ConnectionState.Stopped) {
				close(false);
				return;
			}

			if (!callbackIsAlive()) {
				return;
			}

			byte [] bytes = characteristic.getValue();

			if (characteristic.getUuid().equals(CHAR_UUID_RX_GETTING_RESULTS)) {
				Logg.d(TAG, "onCharacteristicChanged:");

				cancelTimer();

				Logg.d(TAG, "[RECV] gettingResults : %s", Logg.hexString(bytes));
				ibleConnect.onReceiveNotification(NotifierType.GettingResults, bytes);
			}
			else if (characteristic.getUuid().equals(CHAR_UUID_RX_SENSOR_DATA_AND_STATUS)) {
				Logg.d(TAG, "[RECV] sensorDataAndStatus : %s", Logg.hexString(bytes));
				ibleConnect.onReceiveNotification(NotifierType.SensorDataAndStatus, bytes);
			}
			else {
				cancelTimer();

				Logg.d(TAG, "[ERROR] unknown characteristic was sent a notification!");
				Logg.d(TAG, "[ERROR] %s", Logg.hexString(bytes));
			}
		}
	};

	public void connect() {
		if (bluetoothDevice == null) {
			Logg.w(TAG, "Device not found.  Unable to connect.");
			changeState(ConnectionState.FatalError);
		}

		switch (connectionState) {
			case FatalError:
			case Stopped:
			case Connected:
				Logg.d(TAG, "[ERROR] connectReq is ignored");
				break;
			case Disconnected:
				if (sticky) {
					isConnecting = true;

					startTimer(DELAY_MILLIS_TIMEOUT_CONNECT, TimerTag.Connect);
					btGatt = bluetoothDevice.connectGatt(context, false, mGattCallback);

					Logg.d(TAG, "[%s] Trying to create a new connection... (%s)", btGatt, bluetoothDevice);
				}
				break;
		}
	}

	public void disconnect() {
		if (btGatt == null) {
			Logg.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		clearBluetoothDeviceCache(btGatt);

		if (btGatt != null) {
			btGatt.disconnect();

			// BluetoothGatt#disconnect() is lazy occasionally.
			// So I start timeout-timer...
			if (sticky) {
				startTimer(DELAY_MILLIS_TIMEOUT_NOTIFY_DISCONNECT_EVENT, TimerTag.ExpectDisconnectEvent);
			}
		}
	}

	private void clearBluetoothDeviceCache(BluetoothGatt gatt) {
		// Try to refresh BluetoothGatt caches.

		// This is a countermeasure against the BLE bugs of Android.
		// When don't perform it, there is a possibility to continue failing BLE services discovery.

		// But somehow, BluetoothGatt#refresh() is being hidden.
		// So I call forcibly it using Java-Reflection.
		try {
			Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
			if (localMethod != null) {
				boolean bool = ((Boolean) localMethod.invoke(gatt, new Object[0])).booleanValue();
				Logg.d(TAG, "refresh ? %b", bool);
			}
		}
		catch (Exception localException) {
			Logg.d(TAG, "An exception occured while refreshing device");
		}
	}

	public void stop() {
		sticky = false;

		cancelTimer();
		changeState(ConnectionState.Stopped);

		if (btGatt != null) {
			btGatt.disconnect();
			close(false);
		}
	}

	public void close(boolean sticky) {
		this.sticky = sticky;

		if (btGatt == null) {
			return;
		}
		btGatt.close();
		btGatt = null;
	}

	public void enableCharacteristicNotification(NotifierType type) {
		enablingTargetType = type;

		switch (type) {
			case SensorDataAndStatus:
				setCharacteristicNotification(targetCharacteristicSensorValueAndStatus, true);
				break;
			case GettingResults:
				setCharacteristicNotification(targetCharacteristicGettingResults, true);
				break;
		}
	}

	/**
	 * [JP] 接続中のモジュールに対してバイト列を送信します。
	 * {@code shouldFireTimer}がtrueの場合は応答タイムアウトを判定します。
	 * タイムアウトが発生した場合は通信途絶と見なし、切断されたのちに再接続が行われます。
	 *
	 * @see BLEConnect#DELAY_MILLIS_TIMEOUT_COMMAND_IF
	 *
	 * @param bytes 送信するバイト列です。
	 * @param shouldFireTimer 応答タイムアウトタイマを起動するかどうかです。
	 */
	public void writeCharacteristic(byte [] bytes, boolean shouldFireTimer) {
		if (btGatt == null) {
			Logg.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		if (targetCharacteristicControl == null) {
			return;
		}

		if (shouldFireTimer) {
			startTimer(DELAY_MILLIS_TIMEOUT_COMMAND_IF, TimerTag.CommandInterface);
		}

		targetCharacteristicControl.setValue(bytes);
		btGatt.writeCharacteristic(targetCharacteristicControl);
	}

	private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (btGatt == null) {
			Logg.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		startTimer(DELAY_MILLIS_TIMEOUT_ENABLE_NOTIFICATION, TimerTag.EnableNotification);

		if (!btGatt.setCharacteristicNotification(characteristic, enabled)) {
			return;
		}
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
		if (enabled) {
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		}
		else {
			descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
		}
		btGatt.writeDescriptor(descriptor);
	}
}
