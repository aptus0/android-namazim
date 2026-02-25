package com.example.namazm.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.example.namazm.data.model.PrayerNotificationConfig;

public class PrayerAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        String prayerName = getValue(intent, PrayerAlarmScheduler.EXTRA_PRAYER_NAME, "Namaz");
        String prayerKey = getValue(intent, PrayerAlarmScheduler.EXTRA_PRAYER_KEY, PrayerNameMapper.toKey(prayerName));
        int offset = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_OFFSET, 10);
        String mode = getValue(intent, PrayerAlarmScheduler.EXTRA_MODE, PrayerNotificationConfig.MODE_NOTIFICATION);

        NotificationDispatcher dispatcher = new NotificationDispatcher(context.getApplicationContext());
        switch (action) {
            case PrayerAlarmScheduler.ACTION_PRAYER_REMINDER:
                dispatcher.showPrayerReminder(prayerName, offset, prayerKey);
                break;
            case PrayerAlarmScheduler.ACTION_PRAYER_ENTRY:
                dispatcher.showPrayerEntry(
                        prayerName,
                        prayerKey,
                        PrayerNotificationConfig.MODE_ALARM.equals(mode)
                );
                break;
            case PrayerAlarmScheduler.ACTION_HADITH_NEAR_PRAYER:
                dispatcher.showHadithNearPrayer(prayerName, prayerKey);
                break;
            case PrayerAlarmScheduler.ACTION_HADITH_DAILY:
                dispatcher.showDailyHadith();
                break;
            default:
                break;
        }
    }

    @NonNull
    private String getValue(Intent intent, String key, String fallback) {
        String value = intent.getStringExtra(key);
        return value == null ? fallback : value;
    }
}
