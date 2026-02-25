package com.example.namazm.data.local.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HadithDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HadithEntity> items);

    @Query("SELECT COUNT(*) FROM hadith_items")
    int count();

    @Query("SELECT * FROM hadith_items ORDER BY id ASC")
    List<HadithEntity> getAll();
}
