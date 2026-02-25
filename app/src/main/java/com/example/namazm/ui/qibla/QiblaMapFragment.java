package com.example.namazm.ui.qibla;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.namazm.R;
import com.example.namazm.databinding.FragmentQiblaMapBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.Locale;

public class QiblaMapFragment extends Fragment implements OnMapReadyCallback, SensorEventListener {

    private static final float ALIGNMENT_THRESHOLD_DEGREES = 3f;
    private static final long VIBRATION_COOLDOWN_MS = 2500L;

    private FragmentQiblaMapBinding binding;
    private FusedLocationProviderClient locationClient;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private LocationCallback locationCallback;

    private GoogleMap googleMap;
    private Marker qiblaMarker;
    private Polyline qiblaLine;
    private boolean cameraInitialized;

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
                            binding.textMapStatus.setText(R.string.qibla_permission_needed);
                            Snackbar.make(binding.getRoot(), R.string.qibla_permission_needed, Snackbar.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    requestHighAccuracyLocation();
                    startLocationUpdates();
                    enableMyLocationIfPermitted();
                }
        );
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentQiblaMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        sensorManager = (SensorManager) requireContext().getSystemService(android.content.Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        binding.textMapBearing.setText(getString(R.string.qibla_bearing_format, 0));
        binding.textMapDistance.setText(getString(R.string.qibla_distance_format, 0f));
        binding.textMapStatus.setText(R.string.qibla_location_loading);

        binding.buttonRecenter.setOnClickListener(v -> recenterMap());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

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
        googleMap = null;
        qiblaMarker = null;
        qiblaLine = null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        enableMyLocationIfPermitted();
        updateMapAndStatus();
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
        currentAzimuth = QiblaMath.normalizeDegrees((float) Math.toDegrees(orientation[0]));
        updateMapAndStatus();
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
        binding.textMapStatus.setText(R.string.qibla_location_loading);
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
                            binding.textMapStatus.setText(R.string.qibla_location_unavailable);
                        }
                        return;
                    }
                    onLocationReceived(location);
                })
                .addOnFailureListener(error -> {
                    if (binding != null) {
                        binding.textMapStatus.setText(R.string.qibla_location_unavailable);
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
        updateMapAndStatus();
    }

    private void updateMapAndStatus() {
        if (binding == null) {
            return;
        }

        if (currentLocation == null) {
            binding.textMapStatus.setText(R.string.qibla_location_loading);
            return;
        }

        int qiblaDegrees = Math.round(currentQiblaBearing);
        float distanceKm = QiblaMath.computeDistanceKm(currentLocation);
        binding.textMapBearing.setText(getString(R.string.qibla_bearing_format, qiblaDegrees));
        binding.textMapDistance.setText(String.format(Locale.US, getString(R.string.qibla_distance_format), distanceKm));

        boolean aligned = false;
        if (!Float.isNaN(currentAzimuth)) {
            float difference = QiblaMath.shortestAngleDifference(currentAzimuth, currentQiblaBearing);
            aligned = difference <= ALIGNMENT_THRESHOLD_DEGREES;
        }

        binding.textMapStatus.setText(aligned ? R.string.qibla_alignment_aligned : R.string.qibla_alignment_not_aligned);
        int statusColor = ContextCompat.getColor(
                requireContext(),
                aligned ? R.color.qibla_aligned : R.color.text_secondary
        );
        binding.textMapStatus.setTextColor(statusColor);

        if (aligned) {
            vibrateWithCooldown();
        }

        if (googleMap == null) {
            return;
        }

        LatLng me = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        LatLng kaaba = new LatLng(QiblaMath.KAABA_LATITUDE, QiblaMath.KAABA_LONGITUDE);
        float markerRotation = Float.isNaN(currentAzimuth)
                ? 0f
                : QiblaMath.normalizeDegrees(currentQiblaBearing - currentAzimuth);

        if (qiblaMarker == null) {
            qiblaMarker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(me)
                            .anchor(0.5f, 0.85f)
                            .flat(true)
                            .rotation(markerRotation)
                            .icon(buildMarkerIcon())
                            .title(getString(R.string.qibla_title))
            );
        } else {
            qiblaMarker.setPosition(me);
            qiblaMarker.setRotation(markerRotation);
        }

        if (qiblaLine == null) {
            qiblaLine = googleMap.addPolyline(
                    new PolylineOptions()
                            .add(me, kaaba)
                            .width(7f)
                            .color(ContextCompat.getColor(requireContext(), R.color.qibla_aligned))
                            .geodesic(true)
            );
        } else {
            qiblaLine.setPoints(Arrays.asList(me, kaaba));
        }

        if (!cameraInitialized) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 15f));
            cameraInitialized = true;
        }
    }

    private void recenterMap() {
        if (googleMap == null || currentLocation == null) {
            return;
        }
        LatLng me = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 16f));
    }

    private void enableMyLocationIfPermitted() {
        if (googleMap == null || !hasLocationPermission()) {
            return;
        }
        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException ignored) {
            // Permission check is already done.
        }
    }

    private BitmapDescriptor buildMarkerIcon() {
        Drawable drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_qibla_pointer);
        if (drawable == null) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        }
        int sizePx = (int) (44 * requireContext().getResources().getDisplayMetrics().density);
        drawable.setBounds(0, 0, sizePx, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void vibrateWithCooldown() {
        long now = System.currentTimeMillis();
        if (now - lastVibrationAt < VIBRATION_COOLDOWN_MS) {
            return;
        }
        lastVibrationAt = now;

        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) requireContext().getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager == null ? null : manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(40L);
        }
    }
}
