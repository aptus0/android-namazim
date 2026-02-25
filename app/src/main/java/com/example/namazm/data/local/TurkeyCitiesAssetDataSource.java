package com.example.namazm.data.local;

import android.content.Context;

import com.example.namazm.data.model.City;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurkeyCitiesAssetDataSource {

    private static final String FILE_NAME = "cities_tr.json";

    private final Context context;
    private final Gson gson;

    public TurkeyCitiesAssetDataSource(Context context, Gson gson) {
        this.context = context;
        this.gson = gson;
    }

    public List<City> loadCities() {
        Type listType = new TypeToken<List<City>>() {
        }.getType();

        try (InputStream inputStream = context.getAssets().open(FILE_NAME);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            List<City> cities = gson.fromJson(reader, listType);
            return cities == null ? Collections.emptyList() : cities;
        } catch (IOException exception) {
            return fallbackCities();
        }
    }

    private List<City> fallbackCities() {
        List<City> fallback = new ArrayList<>();
        fallback.add(new City(34, "İstanbul", 41.005236, 28.976018));
        fallback.add(new City(6, "Ankara", 39.925533, 32.866287));
        fallback.add(new City(35, "İzmir", 38.423733, 27.142826));
        fallback.add(new City(41, "Kocaeli", 40.853270, 29.881520));
        fallback.add(new City(16, "Bursa", 40.195000, 29.060000));
        return fallback;
    }
}
