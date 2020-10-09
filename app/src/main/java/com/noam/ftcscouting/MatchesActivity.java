package com.noam.ftcscouting;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.noam.ftcscouting.database.FieldsConfig;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.ui.teams.TeamsFragment;
import com.noam.ftcscouting.utils.StaticSync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MatchesActivity extends TitleSettableActivity implements StaticSync.Notifiable {

    private static final String TAG = "MatchesActivity";

    private Kind kindNow = Kind.Auto;
    private MatchesFragment mFragment = null;

    private String event = null, team;
    private volatile boolean holdsLock = false;
    private TextView match, kind;
    private int matchIndex = 0;
    private String[] matches;
    private ImageView next, prev;
    private View fragmentView;

    private static final int
            disabledColor = 0xffcccccc,
            fadeDurMS = 50;
    private static final float fadeOutAlpha = 0.3f;
    private static final long second = 1000, minute = 60 * second, fiftyFiveSeconds = 55 * second;
    private Timer timer = new Timer();

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        if (fragment instanceof MatchesFragment) {
            mFragment = (MatchesFragment) fragment;
            mFragment.init(event, team, matchIndex, holdsLock, matches.length);
        }
        super.onAttachFragment(fragment);
    }

    private Animator.AnimatorListener updateUIWhenDone = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {

        }

        @Override
        public void onAnimationEnd(Animator animator) {
            updateUI();
            if (match.getAlpha() < 1) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(match, "alpha", fadeOutAlpha, 1f);
                anim.setDuration(fadeDurMS);
                anim.start();
            }
            if (kind.getAlpha() < 1) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(kind, "alpha", fadeOutAlpha, 1f);
                anim.setDuration(fadeDurMS);
                anim.start();
            }

            ObjectAnimator anim = ObjectAnimator.ofFloat(fragmentView, "alpha", fadeOutAlpha, 1f);
            anim.setDuration(fadeDurMS);
            anim.start();
        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (timer == null)
            timer = new Timer();
        checkLock();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StaticSync.register(this);

        event = getIntent().getStringExtra(TeamsFragment.EXTRA_EVENT);
        team = getIntent().getStringExtra(TeamsFragment.EXTRA_TEAM_NAME);
        matches = FirebaseHandler.snapshot
                .child("Events")
                .child(event)
                .child(team)
                .child("matches")
                .getValue(String.class)
                .split(";");
        setTitle(FirebaseHandler.unFireKey(team));

        setContentView(R.layout.activity_matches);

        match = findViewById(R.id.match);
        prev = findViewById(R.id.prev);
        next = findViewById(R.id.next);
        kind = findViewById(R.id.kind);
        fragmentView = findViewById(R.id.fragmentView);

        updateUI();
    }

    private void checkLock() {
        Long lockTime = getLock();
        if (lockTime == null || lockTime < System.currentTimeMillis()) {
            acquireLock();
            holdsLock = true;
            mFragment.setHoldsLock(holdsLock);
            animate();
        } else {
            holdsLock = false;
            mFragment.setHoldsLock(holdsLock);
            animate();
            timer.schedule(new CustomTask(Task.CHECK), new Date(lockTime + second));
        }
    }

    private void acquireLock() {
        long time = System.currentTimeMillis();
        FirebaseHandler.silent.add(Arrays.asList("Events", event, team, "LOCK"));
        FirebaseHandler.reference
                .child("Events")
                .child(event)
                .child(team)
                .child("LOCK")
                .setValue(time + minute);
        timer.schedule(new CustomTask(Task.ACQUIRE), new Date(time + fiftyFiveSeconds));
    }

    private Long getLock() {
        return FirebaseHandler.snapshot
                .child("Events")
                .child(event)
                .child(team)
                .child("LOCK")
                .getValue(Long.class);
    }

    private void animate(){
        runOnUiThread(() -> animate(false, false));
    }

    private void animate(boolean animateMatch, boolean animateKind){
        if (animateMatch) {
            match.animate()
                    .setDuration(fadeDurMS)
                    .alpha(fadeOutAlpha)
                    .setListener(updateUIWhenDone)
                    .start();
            if (animateKind){
                kind.animate()
                        .setDuration(fadeDurMS)
                        .alpha(fadeOutAlpha)
                        .start();
            }
        } else if (animateKind){
            kind.animate()
                    .setDuration(fadeDurMS)
                    .alpha(fadeOutAlpha)
                    .setListener(updateUIWhenDone)
                    .start();
        } else {
            fragmentView.animate()
                    .setDuration(fadeDurMS)
                    .alpha(fadeOutAlpha)
                    .setListener(updateUIWhenDone)
                    .start();
            return;
        }
        fragmentView.animate()
                .setDuration(fadeDurMS)
                .alpha(fadeOutAlpha)
                .start();
    }

    public void previousMatch(View v) {
        if (matchIndex <= 0) return;
        matchIndex--;
        boolean animateKind = false;
        if (kindNow == Kind.TelOp) {
            animateKind = true;
            kindNow = Kind.Auto;
        }
        animate(true, animateKind);
    }

    public void nextMatch(View v) {
        if (matchIndex >= matches.length - 1) return;
        matchIndex++;
        boolean animateKind = false;
        if (kindNow == Kind.TelOp) {
            animateKind = true;
            kindNow = Kind.Auto;
        }
        animate(true, animateKind);
    }

    private void updateUI() {
        mFragment.setMatchIndex(matchIndex);
        mFragment.setKind(kindNow);
        match.setText(matches[matchIndex]);
        if (matchIndex == 0) {
            prev.setEnabled(false);
            prev.setColorFilter(disabledColor);
        } else {
            prev.setEnabled(true);
            prev.clearColorFilter();
        }

        if (matchIndex == matches.length - 1) {
            next.setEnabled(false);
            next.setColorFilter(disabledColor);
        } else {
            next.setEnabled(true);
            next.clearColorFilter();
        }
        kind.setText(kindNow.val);
        mFragment.updateUI();
    }

    public void switchKind(View v) {
        kindNow = kindNow == Kind.Auto ? Kind.TelOp : Kind.Auto;
        animate(false, true);
    }

    @Override
    public void onNotified(Object message) {
        if (timer == null) return;
        if (message instanceof ArrayList) {
            ArrayList<String> realMessage = (ArrayList<String>) message;
            if (realMessage.size() >= 4) {
                if (realMessage.get(0).equals("Events") &&
                        realMessage.get(1).equals(event) &&
                        realMessage.get(2).equals(team) &&
                        realMessage.get(3).equals("LOCK")) {
                    timer.cancel();
                    timer = new Timer();
                    checkLock();
                }
            }
        }
    }

    public void save(View v) {
        Map<String, Object> changes = mFragment.getChanges();
        if (changes != null){
            FirebaseHandler.reference
                    .child(event)
                    .child(team)
                    .child(kindNow.val)
                    .updateChildren(changes);
        } else {
        }
    }

    enum Task {
        ACQUIRE,
        CHECK
    }

    private class CustomTask extends TimerTask {

        private Task task;

        public CustomTask(Task task) {
            this.task = task;
        }

        @Override
        public void run() {
            switch (task) {
                case CHECK:
                    checkLock();
                    break;
                case ACQUIRE:
                    acquireLock();
            }
        }
    }

    public enum Kind {
        Auto(FieldsConfig.auto),
        TelOp(FieldsConfig.telOp);

        public final String val;

        Kind(String val) {
            this.val = val;
        }
    }

    @Override
    protected void onStop() {
        timer.cancel();
        timer = null;
        if (holdsLock)
            FirebaseHandler.reference
                    .child("Events")
                    .child(event)
                    .child(team)
                    .child("LOCK")
                    .removeValue();
        super.onStop();
    }
}