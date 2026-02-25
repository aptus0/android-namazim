package com.example.namazm.ui.favorites;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.namazm.data.model.FavoriteContent;
import com.example.namazm.data.repository.NamazRepository;

import java.util.List;

public class FavoritesViewModel extends ViewModel {

    private final NamazRepository repository;
    private final MutableLiveData<List<FavoriteContent>> favorites = new MutableLiveData<>();

    public FavoritesViewModel(NamazRepository repository) {
        this.repository = repository;
        load();
    }

    public LiveData<List<FavoriteContent>> getFavorites() {
        return favorites;
    }

    public void load() {
        favorites.setValue(repository.getFavorites());
    }
}
