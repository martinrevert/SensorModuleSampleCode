package com.alps.sample.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "device_table")
public class Device {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "MAC")
    private String MAC;

    @ColumnInfo(name="uuid")
    private String uuid;


    public Device(@NonNull String MAC) {
        this.MAC = MAC;
    }

    @NonNull
    public String getMAC() {
        return MAC;
    }

    public void setMAC(@NonNull String MAC) {
        this.MAC = MAC;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
