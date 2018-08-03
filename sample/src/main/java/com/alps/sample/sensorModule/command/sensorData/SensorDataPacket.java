package com.alps.sample.sensorModule.command.sensorData;


/**
 * [JP]センサモジュールとのBLEコマンドインタフェース仕様に基いて、
 * 受信したセンサデータパケットのオブジェクト化を行います。
 *
 * センサデータパケットは2種類定義されており、それぞれ
 * {@link SensorDataPacketMagAcc}
 * {@link SensorDataPacketPreHumTemUVAmLight}
 * で表現されます。
 *
 * このセンサデータパケットがどのようなタイミングで送られるかは、
 * {@link com.alps.sample.sensorModule.command.control.CtrlCmdSamplingSensors}
 * {@link com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringMode}
 * での指定によって決まります。
 *
 */
public abstract class SensorDataPacket {
	private static final String TAG = "SensorDataPacket";
	public static final int SENSOR_DATA_LENGTH = 20;
	public static final int INDEX_EVENT_CODE = 0;

	protected int index = -1;
	protected boolean validCommand = false;

	public SensorDataPacket(byte[] receivedBytes) {
		if (receivedBytes.length != SENSOR_DATA_LENGTH) {
			return;
		}

		if (receivedBytes[INDEX_EVENT_CODE] != eventCode()) {
			return;
		}

		validCommand = true;
	}

	public int getIndex() {
		return index;
	}

	abstract public byte eventCode();
}
