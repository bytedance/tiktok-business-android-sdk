package com.example.persistence;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.example.model.EventLog;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class EventLogRepo {
    private EventLogDao eventLogDao;
    private LiveData<List<EventLog>> allEventLogs;

    public EventLogRepo(Application ctx) {
        PersistenceManager db = PersistenceManager.getDatabase(ctx);
        eventLogDao = db.eventLogDao();
        allEventLogs = eventLogDao.getAll();
    }

    public LiveData<List<EventLog>> getAllEventLogs() {
        return allEventLogs;
    }

    private static class getAllAsyncTask extends AsyncTask<Void, Void, List<EventLog>> {

        private EventLogDao eventLogDao;

        getAllAsyncTask(EventLogDao dao) {
            eventLogDao = dao;
        }

        @Override
        protected List<EventLog> doInBackground(Void... voids) {
            return eventLogDao.getLogs();
        }
    }

    public List<EventLog> getLogs() throws ExecutionException, InterruptedException {
        return new getAllAsyncTask(eventLogDao).execute().get();
    }

    public void save(final EventLog eventLog) {
        PersistenceManager.databaseWriteExecutor.execute(() -> eventLogDao.save(eventLog));
    }

    public void clear() {
        PersistenceManager.databaseWriteExecutor.execute(() -> eventLogDao.clear());
    }
}
