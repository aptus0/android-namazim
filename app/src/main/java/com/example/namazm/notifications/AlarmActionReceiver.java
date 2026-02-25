package com.example.namazm.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmActionReceiver extends BroadcastReceiver {

    public static final String ACTION_SNOOZE = "com.example.namazm.notifications.ACTION_SNOOZE";
    public static final String ACTION_DISMISS = "com.example.namazm.notifications.ACTION_DISMISS";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String prayerKey = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_KEY);
        String prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME);
        if (prayerKey == null || prayerName == null) {
            return;
        }

        NotificationDispatcher dispatcher = new NotificationDispatcher(context.getApplicationContext());
        String action = intent.getAction();
        if (ACTION_SNOOZE.equals(action)) {
            PrayerAlarmScheduler scheduler = new PrayerAlarmScheduler(context.getApplicationContext());
            scheduler.scheduleSnooze(prayerKey, prayerName);
        }
        dispatcher.dismissPrayerAlarm(prayerKey);
    }
}
