package com.example.namazm.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                CachedPrayerTimeEntity.class,
                HadithEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class NamazDatabase extends RoomDatabase {

    public abstract PrayerTimeDao prayerTimeDao();

    public abstract HadithDao hadithDao();
}
