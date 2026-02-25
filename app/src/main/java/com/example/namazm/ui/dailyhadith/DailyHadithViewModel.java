package com.example.namazm.ui.dailyhadith;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.namazm.data.model.HadithCollection;
import com.example.namazm.data.model.HadithOfTheDay;
import com.example.namazm.data.repository.NamazRepository;

import java.util.List;

public class DailyHadithViewModel extends ViewModel {

    private final NamazRepository repository;
    private final MutableLiveData<HadithOfTheDay> content = new MutableLiveData<>();
    private final MutableLiveData<Boolean> favorite = new MutableLiveData<>(false);
    private final MutableLiveData<List<HadithCollection>> collections = new MutableLiveData<>();

    private int dayOffset = 0;

    public DailyHadithViewModel(NamazRepository repository) {
        this.repository = repository;
        refresh();
        collections.setValue(repository.getHadithCollections());
    }

    public LiveData<HadithOfTheDay> getContent() {
        return content;
    }

    public LiveData<Boolean> isFavorite() {
        return favorite;
    }

    public LiveData<List<HadithCollection>> getCollections() {
        return collections;
    }

    public void showYesterday() {
        dayOffset = -1;
        refresh();
    }

    public void showToday() {
        dayOffset = 0;
        refresh();
    }

    public void showTomorrow() {
        dayOffset = 1;
        refresh();
    }

    public void toggleFavorite() {
        HadithOfTheDay item = content.getValue();
        repository.toggleFavorite(item);
        syncFavorite();
    }

    public void filterCollections(String query) {
        collections.setValue(repository.searchHadithCollections(query));
    }

    private void refresh() {
        content.setValue(repository.getDailyHadith(dayOffset));
        syncFavorite();
    }

    private void syncFavorite() {
        HadithOfTheDay item = content.getValue();
        if (item == null) {
            favorite.setValue(false);
            return;
        }
        favorite.setValue(repository.isFavorite(item.getId()));
    }
}
