package com.alps.sample.sensorModule.command.control;

import com.alps.sample.sensorModule.enums.MeasuringState;


/**
 * [JP]計測動作状態を読み込み・設定します。
 * 設定コマンドとして使用した場合、センサモジュールの計測動作の開始・停止を操作することができます。
 *
 * @see MeasuringState
 */
public class CtrlCmdMeasuringState extends CtrlCmd {
	private static final String TAG = "CtrlCmdMeasuringState";
	public static final int LENGTH_COMMAND = 3;
	public static final byte EVENT_CODE_SETTING_VALUE = 0x20;


	@Override
	public byte eventCode() {
		return EVENT_CODE_SETTING_VALUE;
	}


	@Override
	public String toString() {
		return String.format("<%s : %s>", getClass().getSimpleName(), measuringState);
	}

	public MeasuringState measuringState;

	/**
	 * Make commandBytes for a getting.
	 */
	public CtrlCmdMeasuringState() {
		super(LENGTH_COMMAND);
		commandBytes[INDEX_EVENT_CODE] += GETTING_MARKER;
		validCommand = true;
	}

	/**
	 * Parse commandBytes of notification from a sensor module.
	 */
	public CtrlCmdMeasuringState(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			byte pattern = (byte) (receivedCommand[LENGTH_COMMAND-1] & 0x01);
			measuringState = MeasuringState.toEnum(pattern);
		}
	}

	/**
	 * Make commandBytes for a setting.
	 */
	public CtrlCmdMeasuringState(MeasuringState measuringState) {
		super(LENGTH_COMMAND);
		commandBytes[LENGTH_COMMAND-1] = measuringState.toPattern();
		this.measuringState = measuringState;
		validCommand = true;
	}
}
