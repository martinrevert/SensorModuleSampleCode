
package com.alps.sample.activity.scan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.LinkedHashMap;


/**
 * [JP] {@link BLEDevice}の情報を表示するViewを提供する{@code android.widget.ArrayAdapter}です。
 */
public class AdapterBLEDevice extends ArrayAdapter<BLEDevice> {
    private final String TAG = getClass().getSimpleName();

    private Context context;
    private int resourceId;
    private LayoutInflater inflater;

    private ViewHolder holder = null;

    private LinkedHashMap<String,BLEDevice> map;

    public AdapterBLEDevice(Context context, int resourceId, LinkedHashMap<String, BLEDevice> map) {
        super(context, resourceId);
        this.map = map;
        this.context = context;
        this.resourceId = resourceId;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private static StringBuffer sb;

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        holder = new ViewHolder();
        final BLEDevice item = getItem(position);
        // get view holder
        if (view == null) {
            view = inflater.inflate(resourceId, parent, false);
            holder.text1 = (TextView) view.findViewById(android.R.id.text1);
            holder.text2 = (TextView) view.findViewById(android.R.id.text2);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // set Main message
        holder.text1.setText(item.getName());
        // set Sub message
        holder.text2.setText("Address: " + item.getAddress() + " ( RSSI: " + item.getRssi() + "dBm )");
        holder = null;

        return view;
    }

    @Override
    public BLEDevice getItem(int position) {
//        return super.getItem(position);
        BLEDevice[] items = new BLEDevice[map.size()];
        map.values().toArray(items);
        return items[position];
    }

    public static class ViewHolder {
        TextView text1;
        TextView text2;
    }

}
