package com.alps.sample.sensorModule.command.control;

import com.alps.sample.sensorModule.enums.MeasuringMode;


/**
 * [JP]4種類の計測モードを読み込み・設定します。
 *
 * @see MeasuringMode
 */
public class CtrlCmdMeasuringMode extends CtrlCmd {
	private static final String TAG = "CtrlCmdMeasuringMode";
	public static final int LENGTH_COMMAND = 3;
	public static final byte EVENT_CODE_SETTING_VALUE = 0x04;


	@Override
	public byte eventCode() {
		return EVENT_CODE_SETTING_VALUE;
	}


	@Override
	public String toString() {
		return String.format("<%s : %s>", getClass().getSimpleName(), measuringMode);
	}

	public MeasuringMode measuringMode;

	/**
	 * Make commandBytes for a getting.
	 */
	public CtrlCmdMeasuringMode() {
		super(LENGTH_COMMAND);
		commandBytes[INDEX_EVENT_CODE] += GETTING_MARKER;
		validCommand = true;
	}

	/**
	 * Parse commandBytes of notification from a sensor module.
	 */
	public CtrlCmdMeasuringMode(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			byte pattern = (byte) (receivedCommand[LENGTH_COMMAND-1] & 0x07);
			measuringMode = MeasuringMode.toEnum(pattern);
		}
	}

	/**
	 * Make commandBytes for a setting.
	 */
	public CtrlCmdMeasuringMode(MeasuringMode measuringMode) {
		super(LENGTH_COMMAND);
		this.measuringMode = measuringMode;
		commandBytes[LENGTH_COMMAND-1] = measuringMode.toPattern();
		validCommand = true;
	}
}
