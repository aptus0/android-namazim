package com.example.namazm.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationSettingsState {

    private final boolean prayerNotificationsEnabled;
    private final List<PrayerNotificationConfig> prayerConfigs;
    private final boolean hadithDailyEnabled;
    private final String hadithDailyTime;
    private final boolean hadithNearPrayerEnabled;

    public NotificationSettingsState(
            boolean prayerNotificationsEnabled,
            List<PrayerNotificationConfig> prayerConfigs,
            boolean hadithDailyEnabled,
            String hadithDailyTime,
            boolean hadithNearPrayerEnabled
    ) {
        this.prayerNotificationsEnabled = prayerNotificationsEnabled;
        this.prayerConfigs = Collections.unmodifiableList(new ArrayList<>(prayerConfigs));
        this.hadithDailyEnabled = hadithDailyEnabled;
        this.hadithDailyTime = hadithDailyTime;
        this.hadithNearPrayerEnabled = hadithNearPrayerEnabled;
    }

    public boolean isPrayerNotificationsEnabled() {
        return prayerNotificationsEnabled;
    }

    public List<PrayerNotificationConfig> getPrayerConfigs() {
        return prayerConfigs;
    }

    public boolean isHadithDailyEnabled() {
        return hadithDailyEnabled;
    }

    public String getHadithDailyTime() {
        return hadithDailyTime;
    }

    public boolean isHadithNearPrayerEnabled() {
        return hadithNearPrayerEnabled;
    }

    public static NotificationSettingsState defaultState() {
        return new NotificationSettingsState(
                true,
                PrayerNotificationConfig.defaultList(),
                true,
                "09:00",
                true
        );
    }
}
