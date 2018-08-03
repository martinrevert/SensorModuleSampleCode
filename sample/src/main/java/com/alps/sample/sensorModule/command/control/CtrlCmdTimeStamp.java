package com.alps.sample.sensorModule.command.control;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * [JP]センサモジュールが保持するタイムスタンプを読み込み・設定します。
 */
public class CtrlCmdTimeStamp extends CtrlCmd {
	private static final String TAG = "CtrlCmdSetTime";
	public static final int LENGTH_COMMAND = 10;
	public static final byte EVENT_CODE_SETTING_VALUE = 0x30;

	public Date date;

	@Override
	public byte eventCode() {
		return EVENT_CODE_SETTING_VALUE;
	}

	@Override
	public String toString() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		return String.format("<%s : date=%s>", getClass().getSimpleName(), df.format(date));
	}


	/**
	 * Make commandBytes for a getting.
	 */
	public CtrlCmdTimeStamp() {
		super(LENGTH_COMMAND);
		commandBytes[INDEX_EVENT_CODE] += GETTING_MARKER;
		validCommand = true;
	}

	/**
	 * Parse commandBytes of notification from a sensor module.
	 */
	public CtrlCmdTimeStamp(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			int offset = INDEX_VALUE_ROOT;
			int ms = ((((int)receivedCommand[offset+1])<<8)& 0x0FF00)|(receivedCommand[offset] & 0x0FF); offset += 2;
			int sec = receivedCommand[offset++];
			int min = receivedCommand[offset++];
			int hour = receivedCommand[offset++];
			int day = receivedCommand[offset++];
			int month = receivedCommand[offset++] - 1;
			int year = receivedCommand[offset++] + 2000;

			Calendar calendar = new GregorianCalendar(year, month, day, hour, min, sec);
			long temp = calendar.getTimeInMillis() + ms;

			calendar = Calendar.getInstance();
			calendar.setTimeInMillis(temp);

			date = calendar.getTime();
		}
	}

	/**
	 * Make commandBytes for a setting.
	 */
	public CtrlCmdTimeStamp(Date date) {
		super(LENGTH_COMMAND);

		this.date = date;

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		int ms = calendar.get(Calendar.MILLISECOND);
		int sec = calendar.get(Calendar.SECOND);
		int min = calendar.get(Calendar.MINUTE);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int month = calendar.get(Calendar.MONTH) + 1;
		int year = calendar.get(Calendar.YEAR) - 2000;

		int offset = INDEX_VALUE_ROOT;
		commandBytes[offset++] = (byte) (ms & 0x0FF);
		commandBytes[offset++] = (byte) ((ms >> 8) & 0x0FF);
		commandBytes[offset++] = (byte) (sec & 0x0FF);
		commandBytes[offset++] = (byte) (min & 0x0FF);
		commandBytes[offset++] = (byte) (hour & 0x0FF);
		commandBytes[offset++] = (byte) (day & 0x0FF);
		commandBytes[offset++] = (byte) (month & 0x0FF);
		commandBytes[offset++] = (byte) (year & 0x0FF);

		validCommand = true;
	}
}
