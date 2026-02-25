package com.example.namazm.data.repository;

import com.example.namazm.data.model.CalendarOverview;
import com.example.namazm.data.model.City;
import com.example.namazm.data.model.FavoriteContent;
import com.example.namazm.data.model.HadithCollection;
import com.example.namazm.data.model.HadithOfTheDay;
import com.example.namazm.data.model.LocationSuggestion;
import com.example.namazm.data.model.NotificationSettingsState;
import com.example.namazm.data.model.PrayerSlot;
import com.example.namazm.data.model.PrayerTimesOverview;
import com.example.namazm.data.model.SettingsState;

import java.util.List;

public interface NamazRepository {

    PrayerTimesOverview getPrayerTimesOverview();

    CalendarOverview getCalendarOverview();

    HadithOfTheDay getDailyHadith(int dayOffset);

    List<HadithCollection> getHadithCollections();

    List<HadithCollection> searchHadithCollections(String query);

    boolean isFavorite(String contentId);

    void toggleFavorite(HadithOfTheDay content);

    List<FavoriteContent> getFavorites();

    FavoriteContent getFavoriteById(String id);

    SettingsState getSettingsState();

    void updateSettings(
            String cityName,
            boolean notificationsEnabled,
            int notificationOffsetMinutes,
            String themeMode,
            String dataSource
    );

    List<City> getCities();

    List<City> searchCities(String query);

    void selectCity(String cityName);

    String getSelectedCityName();

    String getSelectedLocationLabel();

    City findCityByName(String rawCityName);

    List<LocationSuggestion> getLocationSuggestions();

    List<LocationSuggestion> searchLocations(String query);

    LocationSuggestion findLocationByName(String rawName);

    void selectLocation(LocationSuggestion suggestion);

    City findNearestCity(double latitude, double longitude);

    boolean restorePrayerTimesFromCache(String cityName, String dateIso);

    void savePrayerTimesToCache(String cityName, String dateIso, List<PrayerSlot> slots, boolean offlineMode);

    NotificationSettingsState getNotificationSettings();

    void updateNotificationSettings(NotificationSettingsState state);
}
