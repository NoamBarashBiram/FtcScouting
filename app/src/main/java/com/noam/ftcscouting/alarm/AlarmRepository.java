package com.noam.ftcscouting.alarm;

import android.content.Context;

import java.util.List;

public class AlarmRepository {
    private final AlarmDao alarmDao;

    public AlarmRepository(Context context) {
        AlarmDatabase db = AlarmDatabase.getDatabase(context);
        alarmDao = db.alarmDao();
    }

    public void insert(Alarm alarm) {
        alarmDao.insert(alarm);
    }

    public void update(Alarm alarm) {
        AlarmDatabase.databaseWriteExecutor.execute(() -> {
            alarmDao.update(alarm);
        });
    }

    public void clear() {
        alarmDao.clear();
    }

    public void remove(Alarm alarm) {
        alarmDao.remove(alarm.id);
    }

    public void remove(long id) {
        alarmDao.remove(id);
    }

    public List<Alarm> findByEvent(String event) {
        return alarmDao.findByEvent(event);
    }


    public List<Alarm> getAlarmsList() {
        return alarmDao.getAlarms();
    }
}