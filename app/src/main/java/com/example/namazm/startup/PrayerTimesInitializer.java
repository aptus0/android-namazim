package com.example.namazm.startup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.namazm.data.model.City;
import com.example.namazm.data.model.PrayerSlot;
import com.example.namazm.data.remote.AladhanApi;
import com.example.namazm.data.remote.dto.aladhan.AladhanTimingsResponse;
import com.example.namazm.data.repository.NamazRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PrayerTimesInitializer {

    private final NamazRepository repository;
    private final AladhanApi aladhanApi;

    public PrayerTimesInitializer(@NonNull Context context, @NonNull NamazRepository repository) {
        this.repository = repository;

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.aladhan.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.aladhanApi = retrofit.create(AladhanApi.class);
    }

    public void ensureTodayPrayerTimes(@NonNull City city, boolean allowCacheOnly) {
        String dateIso = LocalDate.now().toString();
        boolean cacheLoaded = repository.restorePrayerTimesFromCache(city.getName(), dateIso);

        if (allowCacheOnly && cacheLoaded) {
            return;
        }

        List<PrayerSlot> fromNetwork = fetchTodayFromApi(city.getName());
        if (fromNetwork != null && !fromNetwork.isEmpty()) {
            repository.savePrayerTimesToCache(city.getName(), dateIso, fromNetwork, false);
            return;
        }

        if (cacheLoaded) {
            return;
        }

        repository.savePrayerTimesToCache(
                city.getName(),
                dateIso,
                buildFallbackSlots(city.getId()),
                true
        );
    }

    @Nullable
    private List<PrayerSlot> fetchTodayFromApi(@NonNull String cityName) {
        try {
            Response<AladhanTimingsResponse> response = aladhanApi
                    .getTimingsByCity(cityName, "Turkey", 13, 1)
                    .execute();

            if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                return null;
            }

            AladhanTimingsResponse.Timings timings = response.body().getData().getTimings();
            if (timings == null) {
                return null;
            }

            List<PrayerSlot> slots = new ArrayList<>();
            slots.add(new PrayerSlot("İmsak", sanitizeTime(timings.getFajr()), false));
            slots.add(new PrayerSlot("Güneş", sanitizeTime(timings.getSunrise()), false));
            slots.add(new PrayerSlot("Öğle", sanitizeTime(timings.getDhuhr()), false));
            slots.add(new PrayerSlot("İkindi", sanitizeTime(timings.getAsr()), false));
            slots.add(new PrayerSlot("Akşam", sanitizeTime(timings.getMaghrib()), false));
            slots.add(new PrayerSlot("Yatsı", sanitizeTime(timings.getIsha()), false));
            return slots;
        } catch (IOException exception) {
            return null;
        }
    }

    private String sanitizeTime(String source) {
        if (source == null) {
            return "00:00";
        }

        String value = source.trim();
        int spaceIndex = value.indexOf(' ');
        if (spaceIndex > 0) {
            value = value.substring(0, spaceIndex);
        }
        int plusIndex = value.indexOf('(');
        if (plusIndex > 0) {
            value = value.substring(0, plusIndex).trim();
        }

        if (value.matches("\\d{1,2}:\\d{2}")) {
            String[] parts = value.split(":");
            return String.format(Locale.US, "%02d:%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return "00:00";
    }

    private List<PrayerSlot> buildFallbackSlots(int cityId) {
        int tweak = Math.abs(cityId) % 12;
        int imsakMinute = 30 + (tweak % 20);
        int gunesMinute = (imsakMinute + 80) % 60;
        int ogleMinute = (10 + tweak) % 60;
        int ikindiMinute = (35 + tweak) % 60;
        int aksamMinute = (0 + tweak) % 60;
        int yatsiMinute = (20 + tweak) % 60;

        List<PrayerSlot> slots = new ArrayList<>();
        slots.add(new PrayerSlot("İmsak", String.format(Locale.US, "05:%02d", imsakMinute), false));
        slots.add(new PrayerSlot("Güneş", String.format(Locale.US, "07:%02d", gunesMinute), false));
        slots.add(new PrayerSlot("Öğle", String.format(Locale.US, "13:%02d", ogleMinute), false));
        slots.add(new PrayerSlot("İkindi", String.format(Locale.US, "16:%02d", ikindiMinute), false));
        slots.add(new PrayerSlot("Akşam", String.format(Locale.US, "19:%02d", aksamMinute), false));
        slots.add(new PrayerSlot("Yatsı", String.format(Locale.US, "20:%02d", yatsiMinute), false));
        return slots;
    }
}
