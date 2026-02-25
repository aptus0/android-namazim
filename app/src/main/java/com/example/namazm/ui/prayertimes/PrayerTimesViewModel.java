package com.example.namazm.ui.prayertimes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.namazm.data.model.PrayerTimesOverview;
import com.example.namazm.data.repository.NamazRepository;

public class PrayerTimesViewModel extends ViewModel {

    private final NamazRepository repository;
    private final MutableLiveData<PrayerTimesOverview> overview = new MutableLiveData<>();

    public PrayerTimesViewModel(NamazRepository repository) {
        this.repository = repository;
        refresh();
    }

    public LiveData<PrayerTimesOverview> getOverview() {
        return overview;
    }

    public void refresh() {
        overview.setValue(repository.getPrayerTimesOverview());
    }
}
