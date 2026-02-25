package com.example.namazm.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import com.example.namazm.R;

public final class NotificationChannels {

    public static final String CHANNEL_PRAYER_ALERT = "channel_prayer_alert";
    public static final String CHANNEL_HADITH_DAILY = "channel_hadith_daily";
    public static final String CHANNEL_HADITH_NEAR_PRAYER = "channel_hadith_near_prayer";

    private NotificationChannels() {
    }

    public static void createAll(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel prayerChannel = new NotificationChannel(
                CHANNEL_PRAYER_ALERT,
                context.getString(R.string.channel_prayer_alert_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        prayerChannel.setDescription(context.getString(R.string.channel_prayer_alert_desc));
        prayerChannel.enableVibration(true);
        prayerChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

        AudioAttributes alarmAudio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        Uri defaultSound = android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;
        prayerChannel.setSound(defaultSound, alarmAudio);

        NotificationChannel hadithDaily = new NotificationChannel(
                CHANNEL_HADITH_DAILY,
                context.getString(R.string.channel_hadith_daily_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        hadithDaily.setDescription(context.getString(R.string.channel_hadith_daily_desc));

        NotificationChannel hadithNearPrayer = new NotificationChannel(
                CHANNEL_HADITH_NEAR_PRAYER,
                context.getString(R.string.channel_hadith_near_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        hadithNearPrayer.setDescription(context.getString(R.string.channel_hadith_near_desc));

        manager.createNotificationChannel(prayerChannel);
        manager.createNotificationChannel(hadithDaily);
        manager.createNotificationChannel(hadithNearPrayer);
    }
}
