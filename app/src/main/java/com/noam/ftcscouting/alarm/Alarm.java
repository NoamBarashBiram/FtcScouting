package com.noam.ftcscouting.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_EVENT;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_ID;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_MATCH;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_TEAM;

@Entity(tableName = "alarm_table")
public class Alarm {

    @PrimaryKey
    public final int id;

    @ColumnInfo
    public final long time;

    @ColumnInfo
    public final String event;

    @ColumnInfo
    public final String team;

    @ColumnInfo
    public final String match;

    public Alarm(int id, long time, @NonNull String event, @NonNull String team, @NonNull String match){
        this.id = id;
        this.time = time;
        this.event = event;
        this.team = team;
        this.match = match;
    }

    public void schedule(Context context){
        schedule(context, (AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
    }

    public void schedule(Context context, AlarmManager manager) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        intent.putExtra(EXTRA_EVENT, event);
        intent.putExtra(EXTRA_TEAM, team);
        intent.putExtra(EXTRA_MATCH, match);
        intent.putExtra(EXTRA_ID, id);

        PendingIntent alarmPending = PendingIntent.getBroadcast(context, (int) id, intent, 0);

        manager.setExact(
                AlarmManager.RTC_WAKEUP,
                time,
                alarmPending
        );
    }

    public void cancelAlarm(Context context){
        cancelAlarm(context, (AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
    }

    public void cancelAlarm(Context context, AlarmManager manager) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, (int) id, intent, 0);
        manager.cancel(alarmPendingIntent);
    }
}
