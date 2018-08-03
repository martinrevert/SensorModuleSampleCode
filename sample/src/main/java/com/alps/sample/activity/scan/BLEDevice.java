
package com.alps.sample.activity.scan;

import android.bluetooth.BluetoothDevice;


/**
 * [JP]スキャン結果をまとめるデータクラスです。
 */
public class BLEDevice extends Object {
    public static final String NO_NAME = "NO-NAME";

    private BluetoothDevice bluetoothDevice;
    private String name;
    private String address;
    private String rssi;

    public BLEDevice(BluetoothDevice bluetoothDevice, String rssi, byte[] advertisingData) {
        this.rssi = rssi;
        this.bluetoothDevice = bluetoothDevice;

        String name = null;
        // name = bluetoothDevice.getName();    // this properties will be cached by bluetoothAdapter.
        // so we should NOT refer it.
        String shortName = null;
        String completeName = null;
        int length = advertisingData.length;
        for (int i=0; i<length; i++) {
            int len = advertisingData[i];
            if (len != 0x00) {
                int evt = advertisingData[i+1];
                switch (evt) {
                    case 0x08:
                        shortName = new String(advertisingData, i+2, len-1);
                        break;
                    case 0x09:
                        completeName = new String(advertisingData, i+2, len-1);
                        break;
                    default:
                        break;
                }
                i += len;
            }
            else {
                i += length;
            }
        }

        if (completeName != null) {
            name = completeName;
        }
        if (shortName != null) {
            name = shortName;
        }
        if (name == null) {
            name = NO_NAME;
        }
        this.name = name;

        this.address = bluetoothDevice.getAddress();
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public String getRssi() {
        return rssi;
    }
}
