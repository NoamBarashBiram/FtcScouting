package com.noam.ftcscouting.alarm;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AlarmDao {
    @Insert
    void insert(Alarm alarm);

    @Query("DELETE FROM alarm_table")
    void clear();

    @Query("DELETE FROM alarm_table WHERE id = :id")
    void remove(long id);

    @Query("SELECT * FROM alarm_table WHERE event LIKE :event")
    List<Alarm> findByEvent(String event);

    @Query("SELECT * FROM alarm_table")
    List<Alarm> getAlarms();

    @Update
    void update(Alarm alarm);
}