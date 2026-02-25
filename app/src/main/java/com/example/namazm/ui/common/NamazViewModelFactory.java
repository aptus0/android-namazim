package com.example.namazm.ui.common;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.ui.calendar.CalendarViewModel;
import com.example.namazm.ui.cityselect.CitySelectViewModel;
import com.example.namazm.ui.dailyhadith.DailyHadithViewModel;
import com.example.namazm.ui.favorites.FavoritesViewModel;
import com.example.namazm.ui.notifications.NotificationSettingsViewModel;
import com.example.namazm.ui.prayertimes.PrayerTimesViewModel;
import com.example.namazm.ui.settings.SettingsViewModel;

public class NamazViewModelFactory implements ViewModelProvider.Factory {

    private final NamazRepository repository;

    public NamazViewModelFactory(NamazRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(PrayerTimesViewModel.class)) {
            return (T) new PrayerTimesViewModel(repository);
        }
        if (modelClass.isAssignableFrom(CalendarViewModel.class)) {
            return (T) new CalendarViewModel(repository);
        }
        if (modelClass.isAssignableFrom(DailyHadithViewModel.class)) {
            return (T) new DailyHadithViewModel(repository);
        }
        if (modelClass.isAssignableFrom(SettingsViewModel.class)) {
            return (T) new SettingsViewModel(repository);
        }
        if (modelClass.isAssignableFrom(CitySelectViewModel.class)) {
            return (T) new CitySelectViewModel(repository);
        }
        if (modelClass.isAssignableFrom(NotificationSettingsViewModel.class)) {
            return (T) new NotificationSettingsViewModel(repository);
        }
        if (modelClass.isAssignableFrom(FavoritesViewModel.class)) {
            return (T) new FavoritesViewModel(repository);
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
