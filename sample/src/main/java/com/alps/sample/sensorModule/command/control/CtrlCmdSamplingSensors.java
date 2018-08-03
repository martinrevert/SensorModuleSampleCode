package com.alps.sample.sensorModule.command.control;

import com.alps.sample.sensorModule.enums.Sensor;

import java.util.Set;


/**
 * [JP]動作させるセンサの組み合わせを読み込み・設定します。
 *
 * @see com.alps.sample.sensorModule.enums.Sensor
 */
public class CtrlCmdSamplingSensors extends CtrlCmd {
	private static final String TAG = "CtrlCmdSamplingSensors";
	public static final int LENGTH_COMMAND = 3;
	public static final byte EVENT_CODE_SETTING_VALUE = 0x01;


	@Override
	public byte eventCode() {
		return EVENT_CODE_SETTING_VALUE;
	}


	@Override
	public String toString() {
		return String.format("<%s : %s>", getClass().getSimpleName(), enabledSensors);
	}

	public Set<Sensor> enabledSensors;

	/**
	 * Make commandBytes for a getting.
	 */
	public CtrlCmdSamplingSensors() {
		super(LENGTH_COMMAND);
		commandBytes[INDEX_EVENT_CODE] += GETTING_MARKER;
		validCommand = true;
	}

	/**
	 * Parse commandBytes of notification from a sensor module.
	 */
	public CtrlCmdSamplingSensors(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			byte bitMask = receivedCommand[LENGTH_COMMAND-1];
			enabledSensors = Sensor.toEnumSet(bitMask);
		}
	}

	/**
	 * Make commandBytes for a setting.
	 */
	public CtrlCmdSamplingSensors(Set<Sensor> enabledSensors) {
		super(LENGTH_COMMAND);
		byte bitmask = 0x00;
		for (Sensor sensor : enabledSensors) {
			bitmask |= sensor.toBitMask();
		}
		this.enabledSensors = enabledSensors;
		commandBytes[LENGTH_COMMAND-1] = bitmask;
		validCommand = true;
	}
}
