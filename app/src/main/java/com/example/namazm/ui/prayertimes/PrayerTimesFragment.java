package com.example.namazm.ui.prayertimes;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.namazm.R;
import com.example.namazm.data.model.City;
import com.example.namazm.data.model.LocationSuggestion;
import com.example.namazm.data.model.PrayerSlot;
import com.example.namazm.data.model.PrayerTimesOverview;
import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.databinding.BottomSheetPrayerOptionsBinding;
import com.example.namazm.databinding.FragmentPrayerTimesBinding;
import com.example.namazm.notifications.NotificationOrchestrator;
import com.example.namazm.startup.PrayerTimesInitializer;
import com.example.namazm.ui.common.NamazViewModelFactory;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrayerTimesFragment extends Fragment {

    private static final Locale LOCALE_TR = Locale.forLanguageTag("tr-TR");

    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private final Runnable tickerRunnable = new Runnable() {
        @Override
        public void run() {
            if (binding == null || prayerTimesViewModel == null) {
                return;
            }
            if (checkDayChanged()) {
                refreshPrayerDataForNewDay();
            }
            prayerTimesViewModel.refresh();
            tickerHandler.postDelayed(this, 1000L);
        }
    };

    private FragmentPrayerTimesBinding binding;
    private PrayerTimesAdapter prayerTimesAdapter;
    private PrayerTimesViewModel prayerTimesViewModel;
    private NamazRepository repository;
    private PrayerTimesInitializer prayerTimesInitializer;
    private ExecutorService ioExecutor;
    private Context appContext;
    private LocalDate lastObservedDate = LocalDate.now();
    private final AtomicBoolean dailyRefreshInProgress = new AtomicBoolean(false);

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPrayerTimesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = ServiceLocator.provideRepository();
        appContext = requireContext().getApplicationContext();
        prayerTimesInitializer = new PrayerTimesInitializer(appContext, repository);
        ioExecutor = Executors.newSingleThreadExecutor();

        prayerTimesAdapter = new PrayerTimesAdapter(this::showPrayerOptionsBottomSheet);
        binding.recyclerPrayerSlots.setAdapter(prayerTimesAdapter);

        NamazViewModelFactory factory = new NamazViewModelFactory(repository);
        prayerTimesViewModel = new ViewModelProvider(this, factory).get(PrayerTimesViewModel.class);
        prayerTimesViewModel.getOverview().observe(getViewLifecycleOwner(), this::bindOverview);

        binding.buttonRefresh.setOnClickListener(v -> prayerTimesViewModel.refresh());
        binding.layoutCitySelector.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.citySelectFragment);
        });
        binding.cardRamadanShortcut.setOnClickListener(v -> openBottomTab(R.id.calendarFragment));
        binding.cardHadithShortcut.setOnClickListener(v -> openBottomTab(R.id.dailyHadithFragment));
        binding.cardQiblaCompassShortcut.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.qiblaCompassFragment);
        });
        binding.cardQiblaMapShortcut.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.qiblaMapFragment);
        });
        binding.buttonManageNotifications.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.notificationSettingsFragment);
        });

        lastObservedDate = LocalDate.now();
    }

    @Override
    public void onStart() {
        super.onStart();
        startTicker();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopTicker();
    }

    private void bindOverview(PrayerTimesOverview overview) {
        binding.textCityName.setText(overview.getCityName());
        binding.textNextPrayerName.setText(overview.getNextPrayerName().toUpperCase(LOCALE_TR));
        binding.textNextPrayerTime.setText(overview.getNextPrayerTime());
        binding.textRemainingLarge.setText(overview.getRemainingTime());
        binding.progressNextPrayer.setProgressCompat(overview.getNextPrayerProgress(), true);

        binding.textCityWarning.setVisibility(overview.isCitySelectionRequired() ? View.VISIBLE : View.GONE);
        binding.textOfflineBadge.setVisibility(overview.isOfflineMode() ? View.VISIBLE : View.GONE);
        binding.textLastUpdated.setText(overview.getLastUpdatedLabel());
        binding.textCityAutoBadge.setVisibility(overview.isCitySelectionRequired() ? View.GONE : View.VISIBLE);
        binding.viewLocationStatus.setAlpha(overview.isCitySelectionRequired() ? 0.35f : 1f);

        binding.imageNotificationState.setImageResource(
                overview.isNotificationsEnabled() ? R.drawable.ic_notification_on : R.drawable.ic_notification_off
        );
        binding.imageNotificationState.setContentDescription(
                getString(overview.isNotificationsEnabled()
                        ? R.string.notifications_active
                        : R.string.notifications_passive)
        );
        int notificationTint = ContextCompat.getColor(
                requireContext(),
                overview.isNotificationsEnabled() ? R.color.brand_primary : R.color.bottom_nav_unselected
        );
        binding.imageNotificationState.setImageTintList(ColorStateList.valueOf(notificationTint));

        String imsak = findPrayerTime(overview.getTodaySlots(), "imsak");
        String yatsi = findPrayerTime(overview.getTodaySlots(), "yats");
        String iftar = findPrayerTime(overview.getTodaySlots(), "akşam", "aksam");
        binding.textDayBounds.setText(getString(R.string.day_bounds_format, imsak, yatsi));
        binding.textRamadanIftar.setText(getString(R.string.iftar_card_value, iftar));
        binding.textRamadanSahur.setText(getString(R.string.sahur_card_value, imsak));

        prayerTimesAdapter.submitList(overview.getTodaySlots());
    }

    private void showPrayerOptionsBottomSheet(PrayerSlot prayerSlot) {
        BottomSheetPrayerOptionsBinding sheetBinding = BottomSheetPrayerOptionsBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetBinding.getRoot());

        sheetBinding.textPrayerTitle.setText(
                getString(R.string.prayer_options_title, prayerSlot.getName(), prayerSlot.getTime())
        );

        sheetBinding.buttonOption10Min.setOnClickListener(v -> {
            showOptionSaved(v, R.string.prayer_option_saved_10_min, prayerSlot.getName());
            dialog.dismiss();
        });
        sheetBinding.buttonOption15Min.setOnClickListener(v -> {
            showOptionSaved(v, R.string.prayer_option_saved_15_min, prayerSlot.getName());
            dialog.dismiss();
        });
        sheetBinding.buttonOptionAlarmMode.setOnClickListener(v -> {
            showOptionSaved(v, R.string.prayer_option_saved_alarm_mode, prayerSlot.getName());
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showOptionSaved(View anchor, int messageResId, String prayerName) {
        Snackbar.make(anchor, getString(messageResId, prayerName), Snackbar.LENGTH_SHORT).show();
    }

    private void openBottomTab(int destinationId) {
        View bottomNav = requireActivity().findViewById(R.id.bottom_nav);
        if (bottomNav instanceof BottomNavigationView) {
            ((BottomNavigationView) bottomNav).setSelectedItemId(destinationId);
            return;
        }

        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(destinationId);
    }

    private void startTicker() {
        tickerHandler.removeCallbacks(tickerRunnable);
        tickerHandler.post(tickerRunnable);
    }

    private void stopTicker() {
        tickerHandler.removeCallbacks(tickerRunnable);
    }

    private boolean checkDayChanged() {
        LocalDate today = LocalDate.now();
        if (today.equals(lastObservedDate)) {
            return false;
        }
        lastObservedDate = today;
        return true;
    }

    private void refreshPrayerDataForNewDay() {
        if (ioExecutor == null || dailyRefreshInProgress.getAndSet(true)) {
            return;
        }

        ioExecutor.execute(() -> {
            try {
                City targetCity = resolveTargetCity();
                if (targetCity != null) {
                    prayerTimesInitializer.ensureTodayPrayerTimes(targetCity, false);
                    NotificationOrchestrator.applySettingsAndSchedule(appContext, repository);
                }
            } finally {
                dailyRefreshInProgress.set(false);
                tickerHandler.post(() -> {
                    if (prayerTimesViewModel != null) {
                        prayerTimesViewModel.refresh();
                    }
                });
            }
        });
    }

    private City resolveTargetCity() {
        String selectedCityName = repository.getSelectedCityName();
        City selectedCity = repository.findCityByName(selectedCityName);
        if (selectedCity != null) {
            return selectedCity;
        }

        String selectedLocation = repository.getSelectedLocationLabel();
        LocationSuggestion selectedSuggestion = repository.findLocationByName(selectedLocation);
        if (selectedSuggestion != null) {
            City cityFromLocation = repository.findCityByName(selectedSuggestion.getCityName());
            if (cityFromLocation != null) {
                return cityFromLocation;
            }
        }

        City fallbackCity = repository.findCityByName("İstanbul");
        if (fallbackCity != null) {
            return fallbackCity;
        }

        List<City> cities = repository.getCities();
        if (cities.isEmpty()) {
            return null;
        }
        return cities.get(0);
    }

    private String findPrayerTime(List<PrayerSlot> slots, String... keys) {
        if (slots == null || slots.isEmpty()) {
            return "--:--";
        }

        for (PrayerSlot slot : slots) {
            String name = slot.getName() == null ? "" : slot.getName().toLowerCase(LOCALE_TR);
            for (String key : keys) {
                if (name.contains(key.toLowerCase(LOCALE_TR))) {
                    return slot.getTime();
                }
            }
        }
        return "--:--";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTicker();
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
            ioExecutor = null;
        }
        prayerTimesViewModel = null;
        binding = null;
    }
}
