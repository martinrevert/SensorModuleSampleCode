package com.alps.sample.sensorModule;

import com.alps.sample.log.Logg;


/**
 * [JP] String化する際のフォーマット文字列です。
 * ロケールによって小数点やCSV区切りに使用する文字が異なる問題を吸収します。
 */
public class Format {
	private static final String TAG = "Format";

	public static final String BASE_GUI_FORMAT_MAG_XYZ = "Magnetic(X=%.2f, Y=%.2f, Z=%.2f)[uT]\n";
	public static final String BASE_GUI_FORMAT_ACC_XYZ = "Accel(X=%.5f, Y=%.5f, Z=%.5f)[G]\n";
	public static final String BASE_GUI_FORMAT_DATE = "(%04d-%02d-%02d %02d:%02d:%02d__point__%03d)";
	public static final String BASE_CSV_FORMAT_HEADER = "Time,Index,Battery,Mag_X[uT],Mag_Y[uT],Mag_Z[uT],Acc_X[G],Acc_Y[G],Acc_Z[G],UV-A[mW/cm^2],AmbientLight[Lx],Humidity[%RH],Temperature[degC],Pressure[hPa]\n";
	public static final String BASE_CSV_FORMAT_DATE = "%04d/%02d/%02d %02d:%02d:%02d__point__%03d,%d";
	public static final String BASE_CSV_FORMAT_BATTERY_VOLTAGE = ",%.2f,";
	public static final String BASE_CSV_FORMAT_MAG_XYZ = "%.2f,%.2f,%.2f,";
	public static final String BASE_CSV_FORMAT_EMPTY_XYZ = ",,,";
	public static final String BASE_CSV_FORMAT_ACC_XYZ = "%.5f,%.5f,%.5f,";
	public static final String BASE_CSV_FORMAT_UV = "%.3f,";
	public static final String BASE_CSV_FORMAT_EMPTY = ",";
	public static final String BASE_CSV_FORMAT_AMBIENT_LIGHT = "%.2f,";
	public static final String BASE_CSV_FORMAT_HUMIDITY = "%.3f,";
	public static final String BASE_CSV_FORMAT_TEMPERATURE = "%.2f,";
	public static final String BASE_CSV_FORMAT_PRESSURE = "%.3f";

	public static String GUI_FORMAT_MAG_XYZ = "Magnetic(X=%.2f, Y=%.2f, Z=%.2f)[uT]\n";
	public static String GUI_FORMAT_ACC_XYZ = "Accel(X=%.5f, Y=%.5f, Z=%.5f)[G]\n";
	public static String GUI_FORMAT_DATE = "(%04d-%02d-%02d %02d:%02d:%02d__point__%03d)";
	public static String CSV_FORMAT_HEADER = "Time,Index,Battery,Mag_X[uT],Mag_Y[uT],Mag_Z[uT],Acc_X[G],Acc_Y[G],Acc_Z[G],UV-A[mW/cm^2],AmbientLight[Lx],Humidity[%RH],Temperature[degC],Pressure[hPa]\n";
	public static String CSV_FORMAT_DATE = "%04d/%02d/%02d %02d:%02d:%02d__point__%03d,%d";
	public static String CSV_FORMAT_BATTERY_VOLTAGE = ",%.2f,";
	public static String CSV_FORMAT_MAG_XYZ = "%.2f,%.2f,%.2f,";
	public static String CSV_FORMAT_EMPTY_XYZ = ",,,";
	public static String CSV_FORMAT_ACC_XYZ = "%.5f,%.5f,%.5f,";
	public static String CSV_FORMAT_UV = "%.3f,";
	public static String CSV_FORMAT_EMPTY = ",";
	public static String CSV_FORMAT_AMBIENT_LIGHT = "%.2f,";
	public static String CSV_FORMAT_HUMIDITY = "%.3f,";
	public static String CSV_FORMAT_TEMPERATURE = "%.2f,";
	public static String CSV_FORMAT_PRESSURE = "%.3f";

	public static String DELIMITER = ",";
	public static String POINT = "__point__";

