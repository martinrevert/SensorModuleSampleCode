package com.alps.sample.activity.base.usingBluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.alps.sample.R;
import com.alps.sample.activity.base.usingBluetooth.bleScanner.LEScanner;
import com.alps.sample.activity.base.usingBluetooth.bleScanner.LEScannerForAndroid4_3;
import com.alps.sample.activity.base.usingBluetooth.bleScanner.LEScannerForAndroid5;
import com.alps.sample.activity.scan.BLEDevice;
import com.alps.sample.log.Logg;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


/**
 * [JP]{@link LEScanner}を通じて、BluetoothLowEnergyのスキャン機能を提供します。
 *
 * {@link ActivityUsingBluetooth#onCreate(Bundle)}時に{@code Build.VERSION.SDK_INT}の値を確認し、
 * APIレベルに応じて利用するAPIを変更します。
 */
public abstract class ActivityUsingBluetooth extends AppCompatActivity {
	private static final String TAG = "ActivityUsingBluetooth";


	public ActivityUsingBluetooth() {
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				int sdkLevel = Build.VERSION.SDK_INT;
				if (sdkLevel >= Build.VERSION_CODES.LOLLIPOP) {
					leScanner = new LEScannerForAndroid5(iLEScanner);
				}
				else if (sdkLevel >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
					leScanner = new LEScannerForAndroid4_3(iLEScanner);
				}
	}

	@Override
	protected void onStart() {
		super.onStart();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(bluetoothStateChangedReceiver, intentFilter);
	}

	@Override
	protected void onResume() {
		super.onResume();

		initializeBluetoothAdapter();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (isScanning) {
			enableScanning(false);
		}

		if (bluetoothAdapter != null) {
			bluetoothAdapter = null;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (bluetoothStateChangedReceiver != null) {
			unregisterReceiver(bluetoothStateChangedReceiver);
			bluetoothStateChangedReceiver = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}



	protected BluetoothAdapter bluetoothAdapter;

	private static final int REQUEST_ENABLE_BLUETOOTH = 0;

	/**
	 * [JP]メンバ変数のBluetoothAdapterを初期化します。
	 */
	protected void initializeBluetoothAdapter() {
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();
		if (!bluetoothAdapter.isEnabled()) {
			requestEnablingBluetooth();
		}
	}

	/**
	 * [JP]システムに対してBluetoothパワーオン要求を行います。
	 * このIntentが実行された場合、システムダイアログが表示されます。
	 */
	protected void requestEnablingBluetooth() {
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
	}

	private BroadcastReceiver bluetoothStateChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getExtras().getInt(BluetoothAdapter.EXTRA_STATE);

			switch(state) {
				case BluetoothAdapter.STATE_OFF:
					requestEnablingBluetooth();
					onChangeBluetoothState(false);
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
					break;
				case BluetoothAdapter.STATE_ON:
					onChangeBluetoothState(true);
					break;
				case BluetoothAdapter.STATE_TURNING_ON:
					break;
				default:
					break;
			}
		}
	};

	/**
	 * [JP]端末のBluetooth状態を通知します。
	 *
	 * @param on falseの場合、端末のBluetoothパワーがオフにされたことを示します。
	 */
	abstract protected void onChangeBluetoothState(boolean on);



	private LEScanner leScanner;
	protected boolean isScanning = false;

	/**
	 * [JP]スキャン動作状態を変更します。
	 *
	 * @param enable trueの場合、スキャンを実行します。
	 *               falseの場合、スキャンを停止します。
	 */
	protected void enableScanning(boolean enable) {
		isScanning = enable;

		if (bluetoothAdapter == null) {
			Logg.d(TAG, "[ERROR] bluetoothAdapter == null");
			return;
		}

		if (!bluetoothAdapter.isEnabled()) {
			Logg.d(TAG, "[ERROR] Can't control bluetooth api. (state:OFF)");
			return;
		}

		if (leScanner == null) {
			Logg.d(TAG, "[ERROR] LEScanner == null");
			return;
		}

		leScanner.setScanning(bluetoothAdapter, enable);
	}

	private LEScanner.ILEScanner iLEScanner = new LEScanner.ILEScanner() {
		@Override
		public void onFoundBLEDevice(BLEDevice bleDevice) {
			if (!isScanning) {
				return;
			}

			ActivityUsingBluetooth.this.onFoundBLEDevice(bleDevice);
		}
	};

	protected void onFoundBLEDevice(BLEDevice bleDevice) {
		// NOP : A subclass which uses scanning function should override this callback.
	}
}
