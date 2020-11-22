package com.noam.ftcscouting.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.noam.ftcscouting.MainActivity;
import com.noam.ftcscouting.R;
import com.noam.ftcscouting.database.FirebaseHandler;

import java.util.Timer;
import java.util.TimerTask;

import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_EVENT;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_ID;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_MATCH;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_TEAM;
import static com.noam.ftcscouting.alarm.RescheduleAlarmsService.repo;

public class AlarmService extends Service {

    public static final int GROUP_NOTIFICATION_ID = 0;
    public static final String
            GROUP_ID = "MATCHES_GROUP",
            MATCH_CHANNEL_ID = "UPCOMING_MATCHES_CHANNEL";

    private final MediaPlayer player = new MediaPlayer();
    private Vibrator vibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_NORMAL);
        try {
            player.setDataSource(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.prepare();
            player.setLooping(true);
        } catch (Exception ignored) {
        }

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private Notification notificationNoAction;
    private long id;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.match_channel_name);
            String description = context.getString(R.string.match_channel_description);
            NotificationChannel matchChannel = new NotificationChannel(MATCH_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            matchChannel.setDescription(description);
            matchChannel.enableVibration(true);
            matchChannel.enableLights(true);
            matchChannel.setBypassDnd(true);
            matchChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            matchChannel.setVibrationPattern(new long[]{300, 300, 700, 700, 300});
            context.getSystemService(NotificationManager.class).createNotificationChannel(matchChannel);
        }
    }

    private static void openGroupNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat
                    .from(context)
                    .notify(GROUP_NOTIFICATION_ID,
                            new NotificationCompat.Builder(context, MATCH_CHANNEL_ID)
                                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                                    .setAutoCancel(true)
                                    .setContentTitle("A Match Is About To Begin")
                                    .setGroup(GROUP_ID)
                                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                                    .setGroupSummary(true)
                                    .build()
                    );
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean deleted = false;

        id = intent.getLongExtra(EXTRA_ID, -1);

        if (repo == null) {
            deleted = true;
            new Thread(() -> {
                repo = new AlarmRepository(this);
                repo.remove(id);
            }).start();
        }

        String eventName = intent.getStringExtra(EXTRA_EVENT),
                teamName = intent.getStringExtra(EXTRA_TEAM),
                matchName = intent.getStringExtra(EXTRA_MATCH);

        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.putExtras(intent);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) id, mainActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        final String notifText = FirebaseHandler.unFireKey(teamName) + ", " + matchName + " in " + FirebaseHandler.unFireKey(eventName);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MATCH_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent)
                .setContentTitle("A Match Is About To Begin")
                .setContentText(notifText)
                .setLights(Color.WHITE, 500, 500)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setGroup(GROUP_ID)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);

        notificationNoAction = builder.build();

        builder.setContentIntent(null);


        builder.setSubText(String.format(getString(R.string.countdown_format), 60));
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        startForeground((int) id, builder.build());

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        final int[] i = {59};

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                builder.setSubText(String.format(getString(R.string.countdown_format), i[0]));
                manager.notify((int) id, builder.build());
                i[0]--;
                if (i[0] == -1) {
                    Intent stopIntent = new Intent(AlarmService.this, StopAlarmReceiver.class);
                    stopIntent.putExtra(EXTRA_ID, id);

                    builder.setContentIntent(pendingIntent);
                    builder.addAction(-1, "Stop", PendingIntent.getBroadcast(AlarmService.this, (int) id, stopIntent, 0));
                    builder.setSubText(null);

                    manager.notify((int) id, builder.build());

                    player.start();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(new long[]{500, 500}, new int[]{255, 0}, 0));
                    } else {
                        vibrator.vibrate(new long[]{500, 500}, 0);
                    }

                    this.cancel();
                }
            }
        }, 1000, 1000);

        if (!deleted) {
            new Thread(() -> repo.remove(id)).start();
        }

        StopAlarmReceiver.services.put(id, this);

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void stop(boolean postNewNotification) {
        if (player.isPlaying()) {
            player.stop();
            vibrator.cancel();
            player.release();
        }
        stopForeground(true);

        if (postNewNotification) {
            NotificationManagerCompat.from(this).notify((int) id, notificationNoAction);
        }
    }
}
