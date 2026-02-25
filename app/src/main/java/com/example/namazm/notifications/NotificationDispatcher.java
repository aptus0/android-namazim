package com.example.namazm.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.namazm.R;
import com.example.namazm.data.model.HadithOfTheDay;
import com.example.namazm.data.model.PrayerNotificationConfig;
import com.example.namazm.data.repository.ServiceLocator;

public class NotificationDispatcher {

    private final Context appContext;

    public NotificationDispatcher(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void showPrayerReminder(
            @NonNull String prayerName,
            int offsetMinutes,
            @NonNull String prayerKey
    ) {
        String title = appContext.getString(R.string.notification_prayer_reminder_title, prayerName);
        String body = appContext.getString(R.string.notification_prayer_reminder_body, offsetMinutes);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                appContext,
                NotificationChannels.CHANNEL_PRAYER_ALERT
        )
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (!NotificationPermissionHelper.hasNotificationPermission(appContext)) {
            return;
        }
        NotificationManagerCompat.from(appContext)
                .notify(NotificationIds.prayerReminder(prayerKey), builder.build());
    }

    public void showPrayerEntry(
            @NonNull String prayerName,
            @NonNull String prayerKey,
            boolean alarmMode
    ) {
        String title = appContext.getString(R.string.notification_prayer_entry_title, prayerName);
        String body = appContext.getString(R.string.notification_prayer_entry_body);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                appContext,
                NotificationChannels.CHANNEL_PRAYER_ALERT
        )
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(alarmMode)
                .setAutoCancel(!alarmMode);

        if (alarmMode) {
            builder.setFullScreenIntent(buildAlarmScreenIntent(prayerName, prayerKey), true);
            builder.addAction(
                    android.R.drawable.ic_media_pause,
                    appContext.getString(R.string.alarm_action_snooze),
                    buildActionIntent(AlarmActionReceiver.ACTION_SNOOZE, prayerKey, prayerName)
            );
            builder.addAction(
                    android.R.drawable.ic_delete,
                    appContext.getString(R.string.alarm_action_dismiss),
                    buildActionIntent(AlarmActionReceiver.ACTION_DISMISS, prayerKey, prayerName)
            );
        }

        if (!NotificationPermissionHelper.hasNotificationPermission(appContext)) {
            return;
        }
        NotificationManagerCompat.from(appContext)
                .notify(NotificationIds.prayerEntry(prayerKey), builder.build());
    }

    public void showHadithNearPrayer(@NonNull String prayerName, @NonNull String prayerKey) {
        HadithOfTheDay hadith = ServiceLocator.provideRepository().getDailyHadith(0);
        String title = appContext.getString(R.string.notification_hadith_near_title, prayerName);
        String body = miniHadithText(hadith);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                appContext,
                NotificationChannels.CHANNEL_HADITH_NEAR_PRAYER
        )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        if (!NotificationPermissionHelper.hasNotificationPermission(appContext)) {
            return;
        }
        NotificationManagerCompat.from(appContext)
                .notify(NotificationIds.hadithNearPrayer(prayerKey), builder.build());
    }

    public void showDailyHadith() {
        HadithOfTheDay hadith = ServiceLocator.provideRepository().getDailyHadith(0);
        String expanded = hadith.getText() + "\n(" + hadith.getSource() + ")";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                appContext,
                NotificationChannels.CHANNEL_HADITH_DAILY
        )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(appContext.getString(R.string.notification_hadith_daily_title))
                .setContentText(hadith.getText())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(expanded))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION);

        if (!NotificationPermissionHelper.hasNotificationPermission(appContext)) {
            return;
        }
        NotificationManagerCompat.from(appContext)
                .notify(NotificationIds.hadithDaily(), builder.build());
    }

    public void dismissPrayerAlarm(@NonNull String prayerKey) {
        NotificationManagerCompat.from(appContext).cancel(NotificationIds.prayerEntry(prayerKey));
    }

    public void testNotificationPair() {
        showPrayerReminder("Akşam", 10, PrayerNotificationConfig.KEY_AKSAM);
        showHadithNearPrayer("Akşam", PrayerNotificationConfig.KEY_AKSAM);
    }

    private PendingIntent buildAlarmScreenIntent(String prayerName, String prayerKey) {
        Intent fullScreenIntent = new Intent(appContext, AlarmActivity.class)
                .putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
                .putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_KEY, prayerKey)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = Math.abs(("alarm_screen_" + prayerKey).hashCode());
        return PendingIntent.getActivity(
                appContext,
                requestCode,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent buildActionIntent(String action, String prayerKey, String prayerName) {
        Intent intent = new Intent(appContext, AlarmActionReceiver.class)
                .setAction(action)
                .putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_KEY, prayerKey)
                .putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName);

        int requestCode = Math.abs((action + prayerKey).hashCode());
        return PendingIntent.getBroadcast(
                appContext,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private String miniHadithText(HadithOfTheDay hadith) {
        if (hadith == null) {
            return "";
        }
        String shortText = hadith.getShortText();
        if (shortText != null && !shortText.trim().isEmpty()) {
            return shortText.trim();
        }
        String text = hadith.getText() == null ? "" : hadith.getText().trim();
        if (text.length() <= 90) {
            return text;
        }
        return text.substring(0, 90).trim() + "...";
    }
}
