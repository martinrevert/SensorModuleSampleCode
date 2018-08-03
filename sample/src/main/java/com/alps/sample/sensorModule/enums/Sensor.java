package com.alps.sample.sensorModule.enums;

import java.util.LinkedHashSet;
import java.util.Set;


/**
 * [JP]センサの種類を表現します。
 *
 * @see com.alps.sample.sensorModule.command.control.CtrlCmdSamplingSensors
 */
public enum Sensor {
	Acceleration,
	Magnetic,
	Pressure,
	Temperature,
	Humidity,
	UV,
	AmbientLight;

	public static final int BITMASK_ACCELERATION = 0x01;
	public static final int BITMASK_MAGNETIC = 0x02;
	public static final int BITMASK_PRESSURE = 0x04;
	public static final int BITMASK_HUMIDITY = 0x08;
	public static final int BITMASK_TEMPERATURE = 0x10;
	public static final int BITMASK_UV = 0x20;
	public static final int BITMASK_AMBIENT_LIGHT = 0x40;
	public static final int BITMASK_UNKNOWN = 0x00;

	public static final int BIT_SHIFT_ACCELERATION = 0;
	public static final int BIT_SHIFT_MAGNETIC = 1;
	public static final int BIT_SHIFT_PRESSURE = 2;
	public static final int BIT_SHIFT_HUMIDITY = 3;
	public static final int BIT_SHIFT_TEMPERATURE = 4;
	public static final int BIT_SHIFT_UV = 5;
	public static final int BIT_SHIFT_AMBIENT_LIGHT = 6;

	public byte toBitMask() {
		byte temp = BITMASK_UNKNOWN;
		switch (this) {
			case Acceleration:
				temp = BITMASK_ACCELERATION;
				break;
			case Magnetic:
				temp = BITMASK_MAGNETIC;
				break;
			case Pressure:
				temp = BITMASK_PRESSURE;
				break;
			case Temperature:
				temp = BITMASK_TEMPERATURE;
				break;
			case Humidity:
				temp = BITMASK_HUMIDITY;
				break;
			case UV:
				temp = BITMASK_UV;
				break;
			case AmbientLight:
				temp = BITMASK_AMBIENT_LIGHT;
				break;
		}
		return temp;
	}

	public static Set<Sensor> toEnumSet(byte bitmask) {
		Set<Sensor> set = new LinkedHashSet<Sensor>();
		if ( ((bitmask >> BIT_SHIFT_ACCELERATION) & 0x01 ) == 1) {
			set.add(Acceleration);
		}
		if ( ((bitmask >> BIT_SHIFT_MAGNETIC) & 0x01 ) == 1) {
			set.add(Magnetic);
		}
		if ( ((bitmask >> BIT_SHIFT_PRESSURE) & 0x01 ) == 1) {
			set.add(Pressure);
		}
		if ( ((bitmask >> BIT_SHIFT_TEMPERATURE) & 0x01 ) == 1) {
			set.add(Temperature);
		}
		if ( ((bitmask >> BIT_SHIFT_HUMIDITY) & 0x01 ) == 1) {
			set.add(Humidity);
		}
		if ( ((bitmask >> BIT_SHIFT_UV) & 0x01 ) == 1) {
			set.add(UV);
		}
		if ( ((bitmask >> BIT_SHIFT_AMBIENT_LIGHT) & 0x01 ) == 1) {
			set.add(AmbientLight);
		}

		return set;
	}
}
