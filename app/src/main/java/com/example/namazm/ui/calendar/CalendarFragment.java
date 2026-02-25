package com.example.namazm.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.namazm.R;
import com.example.namazm.data.model.CalendarOverview;
import com.example.namazm.data.model.PrayerTime;
import com.example.namazm.data.model.RamadanDay;
import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.databinding.FragmentCalendarBinding;
import com.example.namazm.ui.common.NamazViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class CalendarFragment extends Fragment {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_IMSAK_IFTAR = 1;
    private static final int FILTER_WEEKLY = 2;
    private static final int WEEKLY_COUNT = 7;

    private FragmentCalendarBinding binding;
    private MonthlyPrayerAdapter monthlyPrayerAdapter;
    private RamadanImsakiyeAdapter ramadanImsakiyeAdapter;
    private String[] monthLabels = new String[0];

    private CalendarOverview currentOverview;
    private List<PrayerTime> monthlyPrayerTimes = new ArrayList<>();
    private List<RamadanDay> ramadanDays = new ArrayList<>();
    private PrayerTime selectedPrayerDay;
    private RamadanDay selectedRamadanDay;
    private int filterMode = FILTER_ALL;
    private int selectedMonthIndex;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NamazRepository repository = ServiceLocator.provideRepository();
        monthLabels = getResources().getStringArray(R.array.calendar_months);

        monthlyPrayerAdapter = new MonthlyPrayerAdapter(this::onPrayerDaySelected);
        ramadanImsakiyeAdapter = new RamadanImsakiyeAdapter(this::onRamadanDaySelected);
        binding.recyclerMonthly.setAdapter(monthlyPrayerAdapter);
        binding.recyclerRamadan.setAdapter(ramadanImsakiyeAdapter);

        binding.buttonMonthPrev.setOnClickListener(v -> shiftMonth(-1));
        binding.buttonMonthNext.setOnClickListener(v -> shiftMonth(1));
        binding.buttonMonthPicker.setOnClickListener(this::showMonthPickerMenu);
        binding.buttonToday.setOnClickListener(v -> resetToCurrentMonth());

        binding.chipFilterAll.setOnClickListener(v -> setFilterMode(FILTER_ALL));
        binding.chipFilterImsakIftar.setOnClickListener(v -> setFilterMode(FILTER_IMSAK_IFTAR));
        binding.chipFilterWeekly.setOnClickListener(v -> setFilterMode(FILTER_WEEKLY));

        binding.buttonDetailNotification.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.notificationSettingsFragment);
        });

        NamazViewModelFactory factory = new NamazViewModelFactory(repository);
        CalendarViewModel viewModel = new ViewModelProvider(this, factory).get(CalendarViewModel.class);
        viewModel.getOverview().observe(getViewLifecycleOwner(), this::bindOverview);
    }

    private void bindOverview(CalendarOverview overview) {
        currentOverview = overview;
        monthlyPrayerTimes = new ArrayList<>(overview.getMonthlyPrayerTimes());
        ramadanDays = new ArrayList<>(overview.getRamadanSchedule());

        binding.textCityAuto.setText(getString(R.string.calendar_city_auto_format, overview.getCityLabel()));
        binding.textTodayTitle.setText(overview.getTodayCountdownTitle());
        binding.textTodayCountdown.setText(overview.getTodayCountdownValue());
        binding.textTodaySahur.setText(getString(R.string.sahur_value, overview.getTodaySahur()));
        binding.textTodayIftar.setText(getString(R.string.iftar_value, overview.getTodayIftar()));

        selectedMonthIndex = resolveMonthIndex(overview.getSelectedMonthLabel());
        updateMonthLabel();

        if (selectedPrayerDay == null && !monthlyPrayerTimes.isEmpty()) {
            selectedPrayerDay = monthlyPrayerTimes.get(0);
        }
        if (selectedRamadanDay == null && !ramadanDays.isEmpty()) {
            selectedRamadanDay = ramadanDays.get(0);
        }

        setFilterMode(overview.isRamadanMonth() ? FILTER_IMSAK_IFTAR : FILTER_ALL);
    }

    private void setFilterMode(int filterMode) {
        this.filterMode = filterMode;
        binding.chipFilterAll.setChecked(filterMode == FILTER_ALL);
        binding.chipFilterImsakIftar.setChecked(filterMode == FILTER_IMSAK_IFTAR);
        binding.chipFilterWeekly.setChecked(filterMode == FILTER_WEEKLY);
        renderCalendarState();
    }

    private void renderCalendarState() {
        if (currentOverview == null) {
            return;
        }

        if (currentOverview.isRamadanMonth()) {
            binding.cardRamadanHero.setVisibility(View.VISIBLE);
            binding.recyclerMonthly.setVisibility(View.GONE);
            binding.recyclerRamadan.setVisibility(View.VISIBLE);

            List<RamadanDay> rows = filteredRamadanRows();
            ramadanImsakiyeAdapter.submitRamadanDays(rows);

            if (rows.isEmpty()) {
                selectedRamadanDay = null;
                bindRamadanDetail(null);
                return;
            }

            if (selectedRamadanDay == null || !containsRamadan(rows, selectedRamadanDay.getDayLabel())) {
                selectedRamadanDay = rows.get(0);
            }

            ramadanImsakiyeAdapter.setSelectedDayLabel(selectedRamadanDay.getDayLabel());
            bindRamadanDetail(selectedRamadanDay);
            return;
        }

        binding.cardRamadanHero.setVisibility(View.GONE);
        binding.recyclerMonthly.setVisibility(View.VISIBLE);
        binding.recyclerRamadan.setVisibility(View.GONE);

        monthlyPrayerAdapter.setCompactMode(filterMode == FILTER_IMSAK_IFTAR);
        List<PrayerTime> rows = filteredMonthlyRows();
        monthlyPrayerAdapter.submitPrayerTimes(rows);

        if (rows.isEmpty()) {
            selectedPrayerDay = null;
            bindPrayerDetail(null);
            return;
        }

        if (selectedPrayerDay == null || !containsPrayer(rows, selectedPrayerDay.getDayLabel())) {
            selectedPrayerDay = rows.get(0);
        }

        monthlyPrayerAdapter.setSelectedDayLabel(selectedPrayerDay.getDayLabel());
        bindPrayerDetail(selectedPrayerDay);
    }

    private List<PrayerTime> filteredMonthlyRows() {
        if (filterMode != FILTER_WEEKLY || monthlyPrayerTimes.size() <= WEEKLY_COUNT) {
            return new ArrayList<>(monthlyPrayerTimes);
        }
        return new ArrayList<>(monthlyPrayerTimes.subList(0, WEEKLY_COUNT));
    }

    private List<RamadanDay> filteredRamadanRows() {
        if (filterMode != FILTER_WEEKLY || ramadanDays.size() <= WEEKLY_COUNT) {
            return new ArrayList<>(ramadanDays);
        }
        return new ArrayList<>(ramadanDays.subList(0, WEEKLY_COUNT));
    }

    private void onPrayerDaySelected(PrayerTime prayerTime) {
        selectedPrayerDay = prayerTime;
        monthlyPrayerAdapter.setSelectedDayLabel(prayerTime.getDayLabel());
        bindPrayerDetail(prayerTime);
    }

    private void onRamadanDaySelected(RamadanDay ramadanDay) {
        selectedRamadanDay = ramadanDay;
        ramadanImsakiyeAdapter.setSelectedDayLabel(ramadanDay.getDayLabel());
        bindRamadanDetail(ramadanDay);
    }

    private void bindPrayerDetail(PrayerTime prayerTime) {
        String city = currentOverview == null ? "" : currentOverview.getCityLabel();
        binding.textDetailCity.setText(city);

        if (prayerTime == null) {
            binding.textDetailDate.setText(getString(R.string.calendar_no_data));
            binding.textDetailImsak.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_imsak), getString(R.string.calendar_not_available)));
            binding.textDetailGunes.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_gunes), getString(R.string.calendar_not_available)));
            binding.textDetailOgle.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_ogle), getString(R.string.calendar_not_available)));
            binding.textDetailIkindi.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_ikindi), getString(R.string.calendar_not_available)));
            binding.textDetailAksam.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_aksam), getString(R.string.calendar_not_available)));
            binding.textDetailYatsi.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_yatsi), getString(R.string.calendar_not_available)));
            return;
        }

        binding.textDetailDate.setText(getString(R.string.calendar_detail_date_format, prayerTime.getDayLabel(), binding.textMonthLabel.getText()));
        binding.textDetailImsak.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_imsak), prayerTime.getImsak()));
        binding.textDetailGunes.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_gunes), prayerTime.getGunes()));
        binding.textDetailOgle.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_ogle), prayerTime.getOgle()));
        binding.textDetailIkindi.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_ikindi), prayerTime.getIkindi()));
        binding.textDetailAksam.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_aksam), prayerTime.getAksam()));
        binding.textDetailYatsi.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_yatsi), prayerTime.getYatsi()));
    }

    private void bindRamadanDetail(RamadanDay ramadanDay) {
        String city = currentOverview == null ? "" : currentOverview.getCityLabel();
        binding.textDetailCity.setText(city);

        if (ramadanDay == null) {
            bindPrayerDetail(null);
            return;
        }

        binding.textDetailDate.setText(getString(R.string.calendar_detail_ramadan_date_format, ramadanDay.getDayLabel(), binding.textMonthLabel.getText()));
        binding.textDetailImsak.setText(getString(R.string.calendar_imsak_row_format, ramadanDay.getImsak()));
        binding.textDetailGunes.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_gunes), getString(R.string.calendar_not_available)));
        binding.textDetailOgle.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_ogle), getString(R.string.calendar_not_available)));
        binding.textDetailIkindi.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_ikindi), getString(R.string.calendar_not_available)));
        binding.textDetailAksam.setText(getString(R.string.calendar_iftar_row_format, ramadanDay.getIftar()));
        binding.textDetailYatsi.setText(getString(R.string.calendar_time_row_format, getString(R.string.prayer_yatsi), getString(R.string.calendar_not_available)));
    }

    private void showMonthPickerMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        for (int index = 0; index < monthLabels.length; index++) {
            popupMenu.getMenu().add(0, index, index, monthLabels[index]);
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            selectedMonthIndex = item.getItemId();
            updateMonthLabel();
            Snackbar.make(anchor, getString(R.string.calendar_month_changed, monthLabels[selectedMonthIndex]), Snackbar.LENGTH_SHORT).show();
            return true;
        });
        popupMenu.show();
    }

    private void shiftMonth(int delta) {
        if (monthLabels.length == 0) {
            return;
        }
        int next = selectedMonthIndex + delta;
        if (next < 0) {
            next = monthLabels.length - 1;
        } else if (next >= monthLabels.length) {
            next = 0;
        }
        selectedMonthIndex = next;
        updateMonthLabel();
    }

    private void resetToCurrentMonth() {
        if (currentOverview == null) {
            return;
        }
        selectedMonthIndex = resolveMonthIndex(currentOverview.getSelectedMonthLabel());
        updateMonthLabel();
        renderCalendarState();
        Snackbar.make(binding.getRoot(), R.string.calendar_today_selected, Snackbar.LENGTH_SHORT).show();
    }

    private void updateMonthLabel() {
        if (monthLabels.length == 0) {
            binding.textMonthLabel.setText(getString(R.string.month_selector_hint));
            return;
        }
        binding.textMonthLabel.setText(monthLabels[selectedMonthIndex]);
    }

    private int resolveMonthIndex(String monthLabel) {
        if (monthLabel == null || monthLabels.length == 0) {
            return 0;
        }
        for (int index = 0; index < monthLabels.length; index++) {
            if (monthLabel.equalsIgnoreCase(monthLabels[index])) {
                return index;
            }
        }
        return 0;
    }

    private boolean containsPrayer(List<PrayerTime> list, String dayLabel) {
        for (PrayerTime item : list) {
            if (item.getDayLabel().equals(dayLabel)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRamadan(List<RamadanDay> list, String dayLabel) {
        for (RamadanDay item : list) {
            if (item.getDayLabel().equals(dayLabel)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
