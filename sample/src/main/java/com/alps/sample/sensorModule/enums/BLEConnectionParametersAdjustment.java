package com.alps.sample.sensorModule.enums;


/**
 * [JP]センサモジュールによってBLE Connection Parametersの調整が行われる際、
 * その開始・成功・失敗を表現します。
 *
 * @see com.alps.sample.sensorModule.command.notify.NotifyBLEConnectionParametersAdjustment
 */
public enum BLEConnectionParametersAdjustment {
	Unknown,
	Adjusted,
	Adjusting,
	Rejected;

	public static final int BIT_PATTERN_ADJUSTED = 0x00;
	public static final int BIT_PATTERN_ADJUSTING = 0x01;
	public static final int BIT_PATTERN_REJECTED = 0x02;

	public static BLEConnectionParametersAdjustment toEnum(byte pattern) {
		BLEConnectionParametersAdjustment temp;
		switch (pattern) {
			case BIT_PATTERN_ADJUSTED:
				temp = Adjusted;
				break;
			case BIT_PATTERN_ADJUSTING:
				temp = Adjusting;
				break;
			case BIT_PATTERN_REJECTED:
				temp = Rejected;
				break;
			default:
				temp = Unknown;
				break;
		}
		return temp;
	}

}
