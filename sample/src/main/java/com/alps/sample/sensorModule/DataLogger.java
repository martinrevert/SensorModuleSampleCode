package com.alps.sample.sensorModule;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import com.alps.sample.R;
import com.alps.sample.log.Logg;
import com.alps.sample.sensorModule.enums.Sensor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * [JP] {@link SensorModule}が保持する{@link LatestData}を、CSVファイルに書き込みます。
 */
public class DataLogger {
	private static final String TAG = "Logger";

	private Context context;
	private boolean isLogging = false;
	private String pathLog;

	private File file;

	public DataLogger(Context context, String name) {
		this.context = context;

		pathLog = Environment.getExternalStorageDirectory().toString() + "/" + context.getString(R.string.app_name);
		file = new File(pathLog);
		file.mkdir();

		pathLog = Environment.getExternalStorageDirectory().toString() + "/" + context.getString(R.string.app_name) + "/" + name.replaceAll(":", "-") + ".csv";
		file = new File(pathLog);
		if (!file.exists()) {
			writeLog(Format.CSV_FORMAT_HEADER);
		}

		requestToScanMediaForUpdatingLogFile();
	}


	public boolean isLogging() {
		return isLogging;
	}

	public void isLogging(boolean isLogging) {
		this.isLogging = isLogging;
		requestToScanMediaForUpdatingLogFile();
	}

	public boolean toggleLogging() {
		isLogging = !isLogging;
		requestToScanMediaForUpdatingLogFile();
		return isLogging;
	}

	private void writeLog(String text) {
		Logg.d(TAG, "[WRITE] %s", text);

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(pathLog, true);
			fos.write(text.getBytes());
			fos.close();

			requestToScanMediaForUpdatingLogFile();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private LatestData dcLast = null;
	private int waitLock = 0;

	public void clear(SensorModule sm) {
		if (dcLast != null) {
			dcLast = null;
		}
	}

	public void release(SensorModule sm) {
		clear(sm);
		isLogging(false);
	}

	public void writeSensorData(SensorModule sm) {
		if (isLogging) {
			LatestData dcCurrent = sm.latestData;

			switch (sm.measuringMode) {
				case Slow:
				case Fast:
				case Force:
					// In Slow, Fast, or Force Mode,
					// If contains the following types in enabled sensors,
					// the sensor module sends two types packets.
					boolean shouldCheckIndex =
									sm.enabledSensors.contains(Sensor.UV) ||
									sm.enabledSensors.contains(Sensor.AmbientLight) ||
									sm.enabledSensors.contains(Sensor.Humidity) ||
									sm.enabledSensors.contains(Sensor.Temperature) ||
									sm.enabledSensors.contains(Sensor.Pressure);

					// In this case, you should wait the 2nd packet to combine data.
					if (shouldCheckIndex) {
						if (waitLock == 0) {
							++waitLock;
							return;
						}
						else {
							waitLock = 0;
						}
					}
					write(sm, sm.latestData);
					break;
				case Hybrid:
					// In Hybrid Mode,
					// Hold the previous data every time. (1)

					// If current data index equals previous index,
					// wait a new data having next index.
					if ((dcLast == null) || (dcLast.index == dcCurrent.index)) {
						Logg.d(TAG, "Wait a new data having next index. (current=%s)", dcCurrent.index);
					}
					// If NOT, write PREVIOUS data.
					else {
						write(sm, dcLast);
					}
					dcLast = dcCurrent.clone(); // (1)
					break;
			}
		}
	}

	private void write(SensorModule sm, LatestData dcCurrent) {
		if (!file.exists()) {
			writeLog(Format.CSV_FORMAT_HEADER);
		}
		writeLog(dcCurrent.makeTextForCSV(sm.enabledSensors));
	}

	public void requestToScanMediaForUpdatingLogFile() {
		Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		Uri fileContentUri = Uri.fromFile(file);
		mediaScannerIntent.setData(fileContentUri);
		context.sendBroadcast(mediaScannerIntent);
	}
}
