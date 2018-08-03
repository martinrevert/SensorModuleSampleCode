package com.alps.sample.sensorModule.command.control;

import com.alps.sample.sensorModule.enums.AwakeMode;


/**
 * [JP]センサモジュールをスリープ状態に設定します。
 *
 * このコマンドを発行すると、モジュールは直ちにスリープ状態へ移行し、BLE通信は切断されます。
 * その後、指定した復帰条件を満たしたときにBLEアドバタイズが再開されます。
 *
 * @see AwakeMode
 */
public class CtrlCmdSleep extends CtrlCmd {
	private static final String TAG = "CtrlCmdSleep";
	public static final int LENGTH_COMMAND = 3;
	public static final byte EVENT_CODE_SETTING_VALUE = 0x22;

	private AwakeMode awakeMode = AwakeMode.Timer;

	@Override
	public byte eventCode() {
		return EVENT_CODE_SETTING_VALUE;
	}


	@Override
	public String toString() {
		return String.format("<%s : mode=%s>", getClass().getSimpleName(), awakeMode);
	}

	/**
	 * Make commandBytes for a setting.
	 */
	public CtrlCmdSleep(AwakeMode awakeMode) {
		super(LENGTH_COMMAND);
		this.awakeMode = awakeMode;
		commandBytes[LENGTH_COMMAND-1] = awakeMode.toBitPattern();
		validCommand = true;

		// The sensor module don't reply anything to this command.
		// So set false to shouldFireTimeoutTimer.
		shouldFireTimeoutTimer = false;
	}
}
