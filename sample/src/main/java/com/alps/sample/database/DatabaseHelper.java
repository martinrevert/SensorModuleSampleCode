package com.alps.sample.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.alps.sample.dao.DeviceDAO;
import com.alps.sample.model.Device;

@Database(entities = {Device.class}, version = 1, exportSchema = false)
public abstract class DatabaseHelper extends RoomDatabase {

    public abstract DeviceDAO DeviceDAO();

    private static volatile DatabaseHelper INSTANCE;

    public static DatabaseHelper getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            DatabaseHelper.class, "device_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}