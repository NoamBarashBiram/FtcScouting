package com.noam.ftcscouting;

import android.app.ActionBar;

import androidx.appcompat.app.AppCompatActivity;

public abstract class TitleSettableActivity extends AppCompatActivity {

    @Override
    public void setTitle(CharSequence title) {
        runOnUiThread(() -> {
            super.setTitle(title);
            ActionBar actionBar = getActionBar();
            if (actionBar == null) {
                androidx.appcompat.app.ActionBar supportActionBar = getSupportActionBar();
                if (supportActionBar != null) {
                    supportActionBar.setTitle(title);
                }
            } else {
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle(title);
            }
        });
    }
}
