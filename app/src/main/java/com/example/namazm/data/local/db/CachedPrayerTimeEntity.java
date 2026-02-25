package com.example.namazm.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_prayer_times")
public class CachedPrayerTimeEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "cache_id")
    private final String cacheId;

    @ColumnInfo(name = "city_name")
    private final String cityName;

    @ColumnInfo(name = "date_iso")
    private final String dateIso;

    @ColumnInfo(name = "imsak")
    private final String imsak;

    @ColumnInfo(name = "gunes")
    private final String gunes;

    @ColumnInfo(name = "ogle")
    private final String ogle;

    @ColumnInfo(name = "ikindi")
    private final String ikindi;

    @ColumnInfo(name = "aksam")
    private final String aksam;

    @ColumnInfo(name = "yatsi")
    private final String yatsi;

    public CachedPrayerTimeEntity(
            @NonNull String cacheId,
            String cityName,
            String dateIso,
            String imsak,
            String gunes,
            String ogle,
            String ikindi,
            String aksam,
            String yatsi
    ) {
        this.cacheId = cacheId;
        this.cityName = cityName;
        this.dateIso = dateIso;
        this.imsak = imsak;
        this.gunes = gunes;
        this.ogle = ogle;
        this.ikindi = ikindi;
        this.aksam = aksam;
        this.yatsi = yatsi;
    }

    @NonNull
    public String getCacheId() {
        return cacheId;
    }

    public String getCityName() {
        return cityName;
    }

    public String getDateIso() {
        return dateIso;
    }

    public String getImsak() {
        return imsak;
    }

    public String getGunes() {
        return gunes;
    }

    public String getOgle() {
        return ogle;
    }

    public String getIkindi() {
        return ikindi;
    }

    public String getAksam() {
        return aksam;
    }

    public String getYatsi() {
        return yatsi;
    }
}
