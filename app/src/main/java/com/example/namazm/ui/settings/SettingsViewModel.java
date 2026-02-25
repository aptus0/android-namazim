package com.example.namazm.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.namazm.data.model.SettingsState;
import com.example.namazm.data.repository.NamazRepository;

public class SettingsViewModel extends ViewModel {

    private final NamazRepository repository;
    private final MutableLiveData<SettingsState> state = new MutableLiveData<>();

    public SettingsViewModel(NamazRepository repository) {
        this.repository = repository;
        refresh();
    }

    public LiveData<SettingsState> getState() {
        return state;
    }

    public void saveSettings(
            boolean notificationsEnabled,
            int notificationOffsetMinutes,
            String themeMode,
            String dataSource
    ) {
        String cityName = repository.getSelectedCityName();
        repository.updateSettings(
                cityName,
                notificationsEnabled,
                notificationOffsetMinutes,
                themeMode,
                dataSource
        );
        refresh();
    }

    public void refresh() {
        state.setValue(repository.getSettingsState());
    }
}
