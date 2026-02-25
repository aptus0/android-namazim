package com.example.namazm.startup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.namazm.MainActivity;
import com.example.namazm.R;
import com.example.namazm.data.model.City;
import com.example.namazm.data.model.LocationSuggestion;
import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.databinding.ActivitySplashBinding;
import com.example.namazm.notifications.NotificationOrchestrator;
import com.example.namazm.notifications.NotificationPermissionHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_DURATION_MS = 1200L;
    private static final float LOCATION_CHANGE_THRESHOLD_KM = 50f;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> beginAutoSetup()
    );

    private ActivitySplashBinding binding;
    private FusedLocationProviderClient locationClient;
    private NamazRepository repository;
    private StartupPreferences startupPreferences;
    private PrayerTimesInitializer prayerTimesInitializer;
    private ExecutorService ioExecutor;
    private long startedAtMillis;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        startedAtMillis = System.currentTimeMillis();
        ioExecutor = Executors.newSingleThreadExecutor();

        ServiceLocator.init(getApplicationContext());
        repository = ServiceLocator.provideRepository();
        startupPreferences = new StartupPreferences(getApplicationContext());

        if (!startupPreferences.isOnboardingCompleted()) {
            startActivity(new Intent(this, OnboardingWelcomeActivity.class));
            finish();
            return;
        }

        prayerTimesInitializer = new PrayerTimesInitializer(getApplicationContext(), repository);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        startPermissionFlow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
    }

    private void startPermissionFlow() {
        binding.textStatus.setText(R.string.splash_status_permission_check);

        List<String> missing = new ArrayList<>();
        if (!hasLocationPermission()) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
            missing.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (missing.isEmpty()) {
            beginAutoSetup();
            return;
        }

        permissionLauncher.launch(missing.toArray(new String[0]));
    }

    private void beginAutoSetup() {
        binding.textStatus.setText(R.string.splash_status_location);

        if (!hasLocationPermission()) {
            initializeWithCity(null, null);
            return;
        }

        CancellationTokenSource tokenSource = new CancellationTokenSource();
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        initializeWithCity(location, null);
                        return;
                    }
                    requestLastKnownLocation();
                })
                .addOnFailureListener(error -> requestLastKnownLocation());
    }

    private void requestLastKnownLocation() {
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> initializeWithCity(location, null))
                .addOnFailureListener(error -> initializeWithCity(null, error));
    }

    private void initializeWithCity(@Nullable Location location, @Nullable Throwable locationError) {
        ioExecutor.execute(() -> {
            if (locationError != null) {
                setStatusOnMainThread(getString(R.string.splash_status_location_failed));
            }

            LocationSuggestion locationSuggestion = resolveLocationSuggestion(location);
            City city = locationSuggestion == null
                    ? null
                    : repository.findCityByName(locationSuggestion.getCityName());
            if (city == null) {
                List<City> cities = repository.getCities();
                city = cities.isEmpty() ? null : cities.get(0);
                if (city != null) {
                    locationSuggestion = LocationSuggestion.fromCity(city);
                }
            }

            if (city == null) {
                launchMain();
                return;
            }

            if (locationSuggestion != null) {
                repository.selectLocation(locationSuggestion);
            } else {
                repository.selectCity(city.getName());
            }

            String todayIso = LocalDate.now().toString();
            boolean shouldUseCacheOnly = shouldUseCacheOnly(location, todayIso);

            String targetLabel = locationSuggestion == null
                    ? city.getName()
                    : locationSuggestion.getSelectionLabel();
            setStatusOnMainThread(getString(R.string.splash_status_fetching, targetLabel));
            prayerTimesInitializer.ensureTodayPrayerTimes(city, shouldUseCacheOnly);

            NotificationOrchestrator.applySettingsAndSchedule(getApplicationContext(), repository);

            if (location != null) {
                startupPreferences.saveCityAndLocation(
                        city.getName(),
                        targetLabel,
                        location.getLatitude(),
                        location.getLongitude()
                );
            } else {
                startupPreferences.saveCityOnly(city.getName(), targetLabel);
            }
            startupPreferences.setLastSyncDate(todayIso);

            if (!NotificationPermissionHelper.canScheduleExactAlarms(getApplicationContext())) {
                setStatusOnMainThread(getString(R.string.splash_status_exact_alarm_warning));
            }

            launchMain();
        });
    }

    private boolean shouldUseCacheOnly(@Nullable Location location, @NonNull String todayIso) {
        String lastSyncDate = startupPreferences.getLastSyncDate();
        if (lastSyncDate == null || !lastSyncDate.equals(todayIso)) {
            return false;
        }

        if (location == null || !startupPreferences.hasLastLocation()) {
            return true;
        }

        float[] results = new float[1];
        Location.distanceBetween(
                startupPreferences.getLastLatitude(),
                startupPreferences.getLastLongitude(),
                location.getLatitude(),
                location.getLongitude(),
                results
        );
        float kilometers = results[0] / 1000f;
        return kilometers < LOCATION_CHANGE_THRESHOLD_KM;
    }

    @Nullable
    private LocationSuggestion resolveLocationSuggestion(@Nullable Location location) {
        String savedLocation = startupPreferences.getSavedLocationLabel();

        if (location != null) {
            if (savedLocation != null && startupPreferences.hasLastLocation()) {
                float[] results = new float[1];
                Location.distanceBetween(
                        startupPreferences.getLastLatitude(),
                        startupPreferences.getLastLongitude(),
                        location.getLatitude(),
                        location.getLongitude(),
                        results
                );
                float kilometers = results[0] / 1000f;
                if (kilometers < LOCATION_CHANGE_THRESHOLD_KM) {
                    LocationSuggestion sticky = repository.findLocationByName(savedLocation);
                    if (sticky != null) {
                        return sticky;
                    }
                }
            }

            LocationSuggestion fromGeo = resolveWithGeocoder(location);
            if (fromGeo != null) {
                return fromGeo;
            }

            City nearest = repository.findNearestCity(location.getLatitude(), location.getLongitude());
            if (nearest != null) {
                return LocationSuggestion.fromCity(nearest);
            }
        }

        if (savedLocation != null && !savedLocation.trim().isEmpty()) {
            LocationSuggestion city = repository.findLocationByName(savedLocation);
            if (city != null) {
                return city;
            }
        }

        City fallback = repository.findCityByName("İstanbul");
        return fallback == null ? null : LocationSuggestion.fromCity(fallback);
    }

    @Nullable
    private LocationSuggestion resolveWithGeocoder(@NonNull Location location) {
        if (!Geocoder.isPresent()) {
            return null;
        }

        Geocoder geocoder = new Geocoder(this, Locale.forLanguageTag("tr-TR"));
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );
            if (addresses == null || addresses.isEmpty()) {
                return null;
            }

            Address address = addresses.get(0);
            String subAdmin = address.getSubAdminArea();
            if (subAdmin != null) {
                LocationSuggestion suggestion = repository.findLocationByName(subAdmin);
                if (suggestion != null) {
                    return suggestion;
                }
            }

            String locality = address.getLocality();
            if (locality != null) {
                LocationSuggestion suggestion = repository.findLocationByName(locality);
                if (suggestion != null) {
                    return suggestion;
                }
            }

            String adminArea = address.getAdminArea();
            if (adminArea != null) {
                LocationSuggestion suggestion = repository.findLocationByName(adminArea);
                if (suggestion != null) {
                    return suggestion;
                }
            }
        } catch (IOException ignored) {
            return null;
        }

        return null;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void setStatusOnMainThread(@NonNull String text) {
        runOnUiThread(() -> {
            if (binding != null) {
                binding.textStatus.setText(text);
            }
        });
    }

    private void launchMain() {
        runOnUiThread(() -> {
            long elapsed = System.currentTimeMillis() - startedAtMillis;
            long delay = Math.max(0L, MIN_SPLASH_DURATION_MS - elapsed);
            binding.getRoot().postDelayed(() -> {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }, delay);
        });
    }
}
