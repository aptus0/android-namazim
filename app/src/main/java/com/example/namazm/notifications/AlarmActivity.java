package com.example.namazm.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.namazm.databinding.ActivityAlarmBinding;

public class AlarmActivity extends AppCompatActivity {

    private ActivityAlarmBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAlarmBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String prayerName = getIntent().getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME);
        String prayerKey = getIntent().getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_KEY);

        if (prayerName == null) {
            prayerName = "Namaz";
        }

        binding.textAlarmTitle.setText(prayerName + " vakti girdi");

        String safePrayerName = prayerName;
        String safePrayerKey = prayerKey == null ? PrayerNameMapper.toKey(prayerName) : prayerKey;

        binding.buttonDismiss.setOnClickListener(v -> {
            sendAction(AlarmActionReceiver.ACTION_DISMISS, safePrayerKey, safePrayerName);
            finish();
        });

        binding.buttonSnooze.setOnClickListener(v -> {
            sendAction(AlarmActionReceiver.ACTION_SNOOZE, safePrayerKey, safePrayerName);
            finish();
        });
    }

    private void sendAction(String action, String prayerKey, String prayerName) {
        Intent intent = new Intent(this, AlarmActionReceiver.class)
                .setAction(action)
                .putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_KEY, prayerKey)
                .putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName);
        sendBroadcast(intent);
    }
}
