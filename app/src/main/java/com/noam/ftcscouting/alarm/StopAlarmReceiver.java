package com.noam.ftcscouting.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.HashMap;

public class StopAlarmReceiver extends BroadcastReceiver {

    static final HashMap<Integer, AlarmService> services = new HashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        remove(intent, true);
    }

    public static void remove(Intent intent, boolean postNewNotification) {
        int id = intent.getIntExtra(AlarmReceiver.EXTRA_ID, -1);
        if (services.containsKey(id)){
            services.get(id).stop(id, postNewNotification);
            services.remove(id);
        }
    }
}
