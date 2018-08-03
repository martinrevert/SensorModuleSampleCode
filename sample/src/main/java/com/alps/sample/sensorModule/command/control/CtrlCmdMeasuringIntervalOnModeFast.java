package com.alps.sample.sensorModule.command.control;


/**
 * [JP]Fast計測モードでの動作間隔を読み込み・設定します。
 *
 * @see com.alps.sample.sensorModule.enums.MeasuringMode
 * @see CtrlCmdMeasuringMode
 * @see CtrlCmdMeasuringState
 */
public class CtrlCmdMeasuringIntervalOnModeFast extends CtrlCmd {
	private static final String TAG = "CtrlCmdMeasuringIntervalOnModeFast";
	public static final int LENGTH_COMMAND = 4;
	public static final byte EVENT_CODE_SETTING_VALUE = 0x06;
	public static final int MAX = 999;
	public static final int MIN = 10;
	public static final int DEFAULT = 100;


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
	public CtrlCmdMeasuringIntervalOnModeFast() {
		super(LENGTH_COMMAND);
		commandBytes[INDEX_EVENT_CODE] += GETTING_MARKER;
		validCommand = true;
	}

	/**
	 * Parse commandBytes of notification from a sensor module.
	 */
	public CtrlCmdMeasuringIntervalOnModeFast(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			interval = ((receivedCommand[LENGTH_COMMAND-1]<<8)&0x0FF00) | (receivedCommand[LENGTH_COMMAND-2]&0x0FF);
		}
	}

	/**
	 * Make commandBytes for a setting.
	 */
	public CtrlCmdMeasuringIntervalOnModeFast(int interval) {
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
