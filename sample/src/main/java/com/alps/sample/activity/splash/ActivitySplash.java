
package com.alps.sample.activity.splash;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import com.alps.sample.R;
import com.alps.sample.activity.scan.ActivityScan;

/**
 * [JP] スプラッシュ画面を表示します。
 */
public class ActivitySplash extends AppCompatActivity implements Runnable {
    // get this activity
    private Activity activity = this;
    // define request code
    private final int REQUEST_ENABLE_BLUETOOTH = 0;
    // define splash hold time
    private final long SPLASH_HOLD_TIME = 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set view
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        setContentView(R.layout.activity_splash);

        // Set textView
        TextView textView = (TextView) findViewById(R.id.textAppInfoVersion);
        String versionName = "";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            versionName = getString(R.string.label_version, packageInfo.versionName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        textView.setText(versionName);

        // get bluetooth adapter
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        boolean isFeature = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        if (!isFeature || adapter == null) {
            // show dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(getString(R.string.msg_bluetooth_useless));
            builder.setNegativeButton(R.string.dialog_button_cancel, null);
            builder.setPositiveButton(R.string.dialog_button_ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            builder.setCancelable(false);
            builder.show();
        } else {
            if (adapter.isEnabled()) {
                // show splash
                Handler handler = new Handler();
                handler.postDelayed(this, SPLASH_HOLD_TIME);
            } else {
                // show dialog
                Intent intentStart = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentStart, REQUEST_ENABLE_BLUETOOTH);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // run gc
        System.gc();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                // show splash
                Handler handler = new Handler();
                handler.postDelayed(this, SPLASH_HOLD_TIME);
            } else {
                // finish activity
                finish();
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void run() {
        // check storage state
        String stateStorage = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(stateStorage)) {
            // show dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(getString(R.string.msg_unmount_storage));
            builder.setNegativeButton(R.string.dialog_button_cancel, null);
            builder.setPositiveButton(R.string.dialog_button_ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            builder.setCancelable(false);
            builder.show();
        } else {
            // start activity
            Intent intent = new Intent(getApplicationContext(), ActivityScan.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }
}
