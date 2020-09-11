package com.example.ui.eventlog;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.model.EventLog;
import com.example.persistence.EventLogRepo;

import java.util.List;

public class EventLogViewModel extends AndroidViewModel {

    private EventLogRepo eventLogRepo;

    public EventLogViewModel(Application application) {
        super(application);
        eventLogRepo = new EventLogRepo(application);
    }

    public LiveData<List<EventLog>> getAllEventLogs() { return eventLogRepo.getAllEventLogs(); }

    public void save(EventLog eventLog) {
        eventLogRepo.save(eventLog);
    }
}