	public static void initialize() {
		String check = String.format("%.1f", 1.0);
		boolean c = check.contains(".");
		Logg.d(TAG, "check string : %s ? %b", check, c);
		if (!c) {
			Logg.d(TAG, "DELIMITER ,");
			String oldDelimiter = DELIMITER;
			DELIMITER = ";";

			GUI_FORMAT_MAG_XYZ = BASE_GUI_FORMAT_MAG_XYZ.replaceAll(oldDelimiter, DELIMITER);
			GUI_FORMAT_ACC_XYZ = BASE_GUI_FORMAT_ACC_XYZ.replaceAll(oldDelimiter, DELIMITER);
			GUI_FORMAT_DATE = BASE_GUI_FORMAT_DATE.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_HEADER = BASE_CSV_FORMAT_HEADER.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_DATE = BASE_CSV_FORMAT_DATE.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_BATTERY_VOLTAGE = BASE_CSV_FORMAT_BATTERY_VOLTAGE.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_MAG_XYZ = BASE_CSV_FORMAT_MAG_XYZ.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_EMPTY_XYZ = BASE_CSV_FORMAT_EMPTY_XYZ.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_ACC_XYZ = BASE_CSV_FORMAT_ACC_XYZ.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_UV = BASE_CSV_FORMAT_UV.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_EMPTY = BASE_CSV_FORMAT_EMPTY.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_AMBIENT_LIGHT = BASE_CSV_FORMAT_AMBIENT_LIGHT.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_HUMIDITY = BASE_CSV_FORMAT_HUMIDITY.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_TEMPERATURE = BASE_CSV_FORMAT_TEMPERATURE.replaceAll(oldDelimiter, DELIMITER);
			CSV_FORMAT_PRESSURE = BASE_CSV_FORMAT_PRESSURE.replaceAll(oldDelimiter, DELIMITER);

			String oldPoint = POINT;
			POINT = ",";
			GUI_FORMAT_DATE = GUI_FORMAT_DATE.replaceAll(oldPoint, POINT);
			CSV_FORMAT_DATE = CSV_FORMAT_DATE.replaceAll(oldPoint, POINT);
		}
		else {
			Logg.d(TAG, "DELIMITER .");

			GUI_FORMAT_MAG_XYZ = BASE_GUI_FORMAT_MAG_XYZ;
			GUI_FORMAT_ACC_XYZ = BASE_GUI_FORMAT_ACC_XYZ;
			GUI_FORMAT_DATE = BASE_GUI_FORMAT_DATE;
			CSV_FORMAT_HEADER = BASE_CSV_FORMAT_HEADER;
			CSV_FORMAT_DATE = BASE_CSV_FORMAT_DATE;
			CSV_FORMAT_BATTERY_VOLTAGE = BASE_CSV_FORMAT_BATTERY_VOLTAGE;
			CSV_FORMAT_MAG_XYZ = BASE_CSV_FORMAT_MAG_XYZ;
			CSV_FORMAT_EMPTY_XYZ = BASE_CSV_FORMAT_EMPTY_XYZ;
			CSV_FORMAT_ACC_XYZ = BASE_CSV_FORMAT_ACC_XYZ;
			CSV_FORMAT_UV = BASE_CSV_FORMAT_UV;
			CSV_FORMAT_EMPTY = BASE_CSV_FORMAT_EMPTY;
			CSV_FORMAT_AMBIENT_LIGHT = BASE_CSV_FORMAT_AMBIENT_LIGHT;
			CSV_FORMAT_HUMIDITY = BASE_CSV_FORMAT_HUMIDITY;
			CSV_FORMAT_TEMPERATURE = BASE_CSV_FORMAT_TEMPERATURE;
			CSV_FORMAT_PRESSURE = BASE_CSV_FORMAT_PRESSURE;

			String oldPoint = POINT;
			POINT = ".";
			GUI_FORMAT_DATE = GUI_FORMAT_DATE.replaceAll(oldPoint, POINT);
			CSV_FORMAT_DATE = CSV_FORMAT_DATE.replaceAll(oldPoint, POINT);
		}
	}
}
