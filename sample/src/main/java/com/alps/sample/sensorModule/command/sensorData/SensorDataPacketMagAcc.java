package com.alps.sample.sensorModule.command.sensorData;


/**
 * [JP]地磁気センサ・加速度センサのアナログセンサ値、
 * および、モジュールのタイムスタンプのうち、時・分・秒・ミリ秒を通知します。
 */
public class SensorDataPacketMagAcc extends SensorDataPacket {
	private static final String TAG = "SensorDataPacketMagAcc";
	public static final byte EVENT_CODE = (byte) 0xF2;

	private int magX = 0;
	private int magY = 0;
	private int magZ = 0;
	private int accX = 0;
	private int accY = 0;
	private int accZ = 0;
	private int ms = 0;
	private int second = 0;
	private int minute = 0;
	private int hour = 0;


	@Override
	public byte eventCode() {
		return EVENT_CODE;
	}


	@Override
	public String toString() {
		return String.format("<%s : i:%d mag[%d:%d:%d] acc[%d:%d:%d] %02d:%02d:%02d.%03d>", getClass().getSimpleName(), index, magX, magY, magZ, accX, accY, accZ, hour, minute, second, ms);
	}

	/**
	 * Parse packetBytes
	 */
	public SensorDataPacketMagAcc(byte[] receivedCommand) {
		super(receivedCommand);
		if (validCommand) {
			// signed short
			magX = (short)(((receivedCommand[3]<<8) & 0xFF00) | (receivedCommand[2]&0xFF));
			magY = (short)(((receivedCommand[5]<<8) & 0xFF00) | (receivedCommand[4]&0xFF));
			magZ = (short)(((receivedCommand[7]<<8) & 0xFF00) | (receivedCommand[6]&0xFF));

			// signed short
			accX = (short)(((receivedCommand[9]<<8) & 0xFF00) | (receivedCommand[8]&0xFF));
			accY = (short)(((receivedCommand[11]<<8) & 0xFF00) | (receivedCommand[10]&0xFF));
			accZ = (short)(((receivedCommand[13]<<8) & 0xFF00) | (receivedCommand[12]&0xFF));

			ms = ((receivedCommand[15]<<8) & 0x0FF00) | (receivedCommand[14]&0x0FF);
			second = receivedCommand[16] & 0x0FF;
			minute = receivedCommand[17] & 0x0FF;
			hour = receivedCommand[18] & 0x0FF;

			index = receivedCommand[19] & 0x0FF;
		}
	}

	public int getMagX() {
		return magX;
	}

	public int getMagY() {
		return magY;
	}

	public int getMagZ() {
		return magZ;
	}

	public int getAccX() {
		return accX;
	}

	public int getAccY() {
		return accY;
	}

	public int getAccZ() {
		return accZ;
	}

	public int getMs() {
		return ms;
	}

	public int getSecond() {
		return second;
	}

	public int getMinute() {
		return minute;
	}

	public int getHour() {
		return hour;
	}
}
