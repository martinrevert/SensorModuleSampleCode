package com.alps.sample.sensorModule.command.control;

import com.alps.sample.sensorModule.enums.AccelerationSensorRange;


/**
 * [JP]加速度センサの重力加速度レンジを読み込み・設定します。
 *
 * @see AccelerationSensorRange
 */
public class CtrlCmdAccelerationSensorRange extends CtrlCmd {
	private static final String TAG = "CtrlCmdAccelerationSensorRange";
	public static final int LENGTH_COMMAND = 3;
	public static final byte EVENT_CODE_SETTING_VALUE = 0x02;


	@Override
	public byte eventCode() {
		return EVENT_CODE_SETTING_VALUE;
	}


	@Override
	public String toString() {
		return String.format("<%s : %s>", getClass().getSimpleName(), range);
	}

	public AccelerationSensorRange range;

	/**
	 * Make commandBytes for a getting.
	 */
	public CtrlCmdAccelerationSensorRange() {
		super(LENGTH_COMMAND);
		commandBytes[INDEX_EVENT_CODE] += GETTING_MARKER;
		validCommand = true;
	}

	/**
	 * Parse commandBytes of notification from a sensor module.
	 */
	public CtrlCmdAccelerationSensorRange(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			byte bitPattern = receivedCommand[LENGTH_COMMAND-1];
			range = AccelerationSensorRange.toEnum(bitPattern);
		}
	}
}
