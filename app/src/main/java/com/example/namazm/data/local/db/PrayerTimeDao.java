package com.example.namazm.data.local.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PrayerTimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedPrayerTimeEntity> items);

    @Query("SELECT * FROM cached_prayer_times WHERE city_name = :cityName ORDER BY date_iso ASC")
    List<CachedPrayerTimeEntity> getByCity(String cityName);

    @Query("SELECT * FROM cached_prayer_times WHERE city_name = :cityName AND date_iso = :dateIso LIMIT 1")
    CachedPrayerTimeEntity getByCityAndDate(String cityName, String dateIso);
}
