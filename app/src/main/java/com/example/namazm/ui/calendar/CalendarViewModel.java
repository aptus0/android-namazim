package com.example.namazm.ui.calendar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.namazm.data.model.CalendarOverview;
import com.example.namazm.data.repository.NamazRepository;

public class CalendarViewModel extends ViewModel {

    private final NamazRepository repository;
    private final MutableLiveData<CalendarOverview> overview = new MutableLiveData<>();

    public CalendarViewModel(NamazRepository repository) {
        this.repository = repository;
        refresh();
    }

    public LiveData<CalendarOverview> getOverview() {
        return overview;
    }

    public void refresh() {
        overview.setValue(repository.getCalendarOverview());
    }
}
