package com.example.namazm.notifications;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.example.namazm.data.model.PrayerNotificationConfig;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PrayerAlarmScheduler {

    public static final String ACTION_PRAYER_REMINDER = "com.example.namazm.notifications.PRAYER_REMINDER";
    public static final String ACTION_PRAYER_ENTRY = "com.example.namazm.notifications.PRAYER_ENTRY";
    public static final String ACTION_HADITH_NEAR_PRAYER = "com.example.namazm.notifications.HADITH_NEAR";
    public static final String ACTION_HADITH_DAILY = "com.example.namazm.notifications.HADITH_DAILY";

    public static final String EXTRA_PRAYER_NAME = "extra_prayer_name";
    public static final String EXTRA_PRAYER_KEY = "extra_prayer_key";
    public static final String EXTRA_OFFSET = "extra_offset";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_SOUND = "extra_sound";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final Context appContext;
    private final AlarmManager alarmManager;

    public PrayerAlarmScheduler(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
    }

    public void cancelAll() {
        if (alarmManager == null) {
            return;
        }

        List<String> keys = Arrays.asList(
                PrayerNotificationConfig.KEY_IMSAK,
                PrayerNotificationConfig.KEY_GUNES,
                PrayerNotificationConfig.KEY_OGLE,
                PrayerNotificationConfig.KEY_IKINDI,
                PrayerNotificationConfig.KEY_AKSAM,
                PrayerNotificationConfig.KEY_YATSI
        );

        for (String key : keys) {
            cancelPendingIntent(buildPrayerIntent(ACTION_PRAYER_REMINDER, key, key, 0, PrayerNotificationConfig.MODE_NOTIFICATION, PrayerNotificationConfig.SOUND_DEFAULT));
            cancelPendingIntent(buildPrayerIntent(ACTION_PRAYER_ENTRY, key, key, 0, PrayerNotificationConfig.MODE_NOTIFICATION, PrayerNotificationConfig.SOUND_DEFAULT));
            cancelPendingIntent(buildPrayerIntent(ACTION_HADITH_NEAR_PRAYER, key, key, 0, PrayerNotificationConfig.MODE_NOTIFICATION, PrayerNotificationConfig.SOUND_DEFAULT));
        }
        cancelPendingIntent(buildDailyHadithIntent());
    }

    public void schedulePrayerReminder(
            @NonNull PrayerNotificationConfig config,
            @NonNull String prayerName,
            long triggerAtMillis,
            boolean withNearHadith
    ) {
        if (alarmManager == null) {
            return;
        }

        PendingIntent reminderIntent = buildPrayerIntent(
                ACTION_PRAYER_REMINDER,
                config.getPrayerKey(),
                prayerName,
                config.getOffsetMinutes(),
                config.getMode(),
                config.getSound()
        );
        scheduleExact(triggerAtMillis, reminderIntent);

        if (!withNearHadith) {
            return;
        }

        PendingIntent hadithIntent = buildPrayerIntent(
                ACTION_HADITH_NEAR_PRAYER,
                config.getPrayerKey(),
                prayerName,
                config.getOffsetMinutes(),
                config.getMode(),
                config.getSound()
        );
        long hadithTime = triggerAtMillis + TimeUnit.SECONDS.toMillis(7);
        scheduleExact(hadithTime, hadithIntent);
    }

    public void schedulePrayerEntry(
            @NonNull PrayerNotificationConfig config,
            @NonNull String prayerName,
            long triggerAtMillis
    ) {
        if (alarmManager == null) {
            return;
        }

        PendingIntent entryIntent = buildPrayerIntent(
                ACTION_PRAYER_ENTRY,
                config.getPrayerKey(),
                prayerName,
                config.getOffsetMinutes(),
                config.getMode(),
                config.getSound()
        );
        scheduleExact(triggerAtMillis, entryIntent);
    }

    @SuppressLint("ScheduleExactAlarm")
    public void scheduleSnooze(@NonNull String prayerKey, @NonNull String prayerName) {
        if (alarmManager == null) {
            return;
        }
        long triggerAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        PendingIntent entryIntent = buildPrayerIntent(
                ACTION_PRAYER_ENTRY,
                prayerKey,
                prayerName,
                0,
                PrayerNotificationConfig.MODE_ALARM,
                PrayerNotificationConfig.SOUND_DEFAULT
        );
        scheduleExact(triggerAt, entryIntent);
    }

    public void scheduleDailyHadith(@NonNull String timeLabel) {
        if (alarmManager == null) {
            return;
        }

        long firstTrigger = nextOccurrenceMillis(timeLabel);
        PendingIntent hadithIntent = buildDailyHadithIntent();

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                firstTrigger,
                AlarmManager.INTERVAL_DAY,
                hadithIntent
        );
    }

    public long nextOccurrenceMillis(@NonNull String timeLabel) {
        LocalTime localTime = LocalTime.parse(timeLabel, TIME_FORMATTER);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime target = now
                .withHour(localTime.getHour())
                .withMinute(localTime.getMinute())
                .withSecond(0)
                .withNano(0);
        if (!target.isAfter(now)) {
            target = target.plusDays(1);
        }
        return target.toInstant().toEpochMilli();
    }

    private void cancelPendingIntent(PendingIntent pendingIntent) {
        alarmManager.cancel(pendingIntent);
    }

    private void scheduleExact(long triggerAtMillis, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !NotificationPermissionHelper.canScheduleExactAlarms(appContext)) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            return;
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    private PendingIntent buildPrayerIntent(
            String action,
            String prayerKey,
            String prayerName,
            int offset,
            String mode,
            String sound
    ) {
        Intent intent = new Intent(appContext, PrayerAlarmReceiver.class)
                .setAction(action)
                .putExtra(EXTRA_PRAYER_KEY, prayerKey)
                .putExtra(EXTRA_PRAYER_NAME, prayerName)
                .putExtra(EXTRA_OFFSET, offset)
                .putExtra(EXTRA_MODE, mode)
                .putExtra(EXTRA_SOUND, sound);

        int requestCode = Math.abs((action + prayerKey).hashCode());
        return PendingIntent.getBroadcast(
                appContext,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent buildDailyHadithIntent() {
        Intent intent = new Intent(appContext, PrayerAlarmReceiver.class)
                .setAction(ACTION_HADITH_DAILY);
        int requestCode = Math.abs(ACTION_HADITH_DAILY.hashCode());
        return PendingIntent.getBroadcast(
                appContext,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
