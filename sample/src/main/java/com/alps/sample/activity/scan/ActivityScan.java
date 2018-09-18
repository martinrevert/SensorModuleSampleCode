
package com.alps.sample.activity.scan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

import com.alps.sample.R;
import com.alps.sample.activity.base.usingBluetooth.ActivityUsingBluetooth;
import com.alps.sample.activity.connection.ActivitySensorCommunication;
import com.alps.sample.database.DatabaseHelper;
import com.alps.sample.database.DatabaseHelper_Impl;
import com.alps.sample.log.Logg;
import com.alps.sample.model.Device;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;


/**
 * [JP] 周囲にある{@link BLEDevice}をスキャンし、リストに表示します。
 * <p>
 * ユーザによって接続操作が行われた場合、
 * 選択された{@code BLEDevice}が持つ{@code BluetoothDevice}オブジェクトを収集し、
 * これを{@link ActivitySensorCommunication}に渡します。
 *
 * @see ActivityUsingBluetooth
 */
@SuppressLint("NewApi")
public class ActivityScan extends ActivityUsingBluetooth implements EasyPermissions.PermissionCallbacks {
    //
    // ---------------------------------------------------
    //     SCANNING-FILTER
    // ---------------------------------------------------
    //
    // Scanning results are filtered by Device name as below:
    //
    // code:
    //     if (BLE_DEVICE_FILTERING_ENABLE) {
    //         if (!name.matches(BLE_DEVICE_NAME_FILTERING_REGULAR_EXPRESSION)) {
    //             return;
    //         }
    //     }
    //
    // So, If you don't need this scan-filter, please modify it as you like.
    //
    public static final boolean BLE_DEVICE_FILTERING_ENABLE = true;
    public static final String BLE_DEVICE_NAME_FILTERING_REGULAR_EXPRESSION = "^SNM.*";
    //
    // ---------------------------------------------------

    public static final int DELAY_MILLIS_UPDATE_SCAN_RESULTS_FIRST = 100;
    public static final int DELAY_MILLIS_UPDATE_SCAN_RESULTS_INTERVAL = 500;

    private final String TAG = getClass().getSimpleName();

    private ListView listFoundBLEDevices;
    private ArrayAdapter<BLEDevice> adapterFoundBLEDevices;
    private LinkedHashMap<String, BLEDevice> foundBLEDevices;
    private ProgressBar progressBar;

    private static final int FINE_LOCATION_PERM = 124;

    private DatabaseHelper db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = DatabaseHelper.getDatabase(this);

        // set the view
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        setContentView(R.layout.activity_scan);
        listFoundBLEDevices = (ListView) findViewById(android.R.id.list);

        // set the tool-bar to this activity
        Toolbar mToolbar = (Toolbar) findViewById(R.id.header);
        mToolbar.setTitle(R.string.app_title);
        setSupportActionBar(mToolbar);

