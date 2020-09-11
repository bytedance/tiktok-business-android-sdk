package com.example.persistence;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.model.EventLog;

import java.util.List;

public class EventLogRepo {
    private EventLogDao eventLogDao;
    private LiveData<List<EventLog>> allEventLogs;

    public EventLogRepo(Application application) {
        PersistenceManager db = PersistenceManager.getDatabase(application);
        eventLogDao = db.eventLogDao();
        allEventLogs = eventLogDao.getAll();
    }

    public LiveData<List<EventLog>> getAllEventLogs() {
        return allEventLogs;
    }

    public void save(final EventLog eventLog) {
        PersistenceManager.databaseWriteExecutor.execute(() -> {
            eventLogDao.save(eventLog);
        });
    }
}
