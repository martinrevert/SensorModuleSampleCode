package com.alps.sample.sensorModule.enums;


/**
 * [JP]計測動作状態を表現します。
 *
 * @see com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringState
 */
public enum MeasuringState {
	/**
	 * [JP]計測が停止されています。
	 */
	Stopped,

	/**
	 * [JP]計測が開始されています。
	 * この状態の場合、設定されている計測モードでの計測結果が通知されます。
	 * ただし、{@link com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringState}以外の設定コマンドは無視されるようになることに注意してください。
	 *
	 * @see com.alps.sample.sensorModule.command.sensorData.SensorDataPacket
	 * @see MeasuringMode
	 */
	Started;

	public static final int BIT_PATTERN_STOPPED = 0x00;
	public static final int BIT_PATTERN_STARTED = 0x01;

	public byte toPattern() {
		byte temp = BIT_PATTERN_STOPPED;
		switch (this) {
			case Stopped:
				temp = BIT_PATTERN_STOPPED;
				break;
			case Started:
				temp = BIT_PATTERN_STARTED;
				break;
		}
		return temp;
	}

	public static MeasuringState toEnum(byte pattern) {
		MeasuringState temp = Stopped;
		switch (pattern) {
			case BIT_PATTERN_STOPPED:
				temp = Stopped;
				break;
			case BIT_PATTERN_STARTED:
				temp = Started;
				break;
		}
		return temp;
	}
}
