package com.example.namazm.ui.settings;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.namazm.R;
import com.example.namazm.data.model.NotificationSettingsState;
import com.example.namazm.data.model.PrayerNotificationConfig;
import com.example.namazm.data.model.SettingsState;
import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.databinding.FragmentSettingsBinding;
import com.example.namazm.notifications.NotificationDebugHelper;
import com.example.namazm.notifications.NotificationOrchestrator;
import com.example.namazm.notifications.NotificationPermissionHelper;
import com.example.namazm.ui.common.NamazViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SettingsFragment extends Fragment {

    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$");
    private static final String PREFS_NAME = "settings_ui_prefs";
    private static final String PREF_VIBRATION_ENABLED = "pref_vibration_enabled";
    private static final String PREF_SNOOZE_MINUTES = "pref_snooze_minutes";

    private FragmentSettingsBinding binding;
    private NamazRepository repository;
    private SettingsState currentSettingsState;
    private NotificationSettingsState currentNotificationState;
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    updateSystemStatus();
                    if (binding != null) {
                        int message = granted ? R.string.permission_granted : R.string.permission_denied;
                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = ServiceLocator.provideRepository();
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, 0);

        setupSoundDropdown();
        setupClickListeners();
        loadLocalAlarmPreferences();

        NamazViewModelFactory factory = new NamazViewModelFactory(repository);
        SettingsViewModel viewModel = new ViewModelProvider(this, factory).get(SettingsViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::bindSettingsState);

        refreshNotificationState();
        updateLocationPermissionChip();
        updateSystemStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLocationPermissionChip();
        updateSystemStatus();
    }

    private void setupSoundDropdown() {
        ArrayAdapter<CharSequence> soundAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.alarm_sounds,
                android.R.layout.simple_list_item_1
        );
        binding.inputAlarmSound.setAdapter(soundAdapter);
    }

    private void setupClickListeners() {
        binding.buttonAboutShortcut.setOnClickListener(this::navigateAbout);
        binding.buttonAbout.setOnClickListener(this::navigateAbout);

        binding.buttonManualCity.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.citySelectFragment);
        });

        binding.buttonRefreshLocation.setOnClickListener(v -> {
            if (!hasLocationPermission()) {
                NotificationPermissionHelper.openSafe(
                        requireActivity(),
                        NotificationPermissionHelper.buildAppDetailsIntent(requireContext())
                );
                Snackbar.make(v, R.string.settings_location_permission_missing, Snackbar.LENGTH_SHORT).show();
                return;
            }
            Snackbar.make(v, R.string.settings_location_refreshing, Snackbar.LENGTH_SHORT).show();
        });

        binding.buttonOpenNotificationPermission.setOnClickListener(v -> openNotificationPermissionFlow());
        binding.buttonOpenExactAlarm.setOnClickListener(v -> NotificationPermissionHelper.openSafe(
                requireActivity(),
                NotificationPermissionHelper.buildExactAlarmSettingsIntent(requireContext())
        ));
        binding.buttonOpenBatteryOptimization.setOnClickListener(v -> NotificationPermissionHelper.openSafe(
                requireActivity(),
                NotificationPermissionHelper.buildBatteryOptimizationIntent(requireContext())
        ));

        binding.buttonPreviewSound.setOnClickListener(v -> {
            NotificationDebugHelper.sendTestPair(requireContext());
            Snackbar.make(v, R.string.settings_preview_sent, Snackbar.LENGTH_SHORT).show();
        });

        binding.buttonSaveSettings.setOnClickListener(this::saveAllSettings);
    }

    private void bindSettingsState(SettingsState state) {
        currentSettingsState = state;
        binding.textLocationValue.setText(getString(R.string.calendar_city_auto_format, state.getSelectedCityName()));
        binding.textVersionValue.setText(getString(R.string.about_app_version));
        binding.textDataSourceValue.setText(getString(
                R.string.settings_data_source_value,
                dataSourceToLabel(state.getDataSource())
        ));

        checkThemeToggle(state.getThemeMode());
    }

    private void refreshNotificationState() {
        currentNotificationState = repository.getNotificationSettings();
        if (currentNotificationState == null) {
            currentNotificationState = NotificationSettingsState.defaultState();
        }
        bindNotificationState(currentNotificationState);
    }

    private void bindNotificationState(NotificationSettingsState state) {
        binding.switchPrayerReminder.setChecked(state.isPrayerNotificationsEnabled());
        binding.switchDailyHadith.setChecked(state.isHadithDailyEnabled());
        binding.switchNearPrayerHadith.setChecked(state.isHadithNearPrayerEnabled());
        binding.inputHadithTime.setText(state.getHadithDailyTime());

        PrayerNotificationConfig referenceConfig = firstConfigOrDefault(state.getPrayerConfigs());
        checkModeToggle(referenceConfig.getMode());
        binding.inputAlarmSound.setText(soundToLabel(referenceConfig.getSound()), false);
    }

    private void saveAllSettings(View anchor) {
        if (currentSettingsState == null) {
            currentSettingsState = repository.getSettingsState();
        }
        if (currentNotificationState == null) {
            currentNotificationState = repository.getNotificationSettings();
        }
        if (currentNotificationState == null) {
            currentNotificationState = NotificationSettingsState.defaultState();
        }

        String hadithTime = binding.inputHadithTime.getText() == null
                ? ""
                : binding.inputHadithTime.getText().toString().trim();
        if (TextUtils.isEmpty(hadithTime) || !TIME_PATTERN.matcher(hadithTime).matches()) {
            hadithTime = "09:00";
        }

        String selectedMode = selectedMode();
        String selectedSound = labelToSound(binding.inputAlarmSound.getText() == null
                ? ""
                : binding.inputAlarmSound.getText().toString());

        List<PrayerNotificationConfig> updatedConfigs = new ArrayList<>();
        for (PrayerNotificationConfig item : currentNotificationState.getPrayerConfigs()) {
            updatedConfigs.add(item.withMode(selectedMode).withSound(selectedSound));
        }

        currentNotificationState = new NotificationSettingsState(
                binding.switchPrayerReminder.isChecked(),
                updatedConfigs,
                binding.switchDailyHadith.isChecked(),
                hadithTime,
                binding.switchNearPrayerHadith.isChecked()
        );
        repository.updateNotificationSettings(currentNotificationState);

        String themeMode = selectedThemeMode();
        int offsetMinutes = currentSettingsState.getNotificationOffsetMinutes();
        repository.updateSettings(
                repository.getSelectedCityName(),
                binding.switchPrayerReminder.isChecked(),
                offsetMinutes,
                themeMode,
                currentSettingsState.getDataSource()
        );

        applyTheme(themeMode);
        saveLocalAlarmPreferences();
        NotificationOrchestrator.applySettingsAndSchedule(requireContext(), repository);
        updateSystemStatus();
        Snackbar.make(anchor, R.string.settings_saved, Snackbar.LENGTH_SHORT).show();
    }

    private void navigateAbout(View anchor) {
        NavController navController = Navigation.findNavController(anchor);
        navController.navigate(R.id.aboutFragment);
    }

    private void openNotificationPermissionFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !NotificationPermissionHelper.hasNotificationPermission(requireContext())) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }

        NotificationPermissionHelper.openSafe(
                requireActivity(),
                NotificationPermissionHelper.buildNotificationSettingsIntent(requireContext())
        );
    }

    private void updateLocationPermissionChip() {
        if (binding == null) {
            return;
        }
        boolean granted = hasLocationPermission();
        binding.chipLocationPermission.setText(
                granted ? R.string.settings_location_permission_granted : R.string.settings_location_permission_needed
        );
        binding.chipLocationPermission.setChipStrokeColorResource(
                granted ? R.color.brand_primary : R.color.slot_border_active
        );
    }

    private boolean hasLocationPermission() {
        int fine = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        );
        int coarse = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
        );
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED;
    }

    private void updateSystemStatus() {
        if (binding == null || getContext() == null) {
            return;
        }

        boolean notificationOk = NotificationPermissionHelper.hasNotificationPermission(requireContext());
        boolean exactOk = NotificationPermissionHelper.canScheduleExactAlarms(requireContext());
        boolean batteryIgnored = NotificationPermissionHelper.isIgnoringBatteryOptimizations(requireContext());

        binding.textNotificationStatus.setText(getString(
                R.string.settings_status_notification,
                notificationOk ? getString(R.string.settings_status_active) : getString(R.string.settings_status_inactive)
        ));
        binding.textExactAlarmStatus.setText(getString(
                R.string.settings_status_exact,
                exactOk ? getString(R.string.settings_status_active) : getString(R.string.settings_status_inactive)
        ));
        binding.textBatteryStatus.setText(getString(
                R.string.settings_status_battery,
                batteryIgnored ? getString(R.string.settings_status_active) : getString(R.string.settings_status_restricted)
        ));
    }

    private void loadLocalAlarmPreferences() {
        boolean vibrationEnabled = sharedPreferences.getBoolean(PREF_VIBRATION_ENABLED, true);
        int snooze = sharedPreferences.getInt(PREF_SNOOZE_MINUTES, 5);
        binding.switchAlarmVibration.setChecked(vibrationEnabled);
        checkSnoozeToggle(snooze);
    }

    private void saveLocalAlarmPreferences() {
        int snoozeMinutes = selectedSnoozeMinutes();
        sharedPreferences.edit()
                .putBoolean(PREF_VIBRATION_ENABLED, binding.switchAlarmVibration.isChecked())
                .putInt(PREF_SNOOZE_MINUTES, snoozeMinutes)
                .apply();
    }

    private PrayerNotificationConfig firstConfigOrDefault(List<PrayerNotificationConfig> configs) {
        if (configs != null && !configs.isEmpty()) {
            return configs.get(0);
        }
        List<PrayerNotificationConfig> defaults = PrayerNotificationConfig.defaultList();
        return defaults.get(0);
    }

    private void checkThemeToggle(String themeMode) {
        int checkedId = R.id.button_theme_system;
        if (SettingsState.THEME_LIGHT.equals(themeMode)) {
            checkedId = R.id.button_theme_light;
        } else if (SettingsState.THEME_DARK.equals(themeMode)) {
            checkedId = R.id.button_theme_dark;
        }
        binding.toggleTheme.check(checkedId);
    }

    private String selectedThemeMode() {
        int checkedId = binding.toggleTheme.getCheckedButtonId();
        if (checkedId == R.id.button_theme_light) {
            return SettingsState.THEME_LIGHT;
        }
        if (checkedId == R.id.button_theme_dark) {
            return SettingsState.THEME_DARK;
        }
        return SettingsState.THEME_SYSTEM;
    }

    private void checkModeToggle(String mode) {
        int checkedId = PrayerNotificationConfig.MODE_ALARM.equals(mode)
                ? R.id.button_mode_alarm
                : R.id.button_mode_notification;
        binding.toggleMode.check(checkedId);
    }

    private String selectedMode() {
        int checkedId = binding.toggleMode.getCheckedButtonId();
        if (checkedId == R.id.button_mode_alarm) {
            return PrayerNotificationConfig.MODE_ALARM;
        }
        return PrayerNotificationConfig.MODE_NOTIFICATION;
    }

    private void checkSnoozeToggle(int minute) {
        int checkedId = R.id.button_snooze_5;
        if (minute == 10) {
            checkedId = R.id.button_snooze_10;
        } else if (minute == 15) {
            checkedId = R.id.button_snooze_15;
        }
        binding.toggleSnooze.check(checkedId);
    }

    private int selectedSnoozeMinutes() {
        int checkedId = binding.toggleSnooze.getCheckedButtonId();
        if (checkedId == R.id.button_snooze_10) {
            return 10;
        }
        if (checkedId == R.id.button_snooze_15) {
            return 15;
        }
        return 5;
    }

    private String soundToLabel(String sound) {
        if (PrayerNotificationConfig.SOUND_EZAN_1.equals(sound)) {
            return getString(R.string.sound_ezan_1);
        }
        if (PrayerNotificationConfig.SOUND_EZAN_2.equals(sound)) {
            return getString(R.string.sound_ezan_2);
        }
        if (PrayerNotificationConfig.SOUND_PHONE.equals(sound)) {
            return getString(R.string.sound_phone);
        }
        return getString(R.string.sound_default);
    }

    private String labelToSound(String label) {
        if (label.equals(getString(R.string.sound_ezan_1))) {
            return PrayerNotificationConfig.SOUND_EZAN_1;
        }
        if (label.equals(getString(R.string.sound_ezan_2))) {
            return PrayerNotificationConfig.SOUND_EZAN_2;
        }
        if (label.equals(getString(R.string.sound_phone))) {
            return PrayerNotificationConfig.SOUND_PHONE;
        }
        return PrayerNotificationConfig.SOUND_DEFAULT;
    }

    private String dataSourceToLabel(String dataSource) {
        if (SettingsState.SOURCE_ALTERNATIVE.equals(dataSource)) {
            return getString(R.string.source_alternative);
        }
        return getString(R.string.source_diyanet);
    }

    private void applyTheme(String themeMode) {
        if (SettingsState.THEME_DARK.equals(themeMode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            return;
        }
        if (SettingsState.THEME_LIGHT.equals(themeMode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            return;
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
