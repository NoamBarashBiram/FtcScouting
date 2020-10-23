package com.noam.ftcscouting;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.noam.ftcscouting.database.FieldsConfig;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.ui.teams.TeamsFragment;
import com.noam.ftcscouting.utils.StaticSync;
import com.noam.ftcscouting.utils.Toaster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.noam.ftcscouting.ui.events.EventsFragment.eventsString;

public class MatchesActivity extends TitleSettableActivity implements StaticSync.Notifiable {

    private static final String TAG = "MatchesActivity";

    private MatchesFragment mFragment = null;

    private String event = null, team;
    private volatile boolean holdsLock = false;
    private TextView match;
    private int matchIndex = 0;
    private String[] matches;
    private ImageView next, prev;
    private View fragmentView;
    private CoordinatorLayout saveRoot;

    private boolean played = true;
    private CheckBox playedCheckBox;

    private static final int
            disabledColor = 0xffcccccc;
    public static final int fadeDurMS = 50;
    public static final float fadeOutAlpha = 0.3f;
    public static final long second = 1000, minute = 60 * second, fiftyFiveSeconds = 55 * second;

    private Long lockLasts = null;
    private Timer timer = new Timer();
    private volatile boolean initialized = false, checkedLock = false, UIConstructed = false;
    private final Object lock = new Object();

    private final Animator.AnimatorListener updateUIWhenDone = new Animator.AnimatorListener() {
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
    public void onAttachFragment(@NonNull Fragment fragment) {
        if (fragment instanceof MatchesFragment) {
            mFragment = (MatchesFragment) fragment;
            if (matches != null) { // initialization has completed before fragment attachment
                mFragment.init(event, team, matchIndex, holdsLock, matches.length);
                initialized = true;
            }

        }
        super.onAttachFragment(fragment);
    }


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
        setContentView(R.layout.activity_matches);
        new Thread(this::init).start(); // initialize everything in the background
    }

