package com.example.namazm.data.model;

import java.util.ArrayList;
import java.util.List;

public class PrayerNotificationConfig {

    public static final String KEY_IMSAK = "imsak";
    public static final String KEY_GUNES = "gunes";
    public static final String KEY_OGLE = "ogle";
    public static final String KEY_IKINDI = "ikindi";
    public static final String KEY_AKSAM = "aksam";
    public static final String KEY_YATSI = "yatsi";

    public static final String MODE_NOTIFICATION = "notification";
    public static final String MODE_ALARM = "alarm";

    public static final String SOUND_DEFAULT = "default";
    public static final String SOUND_EZAN_1 = "ezan_1";
    public static final String SOUND_EZAN_2 = "ezan_2";
    public static final String SOUND_PHONE = "phone";

    private final String prayerKey;
    private final String displayName;
    private final boolean enabled;
    private final int offsetMinutes;
    private final String mode;
    private final String sound;

    public PrayerNotificationConfig(
            String prayerKey,
            String displayName,
            boolean enabled,
            int offsetMinutes,
            String mode,
            String sound
    ) {
        this.prayerKey = prayerKey;
        this.displayName = displayName;
        this.enabled = enabled;
        this.offsetMinutes = offsetMinutes;
        this.mode = mode;
        this.sound = sound;
    }

    public String getPrayerKey() {
        return prayerKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getOffsetMinutes() {
        return offsetMinutes;
    }

    public String getMode() {
        return mode;
    }

    public String getSound() {
        return sound;
    }

    public PrayerNotificationConfig withEnabled(boolean newValue) {
        return new PrayerNotificationConfig(prayerKey, displayName, newValue, offsetMinutes, mode, sound);
    }

    public PrayerNotificationConfig withOffsetMinutes(int newValue) {
        return new PrayerNotificationConfig(prayerKey, displayName, enabled, newValue, mode, sound);
    }

    public PrayerNotificationConfig withMode(String newValue) {
        return new PrayerNotificationConfig(prayerKey, displayName, enabled, offsetMinutes, newValue, sound);
    }

    public PrayerNotificationConfig withSound(String newValue) {
        return new PrayerNotificationConfig(prayerKey, displayName, enabled, offsetMinutes, mode, newValue);
    }

    public static List<PrayerNotificationConfig> defaultList() {
        List<PrayerNotificationConfig> items = new ArrayList<>();
        items.add(new PrayerNotificationConfig(
                KEY_IMSAK,
                "İmsak",
                true,
                10,
                MODE_NOTIFICATION,
                SOUND_DEFAULT
        ));
        items.add(new PrayerNotificationConfig(
                KEY_GUNES,
                "Güneş",
                false,
                10,
                MODE_NOTIFICATION,
                SOUND_DEFAULT
        ));
        items.add(new PrayerNotificationConfig(
                KEY_OGLE,
                "Öğle",
                true,
                10,
                MODE_NOTIFICATION,
                SOUND_DEFAULT
        ));
        items.add(new PrayerNotificationConfig(
                KEY_IKINDI,
                "İkindi",
                true,
                10,
                MODE_NOTIFICATION,
                SOUND_DEFAULT
        ));
        items.add(new PrayerNotificationConfig(
                KEY_AKSAM,
                "Akşam",
                true,
                10,
                MODE_ALARM,
                SOUND_EZAN_1
        ));
        items.add(new PrayerNotificationConfig(
                KEY_YATSI,
                "Yatsı",
                true,
                10,
                MODE_NOTIFICATION,
                SOUND_DEFAULT
        ));
        return items;
    }
}
