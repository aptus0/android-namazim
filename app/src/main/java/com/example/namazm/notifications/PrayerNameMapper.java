package com.example.namazm.notifications;

import com.example.namazm.data.model.PrayerNotificationConfig;

import java.util.Locale;

public final class PrayerNameMapper {

    private PrayerNameMapper() {
    }

    public static String toKey(String prayerName) {
        if (prayerName == null) {
            return "unknown";
        }

        String value = prayerName.toLowerCase(Locale.forLanguageTag("tr-TR"));
        if (value.contains("imsak")) {
            return PrayerNotificationConfig.KEY_IMSAK;
        }
        if (value.contains("güneş") || value.contains("gunes")) {
            return PrayerNotificationConfig.KEY_GUNES;
        }
        if (value.contains("öğle") || value.contains("ogle")) {
            return PrayerNotificationConfig.KEY_OGLE;
        }
        if (value.contains("ikindi")) {
            return PrayerNotificationConfig.KEY_IKINDI;
        }
        if (value.contains("akşam") || value.contains("aksam")) {
            return PrayerNotificationConfig.KEY_AKSAM;
        }
        if (value.contains("yatsı") || value.contains("yatsi")) {
            return PrayerNotificationConfig.KEY_YATSI;
        }
        return value;
    }
}
