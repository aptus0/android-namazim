package com.example.namazm.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.namazm.data.model.NotificationSettingsState;
import com.example.namazm.data.repository.NamazRepository;

public class NotificationSettingsViewModel extends ViewModel {

    private final NamazRepository repository;
    private final MutableLiveData<NotificationSettingsState> state = new MutableLiveData<>();

    public NotificationSettingsViewModel(NamazRepository repository) {
        this.repository = repository;
        load();
    }

    public LiveData<NotificationSettingsState> getState() {
        return state;
    }

    public void save(NotificationSettingsState newState) {
        repository.updateNotificationSettings(newState);
        load();
    }

    private void load() {
        state.setValue(repository.getNotificationSettings());
    }
}
