package com.noam.ftcscouting.ui.selfScoring;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.GenericTypeIndicator;
import com.noam.ftcscouting.MatchesActivity;
import com.noam.ftcscouting.MatchesFragment;
import com.noam.ftcscouting.R;
import com.noam.ftcscouting.database.FieldsConfig;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.utils.StaticSync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.noam.ftcscouting.database.FirebaseHandler.selfScoringEventName;
import static com.noam.ftcscouting.database.FirebaseHandler.unFireKey;
import static com.noam.ftcscouting.ui.events.EventsFragment.eventsString;

public class SelfScoringFragment extends Fragment implements StaticSync.Notifiable, MatchesFragment.OnScoreChangeListener {

    private final static String TAG = "SelfScoringFragment";

    private ArrayList<TimerSection> timerThings;
    private volatile boolean isTimerPlaying = false, isTimerPaused = false;
    private Button start, stop;
    private TextView timerText, telOpScore, autoScore;
    private Timer timer;
    private MediaPlayer player = new MediaPlayer();
    private int timerSection = -1, time = 0;
    private MatchesFragment mFragment;
    private String team;
    private boolean holdsLock;
    private Long lockLasts = null;
    private volatile boolean enabled = false;
    private TextView selfScoringDisabled;
    private int matches = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_self_scoring, container, false);
        start = root.findViewById(R.id.startTimer);
        stop = root.findViewById(R.id.stopTimer);
        timerText = root.findViewById(R.id.timerText);
        selfScoringDisabled = root.findViewById(R.id.disabled);
        autoScore = root.findViewById(R.id.autoScore);
        telOpScore = root.findViewById(R.id.telOpScore);

        start.setOnClickListener(this::startTimer);
        stop.setOnClickListener(this::stopTimer);
        root.findViewById(R.id.save).setOnClickListener(this::save);
        if (!enabled)
            selfScoringDisabled.setVisibility(View.VISIBLE);
        return root;
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        if (fragment instanceof MatchesFragment) {
            mFragment = (MatchesFragment) fragment;
            matches = getMatches();
            mFragment.init(selfScoringEventName, team, matches, holdsLock, matches+1);
            mFragment.setOnScoreChangeListener(this);
        }
        super.onAttachFragment(fragment);
    }

    private int getMatches() {
        if (!enabled) return 0;
        String matches = FirebaseHandler.snapshot
                .child(eventsString)
                .child(selfScoringEventName)
                .child(team)
                .child(FieldsConfig.matches)
                .getValue(String.class);
        return matches.equals("") ? 0 : matches.split(";").length;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (timer == null)
            timer = new Timer();
        new Thread(this::checkLock).start();
    }

    private void acquireLock() {
        if (!enabled) return;
        long time = System.currentTimeMillis();
        FirebaseHandler.silent.add(Arrays.asList(eventsString, selfScoringEventName, team, "LOCK"));
        FirebaseHandler.reference
                .child(eventsString)
                .child(selfScoringEventName)
                .child(team)
                .child("LOCK")
                .setValue(time + MatchesActivity.minute);
        lockLasts = time + MatchesActivity.minute;
        timer.schedule(new CustomTask(MatchesActivity.Task.ACQUIRE), new Date(time + MatchesActivity.fiftyFiveSeconds));
    }

    private Long getLock() {
        if (!enabled) return null;
        return FirebaseHandler.snapshot
                .child(eventsString)
                .child(selfScoringEventName)
                .child(team)
                .child("LOCK")
                .getValue(Long.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Thread(this::init).start(); // initialize everything in the background
    }

    private void init() {
        StaticSync.register(this);

        enabled = FirebaseHandler.snapshot
                .child(eventsString)
                .hasChild(selfScoringEventName);
        if (enabled) {
            team = FirebaseHandler.snapshot
                    .child(eventsString)
                    .child(selfScoringEventName)
                    .getChildren()
                    .iterator()
                    .next()
                    .getKey();

            getActivity().setTitle(unFireKey(team));
        } else {
            if (selfScoringDisabled != null)
                selfScoringDisabled.setVisibility(View.VISIBLE);
        }

        timerThings = new ArrayList<TimerSection>() {{
            add(new TimerSection(R.raw.countdown,
                    3,
                    0,
                    ContextCompat.getColor(getContext(), R.color.colorPrimary)));
            add(new TimerSection(R.raw.start, 2 * 60 + 30, 2 * 60, Color.TRANSPARENT));
            add(new TimerSection(R.raw.pick_controllers_up, 5, 0, Color.YELLOW));
            add(new TimerSection(R.raw.tel_op_start, 3, 0, Color.RED));
            add(new TimerSection(null, 2 * 60, 30, Color.TRANSPARENT));
            add(new TimerSection(R.raw.endgame, 30, 0, Color.LTGRAY));
            add(new TimerSection(R.raw.end, 0, 0, Color.TRANSPARENT));
        }};
    }

    public void startTimer(View v) {
        if (!isTimerPlaying) {
            playResumeTimer();
        } else if (isTimerPaused) {
            playResumeTimer();
            stop.setText(R.string.pause);
            start.setText(R.string.start);
        }
    }

    public void stopTimer(View v) {
        if (isTimerPlaying) {
            if (isTimerPaused) {
                resetTimer();
                stop.setText(R.string.pause);
                start.setText(R.string.start);
            } else {
                pauseTimer();
                stop.setText(R.string.reset);
                start.setText(R.string.resume);
            }
        }
    }

    private void playResumeTimer() {
        isTimerPlaying = true;
        isTimerPaused = false;
        timer = new Timer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (timerSection == -1 || time <= timerThings.get(timerSection).end + 1) {
                    timerSection++;
                    if (timerSection >= timerThings.size()) {
                        timer.cancel();
                        timerText.setBackgroundColor(Color.WHITE);
                        timerText.setText("2:30");
                        isTimerPlaying = isTimerPaused = false;
                        return;
                    }
                    TimerSection section = timerThings.get(timerSection);
                    time = section.start + 1;
                    if (section.sound != null) {
                        player = MediaPlayer.create(getContext(), section.sound);
                        player.start();
                    }
                    timerText.setBackgroundColor(section.color);
                }
                getActivity().runOnUiThread(
                        () -> {
                            timerText.setText(String.format("%s:%02d", time / 60, time % 60));
                            timerText.invalidate();
                        });
                time--;
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);
    }

    private void checkLock() {
        if (!enabled) return;
        Long lockTime = getLock();
        if (lockTime == null || lockTime < System.currentTimeMillis()) {
            acquireLock();
            holdsLock = true;
            mFragment.setEnabled(holdsLock);
            mFragment.updateUI();
        } else {
            if (lockTime.equals(lockLasts)) {
                return;
            }
            holdsLock = false;
            mFragment.setEnabled(holdsLock);
            mFragment.updateUI();
            timer.schedule(new CustomTask(MatchesActivity.Task.CHECK), new Date(lockTime + MatchesActivity.second));
            lockLasts = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isTimerPlaying && !isTimerPaused) {
            pauseTimer();
            stop.setText(R.string.reset);
            start.setText(R.string.resume);
        }
    }

    private void pauseTimer() {
        timer.cancel();
        player.stop();
        isTimerPaused = true;
    }

    private void resetTimer() {
        pauseTimer();
        timerSection = -1;
        timerText.setText("2:30");
        timerText.setBackgroundColor(Color.WHITE);
        isTimerPlaying = false;
    }

    @Override
    public void onScoreChanged(int autoScore, int telOpScore) {
        runOnUiThread(() -> {
            this.autoScore.setText(String.valueOf(autoScore));
            this.telOpScore.setText(String.valueOf(telOpScore));
        });
    }

    private void runOnUiThread(Runnable action) {
        getActivity().runOnUiThread(action);
    }

    private static class TimerSection {
        public final Integer sound;
        public final int start, end, color;

        public TimerSection(Integer sound, int start, int end, int color) {
            this.sound = sound;
            this.start = start;
            this.end = end;
            this.color = color;
        }
    }

    public class CustomTask extends TimerTask {

        private MatchesActivity.Task task;

        public CustomTask(MatchesActivity.Task task) {
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
    public void onNotified(Object message) {
        if (timer == null) return;
        if (message instanceof ArrayList) {
            ArrayList<String> realMessage = (ArrayList<String>) message;
            if (!enabled){
                if (realMessage.get(0).equals(eventsString) && realMessage.get(1).equals(selfScoringEventName)){
                    enabled = true;
                    team = FirebaseHandler.snapshot
                            .child(eventsString)
                            .child(selfScoringEventName)
                            .getChildren()
                            .iterator()
                            .next()
                            .getKey();

                    getActivity().runOnUiThread(() -> selfScoringDisabled.setVisibility(View.GONE));
                    matches = getMatches();
                    mFragment.init(selfScoringEventName, team, matches, holdsLock, matches+1);
                    mFragment.setOnScoreChangeListener(this);

                    getActivity().setTitle(unFireKey(team));
                }
            } else if (realMessage.size() >= 4) {
                if (realMessage.get(0).equals(eventsString) &&
                        realMessage.get(1).equals(selfScoringEventName) &&
                        realMessage.get(2).equals(team) &&
                        realMessage.get(3).equals("LOCK")) {
                    timer.cancel();
                    timer = new Timer();
                    checkLock();
                }
            }
        }
    }

    @Override
    public void onStop() {
        timer.cancel();
        timer = null;
        if (holdsLock)
            FirebaseHandler.reference
                    .child(eventsString)
                    .child(selfScoringEventName)
                    .child(team)
                    .child("LOCK")
                    .removeValue();
        super.onStop();
    }

    public void save(View v) {
        if (!enabled) return;
        long time = System.currentTimeMillis();
        String newMatches = FirebaseHandler.snapshot
                .child(eventsString)
                .child(selfScoringEventName)
                .child(team)
                .child(FieldsConfig.matches)
                .getValue(String.class);
        newMatches += (newMatches.equals("") ? "" : ";") + time;

        Map<String, Object> autoChanges = mFragment.getChanges(FieldsConfig.auto);
        Map<String, Object> telOpChanges = mFragment.getChanges(FieldsConfig.telOp);
        if (autoChanges == null) {
            autoChanges = FirebaseHandler.snapshot
                    .child(eventsString)
                    .child(selfScoringEventName)
                    .child(team)
                    .child(FieldsConfig.auto)
                    .getValue(new GenericTypeIndicator<Map<String, Object>>(){});
        }
        if (telOpChanges == null) {
            telOpChanges = FirebaseHandler.snapshot
                    .child(eventsString)
                    .child(selfScoringEventName)
                    .child(team)
                    .child(FieldsConfig.telOp)
                    .getValue(new GenericTypeIndicator<Map<String, Object>>(){});
        }

        Map<String, Object> update = new HashMap<>();
        update.put(FieldsConfig.matches, newMatches);
        update.put(FieldsConfig.telOp, telOpChanges);
        update.put(FieldsConfig.auto, autoChanges);
        update.put("LOCK", lockLasts);

        FirebaseHandler.reference
                .child(eventsString)
                .child(selfScoringEventName)
                .child(team)
                .updateChildren(update);

        matches++;
        mFragment.setMatchIndex(matches);
        mFragment.init(selfScoringEventName, team, matches, holdsLock, matches+1);
        mFragment.setOnScoreChangeListener(this);
        mFragment.updateUI();
    }
}