package com.alps.sample.dao;


import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.alps.sample.model.Device;

import java.util.List;

@Dao
public interface DeviceDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Device device);

    @Query("SELECT * FROM device_table ORDER BY MAC ASC")
    public List<Device> getAllWords();

    @Query("SELECT * FROM device_table WHERE MAC = :mac")
    public Device getDevicebyMAC(String mac);

    @Query("SELECT * FROM device_table WHERE uuid = :uuid")
    public Device getDevicebyUuid(String uuid);

}
