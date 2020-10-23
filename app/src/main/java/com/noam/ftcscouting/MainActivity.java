package com.noam.ftcscouting;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.utils.StaticSync;
import com.noam.ftcscouting.utils.Toaster;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends TitleSettableActivity implements StaticSync.Notifiable {

    public static String TAG = MainActivity.class.getSimpleName();

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StaticSync.register(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_events, R.id.navigation_self_scoring)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        askAboutCrashlytics();
    }

    private void askAboutCrashlytics() {
        if (preferences.contains(getString(R.string.crash_report_key))) {
            openLoginActivity();
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(preferences.getBoolean(getString(R.string.crash_report_key), false));
            return;
        }
        AtomicBoolean enabled = new AtomicBoolean(false);
        new AlertDialog.Builder(this)
                .setTitle("Crashlytics")
                .setMessage(R.string.crashlytics_explanation)
                .setPositiveButton("I Agree", (dialogInterface, i) -> enabled.set(true))
                .setNegativeButton("I Do Not", (d, w) -> {
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
                        Toaster.toast(MainActivity.this, "Login Failed");
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