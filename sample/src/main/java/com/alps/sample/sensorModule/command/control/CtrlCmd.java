package com.alps.sample.sensorModule.command.control;

import java.nio.ByteBuffer;


/**
 * [JP]センサモジュールとのBLEコマンドインタフェース仕様に基いて、
 * コマンドオブジェクトのバイト列化、受信バイト列のオブジェクト化を行います。
 *
 * # byte format
 * -----------------------------------------------------
 *  ByteIndex ||  0           |   1       |   2
 * -----------------------------------------------------
 *  FieldName ||  EVENT_CODE  |   LENGTH  |   VALUES...
 * -----------------------------------------------------
 *
 * {@link CtrlCmd#CtrlCmd(int)}で生成する場合、
 * そのコマンドオブジェクトはAndroid端末からセンサモジュールに送られるコマンドとして生成されます。
 * 設定コマンドとして送出する場合、センサモジュールから設定結果が{@link CtrlCmdRequestStatus}のフォーマットで返されます。
 *
 * EVENT_CODEに{@link CtrlCmd#GETTING_MARKER}を加算すると、読み込みコマンドとして解釈されます。
 * この場合、読み込み結果がこちらからの設定コマンドと同一のフォーマットで返されるので、
 * {@link CtrlCmd#CtrlCmd(byte[])}でオブジェクト化を行います。
 *
 */
public abstract class CtrlCmd {
	private static final String TAG = "CtrlCmd";
	public static final int HEADER_LENGTH = 2;
	public static final int CONTROL_COMMAND_LENGTH_MIN = 3;
	public static final int DUMMY_DATA = 0x00;

	public static final int INDEX_EVENT_CODE = 0;
	public static final int INDEX_LENGTH = 1;
	public static final int INDEX_VALUE_ROOT = 2;

	public static final int GETTING_MARKER = 0x80;


	protected boolean shouldFireTimeoutTimer;


	/**
	 * [JP] コマンドを送った後にタイムアウトを検出するかどうかの真偽値です。
	 *
	 * @return 真偽値
	 */
	public boolean getShouldFireTimeoutTimer() {
		return shouldFireTimeoutTimer;
	}

	protected byte[] commandBytes;

	/**
	 * [JP] コマンドのバイト列を獲得します。
	 * バイト列はコンストラクタメソッドによって、適切に生成されていることが保証されています。
	 *
	 * @return コマンドバイト列
	 */
	public byte[] getBytes() {
		return commandBytes;
	}



	protected boolean validCommand = false;
	public boolean isValid() {
		return validCommand;
	}


	public CtrlCmd(byte[] receivedBytes) {
		if (receivedBytes.length < CONTROL_COMMAND_LENGTH_MIN) {
			return;
		}

		if (receivedBytes[INDEX_EVENT_CODE] != eventCode()) {
			return;
		}

		validCommand = true;
		shouldFireTimeoutTimer = false;
	}

	public CtrlCmd(int length) {
		if (length < CONTROL_COMMAND_LENGTH_MIN) {
			length = CONTROL_COMMAND_LENGTH_MIN;
		}

		ByteBuffer buffer = ByteBuffer.allocate(length);
		buffer.put(eventCode());
		buffer.put((byte) length);
		for (int i=0; i<length-HEADER_LENGTH; i++) {
			buffer.put((byte) DUMMY_DATA);
		}
		commandBytes = buffer.array();
		shouldFireTimeoutTimer = true;
	}

	abstract public byte eventCode();
}
