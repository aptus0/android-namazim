package com.example.namazm.ui.notifications;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.namazm.R;
import com.example.namazm.data.model.NotificationSettingsState;
import com.example.namazm.data.model.PrayerNotificationConfig;
import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.databinding.FragmentNotificationSettingsBinding;
import com.example.namazm.notifications.NotificationDebugHelper;
import com.example.namazm.notifications.NotificationOrchestrator;
import com.example.namazm.notifications.NotificationPermissionHelper;
import com.example.namazm.ui.common.NamazViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.regex.Pattern;

public class NotificationSettingsFragment extends Fragment {

    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$");

    private FragmentNotificationSettingsBinding binding;
    private PrayerSettingsAdapter prayerSettingsAdapter;
    private NamazRepository repository;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    updatePermissionStatus();
                    String message = granted
                            ? getString(R.string.permission_granted)
                            : getString(R.string.permission_denied);
                    if (binding != null) {
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
        binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = ServiceLocator.provideRepository();

        String[] offsetLabels = new String[]{
                getString(R.string.offset_exact_time),
                getString(R.string.offset_5_min),
                getString(R.string.offset_10_min),
                getString(R.string.offset_15_min),
                getString(R.string.offset_30_min)
        };
        int[] offsetValues = new int[]{0, 5, 10, 15, 30};

        String[] soundLabels = new String[]{
                getString(R.string.sound_default),
                getString(R.string.sound_ezan_1),
                getString(R.string.sound_ezan_2),
                getString(R.string.sound_phone)
        };
        String[] soundValues = new String[]{
                PrayerNotificationConfig.SOUND_DEFAULT,
                PrayerNotificationConfig.SOUND_EZAN_1,
                PrayerNotificationConfig.SOUND_EZAN_2,
                PrayerNotificationConfig.SOUND_PHONE
        };

        prayerSettingsAdapter = new PrayerSettingsAdapter(
                offsetLabels,
                offsetValues,
                soundLabels,
                soundValues
        );
        binding.recyclerPrayerSettings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPrayerSettings.setAdapter(prayerSettingsAdapter);

        NamazViewModelFactory factory = new NamazViewModelFactory(repository);
        NotificationSettingsViewModel viewModel = new ViewModelProvider(this, factory)
                .get(NotificationSettingsViewModel.class);

        viewModel.getState().observe(getViewLifecycleOwner(), this::bindState);

        binding.switchPrayerGlobal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prayerSettingsAdapter.setGlobalEnabled(isChecked);
            binding.recyclerPrayerSettings.setAlpha(isChecked ? 1f : 0.65f);
        });

        binding.buttonSave.setOnClickListener(v -> {
            NotificationSettingsState state = buildStateFromInputs();
            viewModel.save(state);
            NotificationOrchestrator.applySettingsAndSchedule(requireContext(), repository);
            updatePermissionStatus();
            Snackbar.make(v, R.string.settings_saved, Snackbar.LENGTH_SHORT).show();
        });

        binding.buttonTestNotification.setOnClickListener(v -> {
            NotificationDebugHelper.sendTestPair(requireContext());
            Snackbar.make(v, R.string.test_notification_sent, Snackbar.LENGTH_SHORT).show();
        });

        binding.buttonRequestNotificationPermission.setOnClickListener(v -> openNotificationPermissionFlow());
        binding.buttonOpenExactAlarm.setOnClickListener(v -> NotificationPermissionHelper.openSafe(
                requireActivity(),
                NotificationPermissionHelper.buildExactAlarmSettingsIntent(requireContext())
        ));
        binding.buttonOpenBatterySettings.setOnClickListener(v -> NotificationPermissionHelper.openSafe(
                requireActivity(),
                NotificationPermissionHelper.buildBatteryOptimizationIntent(requireContext())
        ));

        updatePermissionStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    private void bindState(NotificationSettingsState state) {
        binding.switchPrayerGlobal.setChecked(state.isPrayerNotificationsEnabled());
        prayerSettingsAdapter.submitList(state.getPrayerConfigs());
        prayerSettingsAdapter.setGlobalEnabled(state.isPrayerNotificationsEnabled());
        binding.recyclerPrayerSettings.setAlpha(state.isPrayerNotificationsEnabled() ? 1f : 0.65f);

        binding.switchHadithDaily.setChecked(state.isHadithDailyEnabled());
        binding.inputHadithTime.setText(state.getHadithDailyTime());
        binding.switchHadithNearPrayer.setChecked(state.isHadithNearPrayerEnabled());
    }

    private NotificationSettingsState buildStateFromInputs() {
        List<PrayerNotificationConfig> items = prayerSettingsAdapter.getCurrentItems();

        String hadithTime = binding.inputHadithTime.getText() == null
                ? ""
                : binding.inputHadithTime.getText().toString().trim();
        if (TextUtils.isEmpty(hadithTime) || !TIME_PATTERN.matcher(hadithTime).matches()) {
            hadithTime = "09:00";
        }

        return new NotificationSettingsState(
                binding.switchPrayerGlobal.isChecked(),
                items,
                binding.switchHadithDaily.isChecked(),
                hadithTime,
                binding.switchHadithNearPrayer.isChecked()
        );
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

    private void updatePermissionStatus() {
        if (binding == null || getContext() == null) {
            return;
        }

        boolean notificationOk = NotificationPermissionHelper.hasNotificationPermission(requireContext());
        boolean exactOk = NotificationPermissionHelper.canScheduleExactAlarms(requireContext());
        boolean batteryIgnored = NotificationPermissionHelper.isIgnoringBatteryOptimizations(requireContext());

        binding.textPermissionStatus.setText(getString(
                R.string.permission_status_format,
                notificationOk ? getString(R.string.permission_on) : getString(R.string.permission_off),
                exactOk ? getString(R.string.permission_on) : getString(R.string.permission_off),
                batteryIgnored ? getString(R.string.permission_on) : getString(R.string.permission_off)
        ));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
