package com.alps.sample.sensorModule;

import com.alps.sample.sensorModule.enums.Sensor;

import java.util.Set;


/**
 * [JP] センサモジュールから送られてきたセンサデータの最新値を保持します。
 */
public class LatestData {
	public float magX = 0;
	public float magY = 0;
	public float magZ = 0;
	public float accX = 0;
	public float accY = 0;
	public float accZ = 0;
	public float pressure = 0;
	public float humidity = 0;
	public float temperature = 0;
	public float uv = 0;
	public float ambientLight = 0;

	public int ms = 0;
	public int second = 0;
	public int minute = 0;
	public int hour = 0;
	public int day = 1;
	public int month = 1;
	public int year = 2000;

	public float batteryVoltage = 0;

	public int index = -1;

	public LatestData clone() {
		LatestData clone = new LatestData();
		clone.magX = this.magX;
		clone.magY = this.magY;
		clone.magZ = this.magZ;
		clone.accX = this.accX;
		clone.accY = this.accY;
		clone.accZ = this.accZ;
		clone.pressure = this.pressure;
		clone.humidity = this.humidity;
		clone.temperature = this.temperature;
		clone.uv = this.uv;
		clone.ambientLight = this.ambientLight;

		clone.ms = this.ms;
		clone.second = this.second;
		clone.minute = this.minute;
		clone.hour = this.hour;
		clone.day = this.day;
		clone.month = this.month;
		clone.year = this.year;

		clone.batteryVoltage = this.batteryVoltage;

		clone.index = this.index;
		return clone;
	}

	public String makeTextForGUI(Set<Sensor> enabledSensors) {
		StringBuffer sbGUI = new StringBuffer();

		sbGUI.delete(0, sbGUI.length());
		String text;

		if (enabledSensors.contains(Sensor.Magnetic)) {
			text = String.format(
					Format.GUI_FORMAT_MAG_XYZ
					, magX, magY, magZ);
			sbGUI.append(text);
		}
		if (enabledSensors.contains(Sensor.Acceleration)) {
			text = String.format(
					Format.GUI_FORMAT_ACC_XYZ
					, accX, accY, accZ);
			sbGUI.append(text);
		}
		if (enabledSensors.contains(Sensor.UV)) {
			text = String.format("UV-A=%.3f[mW/cm2]\n", uv);
			sbGUI.append(text);
		}
		if (enabledSensors.contains(Sensor.AmbientLight)) {
			text = String.format("AmbientLight=%.2f[Lx]\n", ambientLight);
			sbGUI.append(text);
		}
		if (enabledSensors.contains(Sensor.Humidity)) {
			text = String.format("Humidity=%.3f[%%RH]\n", humidity);
			sbGUI.append(text);
		}
		if (enabledSensors.contains(Sensor.Temperature)) {
			text = String.format("Temperature=%.2f[degC]\n", temperature);
			sbGUI.append(text);
		}
		if (enabledSensors.contains(Sensor.Pressure)) {
			text = String.format("Pressure=%.3f[hPa]\n", pressure);
			sbGUI.append(text);
		}

		text = String.format(
				Format.GUI_FORMAT_DATE
				, year
				, month
				, day
				, hour
				, minute
				, second
				, ms);
		sbGUI.append(text);

		return sbGUI.toString();
	}

	public String makeTextForCSV(Set<Sensor> enabledSensors) {
		StringBuffer sbCSV = new StringBuffer();
		sbCSV.delete(0, sbCSV.length());

		String text;
		text = String.format(
				Format.CSV_FORMAT_DATE
				, year, month, day, hour, minute, second, ms, index);
		sbCSV.append(text);
		sbCSV.append(String.format(Format.CSV_FORMAT_BATTERY_VOLTAGE, batteryVoltage));
		if (enabledSensors.contains(Sensor.Magnetic)) {
			text = String.format(Format.CSV_FORMAT_MAG_XYZ, magX, magY, magZ);
			sbCSV.append(text);
		}
		else {
			sbCSV.append(Format.CSV_FORMAT_EMPTY_XYZ);
		}
		if (enabledSensors.contains(Sensor.Acceleration)) {
			text = String.format(Format.CSV_FORMAT_ACC_XYZ, accX, accY, accZ);
			sbCSV.append(text);
		}
		else {
			sbCSV.append(Format.CSV_FORMAT_EMPTY_XYZ);
		}
		if (enabledSensors.contains(Sensor.UV)) {
			text = String.format(Format.CSV_FORMAT_UV, uv);
			sbCSV.append(text);
		}
		else {
			sbCSV.append(Format.CSV_FORMAT_EMPTY);
		}
		if (enabledSensors.contains(Sensor.AmbientLight)) {
			text = String.format(Format.CSV_FORMAT_AMBIENT_LIGHT, ambientLight);
			sbCSV.append(text);
		}
		else {
			sbCSV.append(Format.CSV_FORMAT_EMPTY);
		}
		if (enabledSensors.contains(Sensor.Humidity)) {
			text = String.format(Format.CSV_FORMAT_HUMIDITY, humidity);
			sbCSV.append(text);
		}
		else {
			sbCSV.append(Format.CSV_FORMAT_EMPTY);
		}
		if (enabledSensors.contains(Sensor.Temperature)) {
			text = String.format(Format.CSV_FORMAT_TEMPERATURE, temperature);
			sbCSV.append(text);
		}
		else {
			sbCSV.append(Format.CSV_FORMAT_EMPTY);
		}
		if (enabledSensors.contains(Sensor.Pressure)) {
			text = String.format(Format.CSV_FORMAT_PRESSURE, pressure);
			sbCSV.append(text);
		}
		sbCSV.append("\n");

		return sbCSV.toString();
	}
}
