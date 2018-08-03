package com.alps.sample.sensorModule.command.notify;


import com.alps.sample.sensorModule.enums.SequencerError;
import com.alps.sample.sensorModule.command.control.CtrlCmd;


/**
 * [JP]センサモジュールに対する設定系コマンドについて、実行不可能な指定がされた場合に通知されます。
 *
 * @see SequencerError
 */
public class NotifySequencerError extends CtrlCmd {
	private static final String TAG = "NotifySequencerError";
	public static final int LENGTH_COMMAND = 3;
	public static final byte EVENT_CODE_NOTIFY_VALUE = (byte) 0x82;

	@Override
	public byte eventCode() {
		return EVENT_CODE_NOTIFY_VALUE;
	}

	@Override
	public String toString() {
		return String.format("<%s : %s>", getClass().getSimpleName(), sequencerError);
	}

	public SequencerError sequencerError;

	/**
	 * Parse commandBytes of notification from a sensor module.
	 */
	public NotifySequencerError(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			byte pattern = receivedCommand[LENGTH_COMMAND-1];
			sequencerError = SequencerError.toEnum(pattern);
		}
	}
}
