package com.example.namazm.data.model;

import java.util.List;

public class SettingsState {

    public static final String THEME_SYSTEM = "SYSTEM";
    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_DARK = "DARK";

    public static final String SOURCE_DIYANET = "DIYANET";
    public static final String SOURCE_ALTERNATIVE = "ALTERNATIVE";

    private final boolean notificationsEnabled;
    private final String selectedCityName;
    private final int notificationOffsetMinutes;
    private final String themeMode;
    private final String dataSource;
    private final List<String> availableCityNames;

    public SettingsState(
            boolean notificationsEnabled,
            String selectedCityName,
            int notificationOffsetMinutes,
            String themeMode,
            String dataSource,
            List<String> availableCityNames
    ) {
        this.notificationsEnabled = notificationsEnabled;
        this.selectedCityName = selectedCityName;
        this.notificationOffsetMinutes = notificationOffsetMinutes;
        this.themeMode = themeMode;
        this.dataSource = dataSource;
        this.availableCityNames = availableCityNames;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public String getSelectedCityName() {
        return selectedCityName;
    }

    public int getNotificationOffsetMinutes() {
        return notificationOffsetMinutes;
    }

    public String getThemeMode() {
        return themeMode;
    }

    public String getDataSource() {
        return dataSource;
    }

    public List<String> getAvailableCityNames() {
        return availableCityNames;
    }
}
