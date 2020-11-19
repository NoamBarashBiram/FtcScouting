package com.noam.ftcscouting.alarm;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class RescheduleAlarmsService extends Service {
    private static final String TAG = "RescheduleAlarmsService";
    public static volatile AlarmRepository repo = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {
            if (repo == null)
                repo = new AlarmRepository(this);
            AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            for (Alarm alarm : repo.getAlarmsList()) {
                alarm.schedule(this, manager);
            }
        }).start();

        return START_STICKY;
    }
}
