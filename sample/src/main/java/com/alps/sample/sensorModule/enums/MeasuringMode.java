package com.alps.sample.sensorModule.enums;


/**
 * [JP]4種類の計測モードを表現します。
 *
 * @see com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringMode
 */
public enum MeasuringMode {
	/**
	 * [JP]Slowモードで計測します。
	 *
	 * @see com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringIntervalOnModeSlow
	 */
	Slow,

	/**
	 * [JP]Fastモードで計測します。
	 *
	 * @see com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringIntervalOnModeFast
	 */
	Fast,

	/**
	 * [JP]Hybridモードで計測します。
	 *
	 * このモードでは、{@link com.alps.sample.sensorModule.command.sensorData.SensorDataPacketMagAcc}で通知されるセンサはFastモードとして、
	 * {@link com.alps.sample.sensorModule.command.sensorData.SensorDataPacketPreHumTemUVAmLight}で通知されるセンサはSlowモードとして振る舞われます。
	 *
	 * @see com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringIntervalOnModeSlow
	 * @see com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringIntervalOnModeFast
	 */
	Hybrid,

	/**
	 * [JP]Forceモードで計測します。
	 *
	 * このモードでは、{@link com.alps.sample.sensorModule.command.control.CtrlCmdMeasuringState}で{@link MeasuringState#Started}を発行するたびに
	 * 一度だけ計測が行われます。
	 */
	Force;

	public static final int BIT_PATTERN_SLOW = 0x00;
	public static final int BIT_PATTERN_FAST = 0x01;
	public static final int BIT_PATTERN_FORCE = 0x03;
	public static final int BIT_PATTERN_HYBRID = 0x04;
	public static final int BIT_PATTERN_UNKNOWN = 0x00;

	public byte toPattern() {
		byte temp;
		switch (this) {
			case Slow:
				temp = BIT_PATTERN_SLOW;
				break;
			case Fast:
				temp = BIT_PATTERN_FAST;
				break;
			case Force:
				temp = BIT_PATTERN_FORCE;
				break;
			case Hybrid:
				temp = BIT_PATTERN_HYBRID;
				break;
			default:
				temp = BIT_PATTERN_UNKNOWN;
				break;
		}
		return temp;
	}

	public static MeasuringMode toEnum(byte pattern) {
		MeasuringMode temp;
		switch (pattern) {
			case BIT_PATTERN_SLOW:
				temp = Slow;
				break;
			case BIT_PATTERN_FAST:
				temp = Fast;
				break;
			case BIT_PATTERN_FORCE:
				temp = Force;
				break;
			case BIT_PATTERN_HYBRID:
				temp = Hybrid;
				break;
			default:
				temp = Slow;
				break;
		}
		return temp;
	}
}
