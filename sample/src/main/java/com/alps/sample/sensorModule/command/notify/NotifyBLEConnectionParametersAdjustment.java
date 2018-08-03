package com.alps.sample.sensorModule.command.notify;

import com.alps.sample.sensorModule.enums.BLEConnectionParametersAdjustment;
import com.alps.sample.sensorModule.command.control.CtrlCmd;


/**
 * [JP]BLEのConnection Parametersが更新されるときに通知されます。
 *
 * @see BLEConnectionParametersAdjustment
 */
public class NotifyBLEConnectionParametersAdjustment extends CtrlCmd {
	private static final String TAG = "NotifyBLEConnectionParametersAdjustment";
	public static final int LENGTH_COMMAND = 3;
	public static final byte EVENT_CODE_NOTIFY_VALUE = (byte) 0x81;


	@Override
	public byte eventCode() {
		return EVENT_CODE_NOTIFY_VALUE;
	}


	@Override
	public String toString() {
		return String.format("<%s : %s>", getClass().getSimpleName(), connectionParametersAdjustment);
	}

	public BLEConnectionParametersAdjustment connectionParametersAdjustment;

	/**
	 * Parse commandBytes of notification from a sensor module.
	 */
	public NotifyBLEConnectionParametersAdjustment(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			byte pattern = receivedCommand[LENGTH_COMMAND-1];
			connectionParametersAdjustment = BLEConnectionParametersAdjustment.toEnum(pattern);
		}
	}
}