    private void init() {
        StaticSync.register(this);

        event = getIntent().getStringExtra(TeamsFragment.EXTRA_EVENT);
        team = getIntent().getStringExtra(TeamsFragment.EXTRA_TEAM_NAME);
        matches = FirebaseHandler.snapshot
                .child(eventsString)
                .child(event)
                .child(team)
                .child("matches")
                .getValue(String.class)
                .split(";");

        played = isPlayed();

        setTitle(FirebaseHandler.unFireKey(team));

        playedCheckBox = findViewById(R.id.played);

        playedCheckBox.setChecked(played);

        match = findViewById(R.id.match);
        prev = findViewById(R.id.prev);
        next = findViewById(R.id.next);
        fragmentView = findViewById(R.id.fragmentView);
        saveRoot = findViewById(R.id.saveRoot);

        if (mFragment != null && !initialized) { // fragment has already been attached but not initialized
            mFragment.init(event, team, matchIndex, holdsLock, matches.length);
            initialized = true;
        }

        while (!(checkedLock && initialized)) {
            synchronized (lock) {
                try {
                    lock.wait(0, 1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        runOnUiThread(this::updateUI);

        UIConstructed = true;
    }

    private boolean isPlayed() {
        String[] notPlayed = FirebaseHandler.snapshot
                .child(eventsString)
                .child(event)
                .child(team)
                .child(FieldsConfig.unPlayed)
                .getValue(String.class)
                .split(";");

        String matchStr = String.valueOf(matchIndex);
        for (String match : notPlayed) {
            if (match.equals(matchStr)) {
                return false;
            }
        }
        return true;
    }

    private void checkLock() {
        Long lockTime = getLock();
        if (lockTime == null || lockTime < System.currentTimeMillis()) {
            acquireLock();
            holdsLock = true;
        } else {
            if (lockTime.equals(lockLasts)) {
                return;
            }
            holdsLock = false;
            timer.schedule(new CustomTask(Task.CHECK), new Date(lockTime + second));
            lockLasts = null;
        }

        if (mFragment != null) {
            mFragment.setEnabled(holdsLock && played);
            if (UIConstructed)
                animate();
        }
        checkedLock = true;
    }

    private void acquireLock() {
        long time = System.currentTimeMillis();
        FirebaseHandler.silent.add(Arrays.asList(eventsString, event, team, "LOCK"));
        FirebaseHandler.reference
                .child(eventsString)
                .child(event)
                .child(team)
                .child("LOCK")
                .setValue(time + minute);
        lockLasts = time + minute;
        timer.schedule(new CustomTask(Task.ACQUIRE), new Date(time + fiftyFiveSeconds));
    }

    private Long getLock() {
        return FirebaseHandler.snapshot
                .child(eventsString)
                .child(event)
                .child(team)
                .child("LOCK")
                .getValue(Long.class);
    }

    private void animate() {
        runOnUiThread(() -> animate(false));
    }

    private void animate(boolean animateMatch) {
        if (animateMatch) {
            match.animate()
                    .setDuration(fadeDurMS)
                    .alpha(fadeOutAlpha)
                    .setListener(updateUIWhenDone)
                    .start();

            fragmentView.animate()
                    .setDuration(fadeDurMS)
                    .alpha(fadeOutAlpha)
                    .start();
        } else {
            fragmentView.animate()
                    .setDuration(fadeDurMS)
                    .alpha(fadeOutAlpha)
                    .setListener(updateUIWhenDone)
                    .start();
        }
    }

    public void previousMatch(View v) {
        confirm(() -> {
            if (matchIndex == 0) return;
            matchIndex--;
            animate(true);
        });
    }

    public void nextMatch(View v) {
        confirm(() -> {
            if (matchIndex == matches.length) return;
            matchIndex++;
            animate(true);
        });
    }

    @Override
    public void onBackPressed() {
        confirm(super::onBackPressed);
    }

    private void confirm(Runnable onConformation) {
        if (!mFragment.constructedUI){
            onConformation.run();
            return;
        }
        final Map<String, Object> autoChanges = mFragment.getChanges(FieldsConfig.auto),
                telOpChanges = mFragment.getChanges(FieldsConfig.telOp),
                penaltyChanges = mFragment.getChanges(FieldsConfig.penalty);
        if (autoChanges != null || telOpChanges != null || penaltyChanges != null)
            new AlertDialog.Builder(this)
                    .setTitle(R.string.save_conformation_title)
                    .setMessage(R.string.save_conformation_msg)
                    .setPositiveButton("Save", (dialog, which) -> {
                        save(autoChanges, telOpChanges, penaltyChanges);
                        onConformation.run();
                    })
                    .setNegativeButton("Don't Save", ((dialog, which) -> onConformation.run()))
                    .create().show();
        else
            onConformation.run();
    }

    private void updateUI() {
        played = isPlayed();
        playedCheckBox.setChecked(played);
        mFragment.setMatchIndex(matchIndex);
        mFragment.setEnabled(holdsLock && played);
        if (matchIndex == 0) {
            prev.setEnabled(false);
            prev.setColorFilter(disabledColor);
        } else {
            prev.setEnabled(true);
            prev.clearColorFilter();
        }

        if (matchIndex == matches.length) {
            next.setEnabled(false);
            next.setColorFilter(disabledColor);
        } else {
            next.setEnabled(true);
            next.clearColorFilter();
        }
        if (matchIndex == matches.length) {
            mFragment.showAvg();
            match.setText(R.string.avarage);
        } else {
            mFragment.updateUI();
            match.setText(matches[matchIndex]);
        }

        if (matchIndex == matches.length || "Scouting Pit".equals(matches[matchIndex])) {
            playedCheckBox.setEnabled(false);
        } else {
            playedCheckBox.setEnabled(holdsLock);
        }
    }


    @Override
    public void onNotified(Object message) {
        if (timer == null) return;
        if (message instanceof ArrayList) {
            ArrayList<String> realMessage = (ArrayList<String>) message;
            if (realMessage.size() >= 4) {
                if (realMessage.get(0).equals(eventsString) &&
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
        if (matchIndex < matches.length)
            save(mFragment.getChanges(FieldsConfig.auto),
                    mFragment.getChanges(FieldsConfig.telOp),
                            mFragment.getChanges(FieldsConfig.penalty));
    }

    public void save(Map<String, Object> autoChanges, Map<String, Object> telOpChanges, Map<String, Object> penaltyChanges) {
        boolean changed = false;

        if (autoChanges != null) {
            FirebaseHandler.reference
                    .child(eventsString)
                    .child(event)
                    .child(team)
                    .child(FieldsConfig.auto)
                    .updateChildren(autoChanges)
                    .addOnFailureListener(e -> Toaster.toast(this, e));
            changed = true;
        }
        if (telOpChanges != null) {
            FirebaseHandler.reference
                    .child(eventsString)
                    .child(event)
                    .child(team)
                    .child(FieldsConfig.telOp)
                    .updateChildren(telOpChanges)
                    .addOnFailureListener(e -> Toaster.toast(this, e));
            changed = true;
        }
        if (penaltyChanges != null) {
            FirebaseHandler.reference
                    .child(eventsString)
                    .child(event)
                    .child(team)
                    .child(FieldsConfig.penalty)
                    .updateChildren(penaltyChanges)
                    .addOnFailureListener(e -> Toaster.toast(this, e));
            changed = true;
        }

        if (changed)
            new Thread(this::snackSave).start();
    }

    private void snackSave() {
        Toaster.snack(saveRoot, "Saved");
    }

    public void togglePlayed(View view) {
        Map<String, Object> autoChanges = mFragment.getChanges(FieldsConfig.auto);
        Map<String, Object> telOpChanges = mFragment.getChanges(FieldsConfig.telOp);

        if (autoChanges != null || telOpChanges != null) {
            playedCheckBox.setChecked(played);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.match_not_empty)
                    .setMessage(R.string.match_not_played_verification_message)
                    .setNegativeButton("No", null)
                    .setPositiveButton("Yes", ((dialog, which) -> togglePlayed()))
                    .setCancelable(true)
                    .create()
                    .show();
        } else {
            togglePlayed();
        }
    }

    private void togglePlayed() {
        played = !played;

        playedCheckBox.setChecked(played);

        ArrayList<String> notPlayed = new ArrayList<>(
                Arrays.asList(
                        FirebaseHandler.snapshot
                                .child(eventsString)
                                .child(event)
                                .child(team)
                                .child("unplayed")
                                .getValue(String.class)
                                .split(";")
                )
        );

        if (played)
            notPlayed.remove(String.valueOf(matchIndex));
        else
            notPlayed.add(String.valueOf(matchIndex));

        if (notPlayed.size() > 1 && notPlayed.get(0).equals(""))
            notPlayed.remove(0);

        FirebaseHandler.reference
                .child(eventsString)
                .child(event)
                .child(team)
                .child(FieldsConfig.unPlayed)
                .setValue(TextUtils.join(";", notPlayed))
                .addOnSuccessListener(aVoid -> updateUI());
    }

    public enum Task {
        ACQUIRE,
        CHECK
    }

    public class CustomTask extends TimerTask {

        private final Task task;

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

    @Override
    protected void onStop() {
        timer.cancel();
        timer = null;
        if (holdsLock)
            FirebaseHandler.reference
                    .child(eventsString)
                    .child(event)
                    .child(team)
                    .child("LOCK")
                    .removeValue();
        super.onStop();
    }
}