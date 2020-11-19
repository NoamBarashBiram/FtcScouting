package com.noam.ftcscouting.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.HashMap;

public class StopAlarmReceiver extends BroadcastReceiver {

    static final HashMap<Long, AlarmService> services = new HashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        remove(intent, true);
    }

    public static void remove(Intent intent, boolean postNewNotification) {
        long id = intent.getLongExtra(AlarmReceiver.EXTRA_ID, -1);
        if (services.containsKey(id)){
            services.get(id).stop(postNewNotification);
            services.remove(id);
        }
    }
}
