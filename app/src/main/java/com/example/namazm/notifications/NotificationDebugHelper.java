package com.example.namazm.notifications;

import android.content.Context;

import androidx.annotation.NonNull;

public final class NotificationDebugHelper {

    private NotificationDebugHelper() {
    }

    public static void sendTestPair(@NonNull Context context) {
        NotificationChannels.createAll(context.getApplicationContext());
        NotificationDispatcher dispatcher = new NotificationDispatcher(context.getApplicationContext());
        dispatcher.testNotificationPair();
    }
}
