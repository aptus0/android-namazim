package com.example.namazm.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.namazm.data.model.City;
import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.notifications.NotificationOrchestrator;
import com.example.namazm.startup.PrayerTimesInitializer;
import com.example.namazm.startup.StartupPreferences;

public class NotificationRescheduleWorker extends Worker {

    public NotificationRescheduleWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        ServiceLocator.init(context);
        NamazRepository repository = ServiceLocator.provideRepository();

        StartupPreferences preferences = new StartupPreferences(context);
        String savedLocation = preferences.getSavedLocationLabel();
        City city = repository.findCityByName(savedLocation);
        if (city == null) {
            city = repository.findCityByName(repository.getSelectedCityName());
        }
        if (city == null && !repository.getCities().isEmpty()) {
            city = repository.getCities().get(0);
        }

        if (city != null) {
            PrayerTimesInitializer initializer = new PrayerTimesInitializer(context, repository);
            initializer.ensureTodayPrayerTimes(city, false);
        }

        NotificationOrchestrator.applySettingsAndSchedule(
                context,
                repository
        );
        return Result.success();
    }
}
