package com.alps.sample.sensorModule.enums;


import com.alps.sample.sensorModule.command.control.CtrlCmdWakeUpConfigModeTimerWake;

/**
 * [JP]スリープからの復帰方法を表現します。
 *
 * @see com.alps.sample.sensorModule.command.control.CtrlCmdSleep
 */
public enum AwakeMode {
	/**
	 * [JP]タイマ復帰モードです。
	 * 事前に指定した期間だけ動作を停止します。
	 *
	 * @see CtrlCmdWakeUpConfigModeTimerWake
	 */
	Timer,

	/**
	 * [JP]加速度復帰モードです。
	 * 加速度変化が起こるまで動作を停止します。
	 */
	Accel;

	public static final int BIT_PATTERN_ACCEL = 0x2;
	public static final int BIT_PATTERN_TIMER = 0x3;

	public byte toBitPattern() {
		byte b = 0x00;
		switch (this) {
			case Timer:
				b |= BIT_PATTERN_TIMER;
				break;
			case Accel:
				b |= BIT_PATTERN_ACCEL;
				break;
		}
		return b;
	}
}
