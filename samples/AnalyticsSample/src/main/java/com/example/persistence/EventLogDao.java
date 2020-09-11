package com.example.persistence;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.model.EventLog;

import java.util.List;

@Dao
public interface EventLogDao {
    @Query("SELECT * FROM event_logs ORDER BY id DESC")
    LiveData<List<EventLog>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void save(EventLog log);

    @Query("DELETE FROM event_logs")
    void clear();
}
