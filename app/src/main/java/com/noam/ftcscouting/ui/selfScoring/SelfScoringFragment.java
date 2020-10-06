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

import com.noam.ftcscouting.R;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SelfScoringFragment extends Fragment {

    private ArrayList<TimerSection> timerThings;
    private volatile boolean isTimerPlaying = false, isTimerPaused = false;
    private Button start, stop;
    private TextView timerText;
    private Timer timer;
    private MediaPlayer player = new MediaPlayer();
    private int timerSection = -1, time = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_self_scoring, container, false);
        start = root.findViewById(R.id.startTimer);
        stop = root.findViewById(R.id.stopTimer);
        timerText = root.findViewById(R.id.timerText);
        start.setOnClickListener(this::startTimer);
        stop.setOnClickListener(this::stopTimer);
        return root;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
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
        super.onCreate(savedInstanceState);
    }

    public void startTimer(View v) {
        if (!isTimerPlaying) {
            playResumeTimer();
        } else if (isTimerPaused) {
            playResumeTimer();
            stop.setText("Pause");
            start.setText("Start");
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

    @Override
    public void onPause() {
        super.onPause();
        if (isTimerPlaying && !isTimerPaused){
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
}