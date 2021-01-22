package com.noam.ftcscouting;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.noam.ftcscouting.alarm.AlarmService;
import com.noam.ftcscouting.alarm.StopAlarmReceiver;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.utils.StaticSync;
import com.noam.ftcscouting.utils.Toaster;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_EVENT;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_ID;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_MATCH;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_TEAM;

public class MainActivity extends TitleSettableActivity implements StaticSync.Notifiable {

    public static String TAG = MainActivity.class.getSimpleName();

    private SharedPreferences preferences;
    private Intent matchIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StaticSync.register(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup bottom navigation panel
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_events, R.id.navigation_self_scoring)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        askForServices();

        new Thread(this::checkIntentAndAddChannel).start();
    }

    private void checkIntentAndAddChannel() {
        /* This method checks if the intent supplied to this activity has extras which require it
           to open MatchesActivity, and creates the upcoming matches NotificationChannel */
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_EVENT) &&
                intent.hasExtra(EXTRA_TEAM) &&
                intent.hasExtra(EXTRA_MATCH) &&
                intent.hasExtra(EXTRA_ID)) {
            StopAlarmReceiver.remove(intent, false);
            matchIntent = new Intent(this, EventActivity.class);
            matchIntent.putExtras(intent);
        }

        AlarmService.createNotificationChannel(this);
    }

    private void askForServices() {
        if (preferences.contains(getString(R.string.crash_report_key))) {
            // user has already chosen whether to use in the past, move on
            openLoginActivity();
            FirebaseCrashlytics
                    .getInstance()
                    .setCrashlyticsCollectionEnabled(preferences.getBoolean(getString(R.string.crash_report_key), false));
        } else {
            // an atomic boolean that indicates the user's selection
            AtomicBoolean enabled = new AtomicBoolean(false);
            new AlertDialog.Builder(this)
                    .setTitle("Crashlytics")
                    .setMessage(R.string.crashlytics_explanation)
                    .setPositiveButton(R.string.agree, (dialogInterface, i) -> enabled.set(true))
                    .setNegativeButton(R.string.do_not_agree, (d, w) -> {
                    })
                    .setCancelable(false)
                    .setOnDismissListener(dialogInterface -> {
                        preferences.edit()
                                .putBoolean(
                                        getString(R.string.crash_report_key),
                                        enabled.get())
                                .apply();
                        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled.get());
                        dialogInterface.cancel();
                        openLoginActivity();
                    }).create().show();
        }

        if ("xiaomi".equals(Build.BRAND)) {
            if (!preferences.contains(getString(R.string.accepts_optimization))) {
                new AlertDialog.Builder(this)
                        .setTitle("MIUI Battery Saver")
                        .setMessage(R.string.miui_battery_saver_msg)
                        .setNegativeButton(R.string.no, (dialog, which) ->
                                preferences.edit()
                                        .putBoolean(getString(R.string.accepts_optimization), false)
                                        .apply())
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            openBatterySettings();
                            preferences.edit()
                                    .putBoolean(getString(R.string.accepts_optimization), true)
                                    .apply();
                        }).create().show();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) this.getSystemService(POWER_SERVICE);
            if (preferences.getBoolean(getString(R.string.accepts_optimization), true) &&
                    !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent();
                new AlertDialog.Builder(this)
                        .setTitle("Battery Optimization")
                        .setMessage(R.string.battery_optimization_msg)
                        .setPositiveButton("OK", (dialog, which) -> {
                            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                            preferences.edit().putBoolean(getString(R.string.accepts_optimization),
                                    true).apply();
                        })
                        .setNegativeButton("Never", (dialog, which) ->
                                preferences.edit().putBoolean(getString(R.string.accepts_optimization),
                                        false).apply())
                        .create()
                        .show();
            }
        }
    }

    private void openBatterySettings() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"));
        intent.putExtra("package_name", getPackageName());
        intent.putExtra("package_label", getText(R.string.app_name));
        startActivity(intent);
    }

    public void openLoginActivity() {
        final Intent loginActivity = new Intent(MainActivity.this, LoginActivity.class);
        String empty = "";
        String email = preferences.getString(getString(R.string.email_key), empty);
        if (empty.equals(email)) {
            startActivity(loginActivity);
            return;
        }
        String password = preferences.getString(getString(R.string.password_key), empty);
        if (empty.equals(password)) {
            startActivity(loginActivity);
        } else {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> FirebaseHandler.openDataBase())
                    .addOnFailureListener(ex -> {
                        Toaster.toast(MainActivity.this, getString(R.string.login_failed));
                        startActivity(loginActivity);
                    });
        }
    }

    @Override
    public void onNotified(Object message) {
        if (message.equals(LoginActivity.LOGGED_IN)) {
            FirebaseHandler.openDataBase();
        } else if (message.equals(FirebaseHandler.DATABASE_CLOSED)) {
            Toaster.toast(this, "There has been an error connecting to the database");
        } else if (message.equals(FirebaseHandler.DATABASE_OPENED) && matchIntent != null) {
            startActivity(matchIntent);
        }
    }

    @Override
    protected void onDestroy() {
        StaticSync.unregisterAll();
        super.onDestroy();
    }

    public void dummyClick(View view) {
    }
}