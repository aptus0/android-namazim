package com.example.namazm.startup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.namazm.MainActivity;
import com.example.namazm.R;
import com.example.namazm.databinding.ActivityOnboardingPermissionsBinding;
import com.example.namazm.notifications.NotificationPermissionHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class OnboardingPermissionsActivity extends AppCompatActivity {

    private ActivityOnboardingPermissionsBinding binding;
    private StartupPreferences preferences;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (hasLocationPermission()) {
                    completeAndOpenSplash();
                    return;
                }

                Snackbar.make(
                        binding.getRoot(),
                        R.string.onboarding_location_required,
                        Snackbar.LENGTH_LONG
                ).setAction(R.string.screen_city_select, v -> openManualCitySelection())
                        .show();
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingPermissionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferences = new StartupPreferences(getApplicationContext());

        binding.buttonGrantPermissions.setOnClickListener(v -> requestPermissions());
        binding.buttonManualCity.setOnClickListener(v -> openManualCitySelection());
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();

        if (!hasLocationPermission()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (permissions.isEmpty()) {
            completeAndOpenSplash();
            return;
        }

        permissionLauncher.launch(permissions.toArray(new String[0]));
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void completeAndOpenSplash() {
        preferences.setOnboardingCompleted(true);

        if (!NotificationPermissionHelper.canScheduleExactAlarms(this)) {
            NotificationPermissionHelper.openSafe(
                    this,
                    NotificationPermissionHelper.buildExactAlarmSettingsIntent(this)
            );
        }

        Intent intent = new Intent(this, SplashActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openManualCitySelection() {
        preferences.setOnboardingCompleted(true);

        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_CITY_SELECT, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
