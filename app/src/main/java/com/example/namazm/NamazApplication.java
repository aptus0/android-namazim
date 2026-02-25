package com.example.namazm;

import android.app.Application;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.notifications.NotificationChannels;
import com.example.namazm.notifications.NotificationOrchestrator;
import com.example.namazm.work.DailyContentSyncWorker;
import com.example.namazm.work.NotificationRescheduleWorker;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class NamazApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ServiceLocator.init(this);
        NotificationChannels.createAll(this);
        NotificationOrchestrator.applySettingsAndSchedule(this, ServiceLocator.provideRepository());
        scheduleDailySync();
        scheduleNotificationResync();
    }

    private void scheduleDailySync() {
        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                DailyContentSyncWorker.class,
                24,
                TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_content_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }

    private void scheduleNotificationResync() {
        long initialDelayMinutes = minutesUntilNext005();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                NotificationRescheduleWorker.class,
                24,
                TimeUnit.HOURS
        )
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "notification_resync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    private long minutesUntilNext005() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.withHour(0).withMinute(5).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }

        long minutes = Duration.between(now, next).toMinutes();
        return Math.max(1L, minutes);
    }
}
