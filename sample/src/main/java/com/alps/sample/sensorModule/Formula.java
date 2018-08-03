package com.alps.sample.sensorModule;

import com.alps.sample.log.Logg;
import com.alps.sample.sensorModule.enums.AccelerationSensorRange;
import com.alps.sample.sensorModule.enums.Sensor;

import java.util.Set;


/**
 * [JP]センサモジュールから送られてきたセンサデータ値をデジタル値に変換します。
 * これらの式については、モジュールに実装されているセンサの仕様を確認してください。
 */
public class Formula {
	private static final String TAG = "Formula";


	public Formula() {
	}

	/**
	 * [JP]センサアナログ値をデジタル値に変換します。
	 * ただし、加速度センサ値を変換する場合は{@link Formula#calcAcc(int, AccelerationSensorRange)}を使用してください。
	 *
	 * @param type アナログ値が何のセンサであるかを指定します。
	 * @param rawValue  センサのアナログ値です。
	 * @return デジタル値に変換されたfloat値です。
	 */
	public static float calc(Sensor type, int rawValue) {
		float value;
		switch (type) {
			case Temperature:
				value = (float) (0.02 * ((float)rawValue) - 41.92);
				break;
			case Humidity:
				value = (float) (0.015625 * ((float)rawValue) - 14);
				break;
			case Acceleration:
				Logg.d(TAG, "[ERROR] Use Formula#calcAcc(rawValue, accelerationSensorRange) instead of this method.");
				value = 0;
				break;
			case Magnetic:
				value = (float) (((float)rawValue) * 0.15);
				break;
			case Pressure:
				value = (float) (0.013123 * ((float)rawValue) + 250);
				break;
			case UV: {
				if (rawValue < 0) {
					rawValue = 0;
				}
				value = ((float) rawValue) / 100;
				value /= 0.388f; // correction value for sensor's casing.
				break;
			}
			case AmbientLight: {
				if (rawValue < 0) {
					rawValue = 0;
				}
				value = ((float)rawValue) * 20;
				value /= 0.928; // correction value for sensor's casing.
				break;
			}
			default:
				value = rawValue;
				break;
		}
		return value;
	}

	/**
	 * [JP]20160916 ver1.2以後は未使用
	 */
	public static float correctAmbientLightValue(Set<Sensor> enablingSensors, float baseAmbientLightLx, float uv) {
		float correctedLx;

		if (enablingSensors.contains(Sensor.UV) && enablingSensors.contains(Sensor.AmbientLight)) {
			float sensitivityRatio;
			if (uv < 0.814f) {
				sensitivityRatio = 0.3102f * uv + 0.8525f;
			}
			else {
				sensitivityRatio = 0.03683f * uv + 1.0750f;
			}

			correctedLx = baseAmbientLightLx / sensitivityRatio;
		}
		else {
			correctedLx = baseAmbientLightLx;
		}

		return correctedLx;
	}

	/**
	 * [JP]加速度センサのアナログ値をデジタル値に変換します。
	 * 正しい値で変換するためには、事前に読み出した{@link AccelerationSensorRange}の情報が必要です。
	 * @see com.alps.sample.sensorModule.command.control.CtrlCmdAccelerationSensorRange
	 *
	 * @param rawAccValue 加速度センサのアナログ値です。
	 * @param range 加速度センサの現在の重力加速度レンジです。
	 * @return デジタル値に変換されたfloat値です。
	 */
	public static float calcAcc(int rawAccValue, AccelerationSensorRange range) {
		float value = rawAccValue / (8192.0f / range.toMagnification());
		return value;
	}

	/**
	 * [JP]アナログ値が無効データであるかをチェックします。
	 * ※サンプルコード内では使用していません。
	 *
	 * @param type アナログ値が何のセンサであるかを指定します。
	 * @param rawValue センサのアナログ値です。
	 * @return 有効である場合はtrueを、無効である場合はfalseを返します。
	 */
	public static boolean checkValue(Sensor type, int rawValue) {
		int rawValueMeanDisabling;
		switch (type) {
			case Pressure:
				rawValueMeanDisabling = 0x00000;
				break;
			default:
				rawValueMeanDisabling = 0x08000;
				break;
		}
		return !(rawValueMeanDisabling == (rawValue & 0x0FFFF));
	}
}
