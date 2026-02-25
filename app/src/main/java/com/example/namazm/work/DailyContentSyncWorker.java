package com.example.namazm.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.namazm.data.repository.ServiceLocator;

public class DailyContentSyncWorker extends Worker {

    public DailyContentSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            ServiceLocator.provideRepository().getDailyHadith(0);
            return Result.success();
        } catch (Exception ignored) {
            return Result.retry();
        }
    }
}
