package com.example.persistence;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.example.model.EventLog;
import com.tiktok.TiktokBusinessSdk;
import com.tiktok.appevents.TTProperty;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EventLogRepo {
    private final EventLogDao eventLogDao;
    private final LiveData<List<EventLog>> allEventLogs;
    private final TiktokBusinessSdk ttSdk;

    public EventLogRepo(Application ctx) {
        PersistenceManager db = PersistenceManager.getDatabase(ctx);
        eventLogDao = db.eventLogDao();
        allEventLogs = eventLogDao.getAll();
        ttSdk = TiktokBusinessSdk.with(ctx);
    }

    public LiveData<List<EventLog>> getAllEventLogs() {
        return allEventLogs;
    }

    private static class getAllAsyncTask extends AsyncTask<Void, Void, List<EventLog>> {
        private final EventLogDao eventLogDao;

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
        try {
            JSONObject props = new JSONObject(eventLog.properties);
            Iterator iterator = props.keys();
            TTProperty ttProperty = new TTProperty();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                ttProperty.put(key, props.get(key));
            }
            ttSdk.track(eventLog.eventType, ttProperty);
            PersistenceManager.databaseWriteExecutor.execute(() -> eventLogDao.save(eventLog));
        } catch (Exception ignored) {}
    }

    public void clear() {
        PersistenceManager.databaseWriteExecutor.execute(eventLogDao::clear);
    }
}
