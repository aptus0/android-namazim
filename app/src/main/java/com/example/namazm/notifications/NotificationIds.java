package com.example.namazm.notifications;

public final class NotificationIds {

    private NotificationIds() {
    }

    public static int prayerReminder(String prayerKey) {
        return stableId("prayer_reminder_" + prayerKey);
    }

    public static int prayerEntry(String prayerKey) {
        return stableId("prayer_entry_" + prayerKey);
    }

    public static int hadithNearPrayer(String prayerKey) {
        return stableId("hadith_near_" + prayerKey);
    }

    public static int hadithDaily() {
        return stableId("hadith_daily");
    }

    private static int stableId(String input) {
        return Math.abs(input.hashCode());
    }
}
