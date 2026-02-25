package com.example.namazm.data.model;

import java.util.List;

public class PrayerTimesOverview {

    private final String cityName;
    private final String nextPrayerName;
    private final String nextPrayerTime;
    private final String remainingTime;
    private final int nextPrayerProgress;
    private final boolean notificationsEnabled;
    private final boolean citySelectionRequired;
    private final boolean offlineMode;
    private final String lastUpdatedLabel;
    private final List<PrayerSlot> todaySlots;

    public PrayerTimesOverview(
            String cityName,
            String nextPrayerName,
            String nextPrayerTime,
            String remainingTime,
            int nextPrayerProgress,
            boolean notificationsEnabled,
            boolean citySelectionRequired,
            boolean offlineMode,
            String lastUpdatedLabel,
            List<PrayerSlot> todaySlots
    ) {
        this.cityName = cityName;
        this.nextPrayerName = nextPrayerName;
        this.nextPrayerTime = nextPrayerTime;
        this.remainingTime = remainingTime;
        this.nextPrayerProgress = nextPrayerProgress;
        this.notificationsEnabled = notificationsEnabled;
        this.citySelectionRequired = citySelectionRequired;
        this.offlineMode = offlineMode;
        this.lastUpdatedLabel = lastUpdatedLabel;
        this.todaySlots = todaySlots;
    }

    public String getCityName() {
        return cityName;
    }

    public String getNextPrayerName() {
        return nextPrayerName;
    }

    public String getNextPrayerTime() {
        return nextPrayerTime;
    }

    public String getRemainingTime() {
        return remainingTime;
    }

    public int getNextPrayerProgress() {
        return nextPrayerProgress;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean isCitySelectionRequired() {
        return citySelectionRequired;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public String getLastUpdatedLabel() {
        return lastUpdatedLabel;
    }

    public List<PrayerSlot> getTodaySlots() {
        return todaySlots;
    }
}
