package com.alps.sample;

import android.app.Application;
import com.alps.sample.sensorModule.Format;


/**
 * [JP] アプリケーション起動タイミングで生成され、初期化操作を実行します。
 */
public class AppDelegate extends Application {
	public AppDelegate() {
		Format.initialize();
	}
}
