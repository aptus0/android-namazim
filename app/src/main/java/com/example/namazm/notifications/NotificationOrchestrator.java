package com.example.namazm.notifications;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.namazm.data.model.NotificationSettingsState;
import com.example.namazm.data.model.PrayerNotificationConfig;
import com.example.namazm.data.model.PrayerSlot;
import com.example.namazm.data.model.PrayerTimesOverview;
import com.example.namazm.data.repository.NamazRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class NotificationOrchestrator {

    private NotificationOrchestrator() {
    }

    public static void applySettingsAndSchedule(@NonNull Context context, @NonNull NamazRepository repository) {
        Context appContext = context.getApplicationContext();
        NotificationChannels.createAll(appContext);

        NotificationSettingsState settings = repository.getNotificationSettings();
        PrayerAlarmScheduler scheduler = new PrayerAlarmScheduler(appContext);
        scheduler.cancelAll();

        if (settings.isHadithDailyEnabled()) {
            scheduler.scheduleDailyHadith(settings.getHadithDailyTime());
        }

        if (!settings.isPrayerNotificationsEnabled()) {
            return;
        }

        PrayerTimesOverview overview = repository.getPrayerTimesOverview();
        Map<String, PrayerSlot> slots = mapSlots(overview.getTodaySlots());

        for (PrayerNotificationConfig config : settings.getPrayerConfigs()) {
            if (!config.isEnabled()) {
                continue;
            }

            PrayerSlot slot = slots.get(config.getPrayerKey());
            if (slot == null) {
                continue;
            }

            long prayerTime = scheduler.nextOccurrenceMillis(slot.getTime());
            scheduler.schedulePrayerEntry(config, slot.getName(), prayerTime);

            int offset = Math.max(config.getOffsetMinutes(), 0);
            if (offset <= 0) {
                continue;
            }

            long reminderTime = prayerTime - TimeUnit.MINUTES.toMillis(offset);
            if (reminderTime <= System.currentTimeMillis()) {
                continue;
            }

            scheduler.schedulePrayerReminder(
                    config,
                    slot.getName(),
                    reminderTime,
                    settings.isHadithNearPrayerEnabled()
            );
        }
    }

    private static Map<String, PrayerSlot> mapSlots(List<PrayerSlot> items) {
        Map<String, PrayerSlot> map = new HashMap<>();
        for (PrayerSlot slot : items) {
            map.put(PrayerNameMapper.toKey(slot.getName()), slot);
        }
        return map;
    }
}
