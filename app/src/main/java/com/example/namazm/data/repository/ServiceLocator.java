package com.example.namazm.data.repository;

import android.content.Context;

import com.example.namazm.data.local.TurkeyCitiesAssetDataSource;
import com.example.namazm.data.local.TurkeyDistrictsAssetDataSource;
import com.example.namazm.data.local.db.NamazDatabase;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import androidx.room.Room;

public final class ServiceLocator {

    private static volatile NamazRepository repository;

    private ServiceLocator() {
    }

    public static void init(Context context) {
        if (repository != null) {
            return;
        }

        synchronized (ServiceLocator.class) {
            if (repository == null) {
                Gson gson = new GsonBuilder().create();
                TurkeyCitiesAssetDataSource cityDataSource = new TurkeyCitiesAssetDataSource(
                        context.getApplicationContext(),
                        gson
                );
                TurkeyDistrictsAssetDataSource districtDataSource = new TurkeyDistrictsAssetDataSource(
                        context.getApplicationContext(),
                        gson
                );

                NamazDatabase database = Room.databaseBuilder(
                        context.getApplicationContext(),
                        NamazDatabase.class,
                        "namaz_db"
                ).fallbackToDestructiveMigration().build();

                repository = new InMemoryNamazRepository(
                        context.getApplicationContext(),
                        cityDataSource,
                        districtDataSource,
                        database.prayerTimeDao(),
                        database.hadithDao()
                );
            }
        }
    }

    public static NamazRepository provideRepository() {
        if (repository == null) {
            throw new IllegalStateException("ServiceLocator is not initialized. Call init() first.");
        }
        return repository;
    }
}
