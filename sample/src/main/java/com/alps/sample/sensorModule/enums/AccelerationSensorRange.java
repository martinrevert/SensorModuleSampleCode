package com.alps.sample.sensorModule.enums;


/**
 * [JP]加速度センサの重力加速度レンジを表現します。
 *
 * @see com.alps.sample.sensorModule.command.control.CtrlCmdAccelerationSensorRange
 * @see com.alps.sample.sensorModule.Formula#calcAcc(int, AccelerationSensorRange)
 */
public enum AccelerationSensorRange {
	/**
	 * ± 2G range
	 */
	G2,

	/**
	 * ± 4G range
	 */
	G4,

	/**
	 * ± 8G range
	 */
	G8,

	/**
	 * ± 12G range
	 */
	G12,

	/**
	 * ± 16G range
	 */
	G16;

	public static final int BIT_PATTERN_G2 = 0x0;
	public static final int BIT_PATTERN_G4 = 0x1;
	public static final int BIT_PATTERN_G8 = 0x2;
	public static final int BIT_PATTERN_G12 = 0x3;
	public static final int BIT_PATTERN_G16 = 0x4;

	public static final int MAGNIFICATION_G2 = 2;
	public static final int MAGNIFICATION_G4 = 4;
	public static final int MAGNIFICATION_G8 = 8;
	public static final int MAGNIFICATION_G12 = 12;
	public static final int MAGNIFICATION_G16 = 16;

	public byte toBitPattern() {
		byte b = 0x00;
		switch (this) {
			case G2:
				b |= BIT_PATTERN_G2;
				break;
			case G4:
				b |= BIT_PATTERN_G4;
				break;
			case G8:
				b |= BIT_PATTERN_G8;
				break;
			case G12:
				b |= BIT_PATTERN_G12;
				break;
			case G16:
				b |= BIT_PATTERN_G16;
				break;
		}
		return b;
	}

	public static AccelerationSensorRange toEnum(byte bitPattern) {
		AccelerationSensorRange range;
		switch (bitPattern) {
			case BIT_PATTERN_G2:
				range = G2;
				break;
			case BIT_PATTERN_G4:
				range = G4;
				break;
			case BIT_PATTERN_G8:
				range = G8;
				break;
			case BIT_PATTERN_G12:
				range = G12;
				break;
			case BIT_PATTERN_G16:
				range = G16;
				break;
			default:
				range = G2;
				break;
		}
		return range;
	}

	public int toMagnification() {
		int mag;
		switch (this) {
			case G2:
				mag = MAGNIFICATION_G2;
				break;
			case G4:
				mag = MAGNIFICATION_G4;
				break;
			case G8:
				mag = MAGNIFICATION_G8;
				break;
			case G12:
				mag = MAGNIFICATION_G12;
				break;
			case G16:
				mag = MAGNIFICATION_G16;
				break;
			default:
				mag = MAGNIFICATION_G2;
				break;
		}
		return mag;
	}
}
