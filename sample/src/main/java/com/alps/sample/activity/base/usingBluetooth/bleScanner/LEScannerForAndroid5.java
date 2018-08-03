package com.alps.sample.activity.base.usingBluetooth.bleScanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import com.alps.sample.activity.scan.BLEDevice;


/**
 * [JP]Android5.0(API Level 21)準拠でのBluetoothLowEnergyスキャンを行います。
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LEScannerForAndroid5 extends LEScanner {
	private static final String TAG = "LEScannerForAndroid5";

	public LEScannerForAndroid5(ILEScanner iLEScanner) {
		super(iLEScanner);
	}

	@Override
	public void setScanning(BluetoothAdapter bluetoothAdapter, boolean enable) {
		BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

		if (bluetoothLeScanner == null) {
			return;
		}

		if (enable) {
			ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
			bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
		} else {
			bluetoothLeScanner.stopScan(scanCallback);
		}
	}

	private ScanCallback scanCallback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			super.onScanResult(callbackType, result);

			BluetoothDevice device = result.getDevice();
			int rssi = result.getRssi();

			BLEDevice bleDevice = new BLEDevice(device, String.valueOf(rssi), result.getScanRecord().getBytes());

			if (iLEScanner != null) {
				iLEScanner.onFoundBLEDevice(bleDevice);
			}
		}
	};
}