        progressBar = (ProgressBar) findViewById(R.id.progress_scanning);

        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions(this, perms)) {
            //I have permission, do the thing
        } else {
            EasyPermissions.requestPermissions(this, "FINE LOCATION IS MANDATORY TO SCAN BLE DEVICES", FINE_LOCATION_PERM, Manifest.permission.ACCESS_FINE_LOCATION);
        }


        // set button
        Button button = (Button) findViewById(R.id.button_select);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Parcelable> parcelableList = new ArrayList<Parcelable>();

                SparseBooleanArray array = listFoundBLEDevices.getCheckedItemPositions();
                for (int i = 0; i < foundBLEDevices.size(); i++) {
                    boolean checked = array.get(i);
                    if (checked) {
                        BLEDevice bleDevice = adapterFoundBLEDevices.getItem(i);
                        BluetoothDevice bluetoothDevice = bleDevice.getBluetoothDevice();
                        parcelableList.add(bluetoothDevice);

                        Logg.d(TAG, "position : %d (%s)", i, bluetoothDevice);
                    }
                }

                int size = parcelableList.size();
                if ((0 < size) && (size <= 4)) {
                    toggleScanning(false);
                    connectToTarget(parcelableList);
                } else {
                    Toast.makeText(ActivityScan.this, "Please select the sensor modules less than 4.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void connectToTarget(List<Parcelable> parcelableList) {
        int size = parcelableList.size();
        if (size > 0) {
            final Parcelable parcelables[] = new Parcelable[parcelableList.size()];
            parcelableList.toArray(parcelables);


            String body = getString(R.string.connect_to_following_devices);

            for (Parcelable parcelable : parcelables) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) parcelable;
                Logg.d(TAG, "putExtra : %s", bluetoothDevice);
                body += String.format("\n%s (%s)", (bluetoothDevice.getName() == null) ? getString(R.string.no_device_name) : bluetoothDevice.getName(), bluetoothDevice.getAddress());

                //Todo guardar en DB aqui cada vez que itera por ALPS BLE a conectar
                final Device bleDev = new Device(bluetoothDevice.getAddress());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // inserta mac adrres en local db
                        db.DeviceDAO().insert(bleDev);
                    }
                }).start();
            }

      //      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      //      builder.setMessage(body);
      //      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      //          @Override
      //          public void onClick(DialogInterface dialog, int which) {
                    adapterFoundBLEDevices.clear();

                    Intent intent = new Intent(getApplicationContext(), ActivitySensorCommunication.class);
                    intent.putExtra(ActivitySensorCommunication.EXTRAS_DEVICES, parcelables);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
      //          }
      //      });
      //      builder.setNegativeButton(R.string.dialog_button_cancel, null);
      //      builder.setCancelable(false);
      //      builder.show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initializeScanList() {
        foundBLEDevices = new LinkedHashMap<String, BLEDevice>();
        adapterFoundBLEDevices = new AdapterBLEDevice(getApplicationContext(), R.layout.custom_row, foundBLEDevices);
        listFoundBLEDevices.setAdapter(adapterFoundBLEDevices);
    }

    @Override
    protected void onResume() {
        super.onResume();

        initializeScanList();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        toggleScanning(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStop() {
        super.onStop();

        updateLoopHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (foundBLEDevices != null) {
            foundBLEDevices.clear();
            foundBLEDevices = null;
        }

        listFoundBLEDevices = null;
    }

    @Override
    protected void onChangeBluetoothState(boolean on) {
        toggleScanning(on);
    }

    @Override
    protected void onFoundBLEDevice(BLEDevice bleDevice) {
        String name = bleDevice.getName();

        if (BLE_DEVICE_FILTERING_ENABLE) {
            if (!name.matches(BLE_DEVICE_NAME_FILTERING_REGULAR_EXPRESSION)) {
                return;
            }
        }

        synchronized (foundBLEDevices) {
            foundBLEDevices.put(bleDevice.getAddress(), bleDevice);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        // restore action bar
        if (!isScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                toggleScanning(true);
                break;
            case R.id.menu_stop:
                toggleScanning(false);
                break;
            case R.id.action_clear:
                synchronized (foundBLEDevices) {
                    initializeScanList();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.msg_finish_app));
            builder.setPositiveButton(getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityScan.this.finish();
                }
            });
            builder.setNegativeButton(R.string.dialog_button_cancel, null);
            builder.setCancelable(false);
            builder.show();
        }
        return super.dispatchKeyEvent(event);
    }

    protected void toggleScanning(final boolean enable) {
        progressBar.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        updateLoopHandler.removeCallbacksAndMessages(null);

        invalidateOptionsMenu();

        enableScanning(enable);

        if (enable) {
            // Start runnable-loop to update ListView at fixed intervals
            updateLoopHandler.postDelayed(runnableLoopingUpdate, DELAY_MILLIS_UPDATE_SCAN_RESULTS_FIRST);
        }
    }

    private Handler updateLoopHandler = new Handler(Looper.getMainLooper());
    private Runnable runnableLoopingUpdate = new Runnable() {
        @Override
        public void run() {
            if (isScanning) {
                synchronized (foundBLEDevices) {
                    if (adapterFoundBLEDevices != null) {
                        adapterFoundBLEDevices.clear();
                        adapterFoundBLEDevices.addAll(foundBLEDevices.values());
                        adapterFoundBLEDevices.notifyDataSetChanged();
                    }
                }

                updateLoopHandler.postDelayed(this, DELAY_MILLIS_UPDATE_SCAN_RESULTS_INTERVAL);
            }
        }
    };

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }
}
