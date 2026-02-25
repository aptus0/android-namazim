package com.example.namazm.startup;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StartupPreferences {

    private static final String PREFS_NAME = "startup_prefs";
    private static final String KEY_CITY = "key_city";
    private static final String KEY_LOCATION_LABEL = "key_location_label";
    private static final String KEY_LAT = "key_lat";
    private static final String KEY_LON = "key_lon";
    private static final String KEY_LAST_SYNC_DATE = "key_last_sync_date";
    private static final String KEY_ONBOARDING_COMPLETED = "key_onboarding_completed";

    private final SharedPreferences preferences;

    public StartupPreferences(@NonNull Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveCityAndLocation(@NonNull String cityName, double latitude, double longitude) {
        saveCityAndLocation(cityName, cityName, latitude, longitude);
    }

    public void saveCityAndLocation(
            @NonNull String cityName,
            @NonNull String locationLabel,
            double latitude,
            double longitude
    ) {
        preferences.edit()
                .putString(KEY_CITY, cityName)
                .putString(KEY_LOCATION_LABEL, locationLabel)
                .putFloat(KEY_LAT, (float) latitude)
                .putFloat(KEY_LON, (float) longitude)
                .apply();
    }

    public void saveCityOnly(@NonNull String cityName) {
        saveCityOnly(cityName, cityName);
    }

    public void saveCityOnly(@NonNull String cityName, @NonNull String locationLabel) {
        preferences.edit()
                .putString(KEY_CITY, cityName)
                .putString(KEY_LOCATION_LABEL, locationLabel)
                .remove(KEY_LAT)
                .remove(KEY_LON)
                .apply();
    }

    @Nullable
    public String getSavedCity() {
        return preferences.getString(KEY_CITY, null);
    }

    @Nullable
    public String getSavedLocationLabel() {
        String label = preferences.getString(KEY_LOCATION_LABEL, null);
        if (label != null && !label.trim().isEmpty()) {
            return label;
        }
        return getSavedCity();
    }

    public boolean hasLastLocation() {
        return preferences.contains(KEY_LAT) && preferences.contains(KEY_LON);
    }

    public double getLastLatitude() {
        return preferences.getFloat(KEY_LAT, 0f);
    }

    public double getLastLongitude() {
        return preferences.getFloat(KEY_LON, 0f);
    }

    public void setLastSyncDate(@NonNull String dateIso) {
        preferences.edit().putString(KEY_LAST_SYNC_DATE, dateIso).apply();
    }

    @Nullable
    public String getLastSyncDate() {
        return preferences.getString(KEY_LAST_SYNC_DATE, null);
    }

    public boolean isOnboardingCompleted() {
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }
}
