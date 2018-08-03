package com.alps.sample.activity.base.usingBluetooth.bleScanner;

import android.bluetooth.BluetoothAdapter;
import com.alps.sample.activity.scan.BLEDevice;


/**
 * [JP]BluetoothLowEnergyのスキャンを行う抽象クラスです。
 */
public abstract class LEScanner {
	private static final String TAG = "LEScanner";

	/**
	 * [JP]スキャン結果を通知します。
	 */
	public interface ILEScanner {
		/**
		 * [JP]スキャン結果を{@link BLEDevice}として通知します。
		 *
		 * @param bleDevice スキャン結果がまとめられたオブジェクトです。
		 */
		void onFoundBLEDevice(BLEDevice bleDevice);
	}
	protected ILEScanner iLEScanner;

	public LEScanner(ILEScanner iLEScanner) {
		this.iLEScanner = iLEScanner;
	}


	/**
	 * [JP]現在のスキャン動作状態を切り替えます。
	 *
	 * @param bluetoothAdapter 切り替え対象の{@code android.bluetooth.BluetoothAdapter}です。
	 * @param enable trueの場合、スキャンが開始されます。
	 *               falseの場合、スキャンが停止されます。
	 */
	abstract public void setScanning(BluetoothAdapter bluetoothAdapter, boolean enable);
}
