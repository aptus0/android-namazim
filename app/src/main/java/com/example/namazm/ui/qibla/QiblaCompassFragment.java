package com.example.namazm.ui.qibla;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.namazm.R;
import com.example.namazm.databinding.FragmentQiblaCompassBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class QiblaCompassFragment extends Fragment implements SensorEventListener {

    private static final float ALIGNMENT_THRESHOLD_DEGREES = 3f;
    private static final long VIBRATION_COOLDOWN_MS = 2500L;

    private FragmentQiblaCompassBinding binding;
    private FusedLocationProviderClient locationClient;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private LocationCallback locationCallback;

    private Location currentLocation;
    private float currentAzimuth = Float.NaN;
    private float currentQiblaBearing = Float.NaN;
    private long lastVibrationAt = 0L;

    private ActivityResultLauncher<String> locationPermissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        if (binding != null) {
                            Snackbar.make(binding.getRoot(), R.string.qibla_permission_needed, Snackbar.LENGTH_SHORT).show();
                            binding.textQiblaStatus.setText(R.string.qibla_permission_needed);
                        }
                        return;
                    }
                    requestHighAccuracyLocation();
                    startLocationUpdates();
                }
        );
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentQiblaCompassBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        binding.textQiblaBearing.setText(getString(R.string.qibla_bearing_format, 0));
        binding.textQiblaDistance.setText(getString(R.string.qibla_distance_format, 0f));
        binding.textQiblaHeading.setText(getString(R.string.qibla_heading_format, 0));
        binding.textQiblaStatus.setText(R.string.qibla_location_loading);

        binding.buttonRefreshLocation.setOnClickListener(v -> requestLocationPermissionOrStart());
        requestLocationPermissionOrStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerSensorListener();
        if (hasLocationPermission()) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterSensorListener();
        stopLocationUpdates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) {
            return;
        }

        float[] rotationMatrix = new float[9];
        float[] orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        SensorManager.getOrientation(rotationMatrix, orientation);

        float azimuth = (float) Math.toDegrees(orientation[0]);
        currentAzimuth = QiblaMath.normalizeDegrees(azimuth);
        updateCompassUi();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op.
    }

    private void registerSensorListener() {
        if (sensorManager == null || rotationSensor == null) {
            return;
        }
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterSensorListener() {
        if (sensorManager == null) {
            return;
        }
        sensorManager.unregisterListener(this);
    }

    private void requestLocationPermissionOrStart() {
        if (hasLocationPermission()) {
            requestHighAccuracyLocation();
            startLocationUpdates();
            return;
        }
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestHighAccuracyLocation() {
        if (!hasLocationPermission()) {
            return;
        }

        binding.textQiblaStatus.setText(R.string.qibla_location_loading);
        CancellationTokenSource tokenSource = new CancellationTokenSource();
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        onLocationReceived(location);
                        return;
                    }
                    requestLastKnownLocation();
                })
                .addOnFailureListener(error -> requestLastKnownLocation());
    }

    private void requestLastKnownLocation() {
        if (!hasLocationPermission()) {
            return;
        }
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        if (binding != null) {
                            binding.textQiblaStatus.setText(R.string.qibla_location_unavailable);
                        }
                        return;
                    }
                    onLocationReceived(location);
                })
                .addOnFailureListener(error -> {
                    if (binding != null) {
                        binding.textQiblaStatus.setText(R.string.qibla_location_unavailable);
                    }
                });
    }

    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            return;
        }
        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        onLocationReceived(location);
                    }
                }
            };
        }

        try {
            LocationRequest request = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    3000L
            )
                    .setMinUpdateIntervalMillis(1500L)
                    .setWaitForAccurateLocation(true)
                    .build();
            locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } catch (SecurityException ignored) {
            // Permission check is already done.
        }
    }

    private void stopLocationUpdates() {
        if (locationCallback == null) {
            return;
        }
        locationClient.removeLocationUpdates(locationCallback);
    }

    private void onLocationReceived(Location location) {
        currentLocation = location;
        currentQiblaBearing = QiblaMath.computeQiblaBearing(location);
        updateCompassUi();
    }

    private void updateCompassUi() {
        if (binding == null) {
            return;
        }

        if (currentLocation == null) {
            binding.textQiblaStatus.setText(R.string.qibla_location_loading);
            return;
        }

        int qiblaDegrees = Math.round(currentQiblaBearing);
        float distanceKm = QiblaMath.computeDistanceKm(currentLocation);
        binding.textQiblaBearing.setText(getString(R.string.qibla_bearing_format, qiblaDegrees));
        binding.textQiblaDistance.setText(String.format(Locale.US, getString(R.string.qibla_distance_format), distanceKm));

        if (Float.isNaN(currentAzimuth)) {
            return;
        }

        int headingDegrees = Math.round(currentAzimuth);
        binding.textQiblaHeading.setText(getString(R.string.qibla_heading_format, headingDegrees));

        float pointerRotation = QiblaMath.normalizeDegrees(currentQiblaBearing - currentAzimuth);
        binding.imageQiblaPointer.setRotation(pointerRotation);

        float difference = QiblaMath.shortestAngleDifference(currentAzimuth, currentQiblaBearing);
        boolean isAligned = difference <= ALIGNMENT_THRESHOLD_DEGREES;
        binding.textQiblaStatus.setText(isAligned ? R.string.qibla_alignment_aligned : R.string.qibla_alignment_not_aligned);
        int statusColor = ContextCompat.getColor(
                requireContext(),
                isAligned ? R.color.qibla_aligned : R.color.text_secondary
        );
        binding.textQiblaStatus.setTextColor(statusColor);

        if (isAligned) {
            vibrateWithCooldown();
        }
    }

    private void vibrateWithCooldown() {
        long now = System.currentTimeMillis();
        if (now - lastVibrationAt < VIBRATION_COOLDOWN_MS) {
            return;
        }
        lastVibrationAt = now;

        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager == null ? null : manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(40L);
        }

        Snackbar.make(binding.getRoot(), R.string.qibla_vibration_feedback, Snackbar.LENGTH_SHORT).show();
    }
}
