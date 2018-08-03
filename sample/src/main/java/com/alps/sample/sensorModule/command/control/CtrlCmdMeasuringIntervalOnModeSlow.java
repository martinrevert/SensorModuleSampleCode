package com.alps.sample.sensorModule.command.control;


/**
 * [JP]Slow計測モードでの動作間隔を読み込み・設定します。
 *
 * @see com.alps.sample.sensorModule.enums.MeasuringMode
 * @see CtrlCmdMeasuringMode
 * @see CtrlCmdMeasuringState
 */
public class CtrlCmdMeasuringIntervalOnModeSlow extends CtrlCmd {
	private static final String TAG = "CtrlCmdMeasuringIntervalOnModeSlow";
	public static final int LENGTH_COMMAND = 4;
	public static final byte EVENT_CODE_SETTING_VALUE = 0x05;
	public static final int MAX = 65535;
	public static final int MIN = 1;
	public static final int DEFAULT = 1;


	@Override
	public byte eventCode() {
		return EVENT_CODE_SETTING_VALUE;
	}


	@Override
	public String toString() {
		return String.format("<%s : interval=%d>", getClass().getSimpleName(), interval);
	}

	public int interval = MIN;

	/**
	 * Make commandBytes for a getting.
	 */
	public CtrlCmdMeasuringIntervalOnModeSlow() {
		super(LENGTH_COMMAND);
		commandBytes[INDEX_EVENT_CODE] += GETTING_MARKER;
		validCommand = true;
	}

	/**
	 * Parse commandBytes of notification from a sensor module.
	 */
	public CtrlCmdMeasuringIntervalOnModeSlow(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			interval = ((receivedCommand[LENGTH_COMMAND-1]<<8)&0x0FF00) | (receivedCommand[LENGTH_COMMAND-2]&0x0FF);
		}
	}

	/**
	 * Make commandBytes for a setting.
	 */
	public CtrlCmdMeasuringIntervalOnModeSlow(int interval) {
		super(LENGTH_COMMAND);
		if (interval > MAX) {
			interval = MAX;
		}
		else if (interval < MIN) {
			interval = MIN;
		}
		this.interval = interval;
		commandBytes[LENGTH_COMMAND-1] = (byte) ((interval>>8) & 0x0FF);
		commandBytes[LENGTH_COMMAND-2] = (byte) ((interval>>0) & 0x0FF);
		validCommand = true;
	}
}
