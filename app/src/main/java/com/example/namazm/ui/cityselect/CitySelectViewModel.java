package com.example.namazm.ui.cityselect;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.namazm.data.model.LocationSuggestion;
import com.example.namazm.data.repository.NamazRepository;

import java.util.List;

public class CitySelectViewModel extends ViewModel {

    private final NamazRepository repository;
    private final MutableLiveData<List<LocationSuggestion>> locations = new MutableLiveData<>();

    public CitySelectViewModel(NamazRepository repository) {
        this.repository = repository;
        locations.setValue(repository.getLocationSuggestions());
    }

    public LiveData<List<LocationSuggestion>> getLocations() {
        return locations;
    }

    public void search(String query) {
        locations.setValue(repository.searchLocations(query));
    }

    public void selectLocation(LocationSuggestion suggestion) {
        repository.selectLocation(suggestion);
    }
}
