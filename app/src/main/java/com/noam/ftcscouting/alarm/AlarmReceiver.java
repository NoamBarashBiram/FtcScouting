package com.noam.ftcscouting.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class AlarmReceiver extends BroadcastReceiver {

    private final static String TAG = "AlarmReceiver";

    public static final String
            EXTRA_EVENT = "EXTRA_EVENT",
            EXTRA_TEAM = "EXTRA_TEAM",
            EXTRA_MATCH = "EXTRA_MATCH",
            EXTRA_ID = "EXTRA_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("TAG", "onReceive: ");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            rescheduleAlarms(context);
            Log.e(TAG, "onReceive: Rescheduling");
        } else {
            startAlarm(context, intent);
        }
    }

    private void rescheduleAlarms(Context context) {
        Intent intentService = new Intent(context, RescheduleAlarmsService.class);
        context.startService(intentService);
    }

    private void startAlarm(Context context, Intent intent) {
        Intent intentService = new Intent(context, AlarmService.class);
        intentService.putExtras(intent);

        context.startService(intentService);
    }
}
