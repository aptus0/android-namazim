package com.example.namazm.data.model;

import java.util.List;

public class CalendarOverview {

    private final String cityLabel;
    private final String selectedMonthLabel;
    private final boolean ramadanMonth;
    private final String todayCountdownTitle;
    private final String todayCountdownValue;
    private final String todaySahur;
    private final String todayIftar;
    private final List<PrayerTime> monthlyPrayerTimes;
    private final List<RamadanDay> ramadanSchedule;

    public CalendarOverview(
            String cityLabel,
            String selectedMonthLabel,
            boolean ramadanMonth,
            String todayCountdownTitle,
            String todayCountdownValue,
            String todaySahur,
            String todayIftar,
            List<PrayerTime> monthlyPrayerTimes,
            List<RamadanDay> ramadanSchedule
    ) {
        this.cityLabel = cityLabel;
        this.selectedMonthLabel = selectedMonthLabel;
        this.ramadanMonth = ramadanMonth;
        this.todayCountdownTitle = todayCountdownTitle;
        this.todayCountdownValue = todayCountdownValue;
        this.todaySahur = todaySahur;
        this.todayIftar = todayIftar;
        this.monthlyPrayerTimes = monthlyPrayerTimes;
        this.ramadanSchedule = ramadanSchedule;
    }

    public String getCityLabel() {
        return cityLabel;
    }

    public String getSelectedMonthLabel() {
        return selectedMonthLabel;
    }

    public boolean isRamadanMonth() {
        return ramadanMonth;
    }

    public String getTodayCountdownTitle() {
        return todayCountdownTitle;
    }

    public String getTodayCountdownValue() {
        return todayCountdownValue;
    }

    public String getTodaySahur() {
        return todaySahur;
    }

    public String getTodayIftar() {
        return todayIftar;
    }

    public List<PrayerTime> getMonthlyPrayerTimes() {
        return monthlyPrayerTimes;
    }

    public List<RamadanDay> getRamadanSchedule() {
        return ramadanSchedule;
    }
}
