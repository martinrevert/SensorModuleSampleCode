package com.alps.sample.log;

import android.util.Log;


/**
 * [JP] {@code android.util.Log}の便利クラスです。
 */
public class Logg {
	private static final String TAG = "Logg";
	private static final String EMPTY = "";

	private static boolean LOG_ENABLE = true;

	public static int d(String tag, String text) {
		if (LOG_ENABLE) {
			return Log.d(tag, text);
		}
		else {
			return 0;
		}
	}

	public static int d(String tag, String format, Object... args) {
		if (LOG_ENABLE) {
			return Log.d(tag, format(format, args));
		}
		else {
			return 0;
		}
	}
	public static int w(String tag, String format, Object... args) {
		if (LOG_ENABLE) {
			return Log.w(tag, format(format, args));
		}
		else {
			return 0;
		}
	}
	public static int i(String tag, String format, Object... args) {
		if (LOG_ENABLE) {
			return Log.i(tag, format(format, args));
		}
		else {
			return 0;
		}
	}
	public static int e(String tag, String format, Object... args) {
		if (LOG_ENABLE) {
			return Log.e(tag, format(format, args));
		}
		else {
			return 0;
		}
	}

	private static String format(String format, Object... args) {
		try {
			return String.format(format == null ? EMPTY : format, args);
		} catch (RuntimeException e) {
			Logg.w(TAG, "format error. reason=%s, format=%s", e.getMessage(), format);
			return String.format(EMPTY, format);
		}

	}

	public static StringBuffer buffer = new StringBuffer();
	public static String hexString(byte[] bytes) {
		if (bytes == null) {
			return "(null)";
		}

		buffer.delete(0, buffer.length());
		for (byte b : bytes) {
			buffer.append(String.format("%02X", b));
		}
		return buffer.toString();
	}
}
