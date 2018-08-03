package com.alps.sample.sensorModule.enums;


/**
 * [JP]実行不可能な指定がされた場合のエラー情報を表現します。
 *
 * @see com.alps.sample.sensorModule.command.notify.NotifySequencerError
 */
public enum SequencerError {
	Unknown,
	Released,
	Happened;

	public static final int BIT_PATTERN_RELEASED = 0x00;
	public static final int BIT_PATTERN_HAPPENED = 0x01;

	public static SequencerError toEnum(byte pattern) {
		SequencerError temp;
		switch (pattern) {
			case BIT_PATTERN_RELEASED:
				temp = Released;
				break;
			case BIT_PATTERN_HAPPENED:
				temp = Happened;
				break;
			default:
				temp = Unknown;
				break;
		}
		return temp;
	}
}
