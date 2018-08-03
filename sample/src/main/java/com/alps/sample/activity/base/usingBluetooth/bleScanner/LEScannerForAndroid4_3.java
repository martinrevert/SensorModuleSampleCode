package com.alps.sample.activity.base.usingBluetooth.bleScanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.alps.sample.activity.scan.BLEDevice;


/**
 * [JP]Android4.3(API Level 18)準拠でのBluetoothLowEnergyスキャンを行います。
 */
@SuppressWarnings("deprecation")
public class LEScannerForAndroid4_3 extends LEScanner {
	private static final String TAG = "LEScannerForAndroid4_3";

	public LEScannerForAndroid4_3(ILEScanner iLEScanner) {
		super(iLEScanner);
	}

	@Override
	public void setScanning(BluetoothAdapter bluetoothAdapter, boolean enable) {
		if (enable) {
			bluetoothAdapter.startLeScan(leScanCallback);
		} else {
			bluetoothAdapter.stopLeScan(leScanCallback);
		}
	}

	private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			BLEDevice bleDevice = new BLEDevice(device, String.valueOf(rssi), scanRecord);

			if (iLEScanner != null) {
				iLEScanner.onFoundBLEDevice(bleDevice);
			}
		}
	};
}
