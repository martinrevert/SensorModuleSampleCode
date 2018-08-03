package com.alps.sample.sensorModule.command.control;

import com.alps.sample.sensorModule.command.Commander;

import java.util.ArrayList;


/**
 * [JP]センサモジュールの状態通知を要求します。
 * {@link CtrlCmdRequestStatus#rssi}や{@link CtrlCmdRequestStatus#batteryVoltage}を獲得することができます。
 *
 * また、センサモジュールに対する設定系コマンドへの応答は、すべてこのコマンドのフォーマットで表現されます。
 * この場合、{@link CtrlCmdRequestStatus#ack}によって設定の成功・失敗が通知されます。
 *
 * @see com.alps.sample.sensorModule.SensorModule#writeSettings(ArrayList, boolean, Commander.ICommander)
 */
public class CtrlCmdRequestStatus extends CtrlCmd {
	private static final String TAG = "CtrlCmdRequestStatus";
	public static final int LENGTH_COMMAND = 3;

	public static final byte EVENT_CODE_RESULT = (byte) 0xE0;
	public static final byte EVENT_CODE_READING = 0x2E;
	public static final byte BIT_PATTERN_READ_STATUS = 0x01;

	public static final byte MAGIC_NUMBER_AUTO_NOTIFY = 0x00;
	public static final byte MAGIC_NUMBER_ACK = 0x01;

	public byte rssi = 0;
	public float batteryVoltage = 0;

	public boolean autoNotify = false;
	public boolean ack = false;


	@Override
	public byte eventCode() {
		return EVENT_CODE_RESULT;
	}


	@Override
	public String toString() {
		return String.format("<%s : RSSI=%d, battery=%.2f, ACK=%b>", getClass().getSimpleName(), rssi, batteryVoltage, ack);
	}

	/**
	 * Make commandBytes for a getting.
	 */
	public CtrlCmdRequestStatus() {
		super(LENGTH_COMMAND);
		commandBytes[INDEX_EVENT_CODE] = EVENT_CODE_READING;
		commandBytes[LENGTH_COMMAND-1] = BIT_PATTERN_READ_STATUS;
		validCommand = true;
	}

	/**
	 * Parse commandBytes of notification from a sensor module.
	 */
	public CtrlCmdRequestStatus(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			int offset = 6;
			rssi = (byte) (receivedCommand[offset++] & 0x0FF);

			batteryVoltage = ((float)((receivedCommand[offset] & 0x0FF) | ((receivedCommand[offset+1]<<8) & 0x0FF00)))/1000.0f; offset+=3;

			byte ackNackAuto = (byte) (receivedCommand[offset] & 0x0FF);
			if (ackNackAuto == MAGIC_NUMBER_AUTO_NOTIFY) {
				autoNotify = true;
			}
			else {
				autoNotify = false;
				ack = (ackNackAuto == MAGIC_NUMBER_ACK);
			}
		}
	}

	public static final float DOUBLE_BATTERY_VOLTAGE_LEVEL_5 = 2.970f;
	public static final float DOUBLE_BATTERY_VOLTAGE_LEVEL_4 = 2.940f;
	public static final float DOUBLE_BATTERY_VOLTAGE_LEVEL_3 = 2.900f;
	public static final float DOUBLE_BATTERY_VOLTAGE_LEVEL_2 = 2.800f;
	public static final float DOUBLE_BATTERY_VOLTAGE_LEVEL_1 = 2.500f;
}
