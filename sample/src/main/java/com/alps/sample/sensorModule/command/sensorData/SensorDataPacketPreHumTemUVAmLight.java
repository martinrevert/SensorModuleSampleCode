package com.alps.sample.sensorModule.command.sensorData;


/**
 * [JP]気圧センサ・湿度センサ・温度センサ・UVセンサ・環境光センサのアナログセンサ値、
 * および、モジュールのタイムスタンプのうち、年・月・日を通知します。
 */
public class SensorDataPacketPreHumTemUVAmLight extends SensorDataPacket {
	private static final String TAG = "SensorDataPacketD";
	public static final byte EVENT_CODE = (byte) 0xF3;

	private int pressure = 0;
	private int humidity = 0;
	private int temperature = 0;
	private int uv = 0;
	private int ambientLight = 0;
	private int day = 1;
	private int month = 1;
	private int year = 2000;


	@Override
	public byte eventCode() {
		return EVENT_CODE;
	}


	@Override
	public String toString() {
		return String.format("<%s : i:%d pressure[%d] humidity[%d] temperature[%d] uv[%d] ambientLight[%d] %04d-%02d-%02d>", getClass().getSimpleName(), index, pressure, humidity, temperature, uv, ambientLight, year, month, day);
	}

	/**
	 * Parse packetBytes
	 */
	public SensorDataPacketPreHumTemUVAmLight(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			// unsigned short
			pressure = ((receivedCommand[3]<<8) & 0x0FF00) | (receivedCommand[2]&0x0FF);

			// unsigned short
			humidity = ((receivedCommand[5]<<8) & 0x0FF00) | (receivedCommand[4]&0x0FF);

			// unsigned short
			temperature = ((receivedCommand[7]<<8) & 0x0FF00) | (receivedCommand[6]&0x0FF);

			// signed short
			uv = (short)(((receivedCommand[9]<<8) & 0x0FF00) | (receivedCommand[8]&0x0FF));

			// signed short
			ambientLight = (short)(((receivedCommand[11]<<8) & 0x0FF00) | (receivedCommand[10]&0x0FF));

			day = receivedCommand[16] & 0x0FF;
			month = receivedCommand[17] & 0x0FF;
			year = (receivedCommand[18] & 0x0FF) + 2000;

			index = receivedCommand[19] & 0x0FF;
		}
	}

	public int getPressure() {
		return pressure;
	}

	public int getHumidity() {
		return humidity;
	}

	public int getTemperature() {
		return temperature;
	}

	public int getUv() {
		return uv;
	}

	public int getAmbientLight() {
		return ambientLight;
	}

	public int getDay() {
		return day;
	}

	public int getMonth() {
		return month;
	}

	public int getYear() {
		return year;
	}
}
