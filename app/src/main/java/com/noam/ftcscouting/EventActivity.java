package com.noam.ftcscouting;

import android.content.Intent;
import android.os.Bundle;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_EVENT;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_ID;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_MATCH;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_TEAM;

public class EventActivity extends TitleSettableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_my_matches, R.id.navigation_teams, R.id.navigation_ranks)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        new Thread(this::checkForIntent).start();
    }

    private void checkForIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_TEAM) &&
                intent.hasExtra(EXTRA_MATCH)) {
            Intent matchIntent = new Intent(this, MatchesActivity.class);
            matchIntent.putExtras(intent);
            startActivity(matchIntent);
        }
    }
}