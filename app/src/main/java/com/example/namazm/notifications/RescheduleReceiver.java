package com.example.namazm.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.namazm.data.repository.ServiceLocator;

public class RescheduleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) {
            return;
        }

        ServiceLocator.init(context.getApplicationContext());
        NotificationOrchestrator.applySettingsAndSchedule(
                context.getApplicationContext(),
                ServiceLocator.provideRepository()
        );
    }
}
