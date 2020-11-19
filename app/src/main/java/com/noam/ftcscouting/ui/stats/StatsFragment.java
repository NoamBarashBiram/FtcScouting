package com.noam.ftcscouting.ui.stats;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.noam.ftcscouting.MatchesFragment;
import com.noam.ftcscouting.R;
import com.noam.ftcscouting.database.FieldsConfig;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.database.ScoreCalculator;
import com.noam.ftcscouting.ui.views.GraphView;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.noam.ftcscouting.database.FirebaseHandler.selfScoringEventName;
import static com.noam.ftcscouting.ui.events.EventsFragment.eventsString;

public class StatsFragment extends Fragment {

    public static final String TAG = "StatsFragment";
    private static final int scoreHeight = (int) (75 * Resources.getSystem().getDisplayMetrics().scaledDensity); // translate 75 sp to dp

    private final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

    private int matches;

    private String teamName;

    private CheckBox autoCheck, teleOpCheck, penaltyCheck;

    private GraphView graph;

    private ScoreCalculator calculator;

    private LinearLayout teamsLayout;

    private View prevView, fragmentView;

    private ConstraintLayout overallView;

    private MatchesFragment mFragment;

    private Button fieldAvg, overall;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_stats, container, false);

        // Initialize all views variables
        teamsLayout = root.findViewById(R.id.teams);
        graph = root.findViewById(R.id.graph);
        autoCheck = root.findViewById(R.id.autoCheck);
        teleOpCheck = root.findViewById(R.id.teleOpCheck);
        penaltyCheck = root.findViewById(R.id.penaltyCheck);
        fieldAvg = root.findViewById(R.id.fieldAvg);
        overall = root.findViewById(R.id.overall);
        overallView = root.findViewById(R.id.overallView);
        fragmentView = root.findViewById(R.id.fragment);


        // Set text near the CheckBoxes OnClickListener to toggle the CheckBox near it,
        // but only when the CheckBox is enabled
        root.findViewById(R.id.autoText).setOnClickListener(v -> {
            if (autoCheck.isEnabled())
                autoCheck.setChecked(!autoCheck.isChecked());
        });
        root.findViewById(R.id.teleOpText).setOnClickListener(v -> {
            if (teleOpCheck.isEnabled())
                teleOpCheck.setChecked(!teleOpCheck.isChecked());
        });
        root.findViewById(R.id.penaltyText).setOnClickListener(v -> {
            if (penaltyCheck.isEnabled())
                penaltyCheck.setChecked(!penaltyCheck.isChecked());
        });

        // Set CheckBoxes to updateUI when toggled
        autoCheck.setOnCheckedChangeListener(
                (buttonView, isChecked) -> new Thread(this::updateUI).start());
        teleOpCheck.setOnCheckedChangeListener(
                (buttonView, isChecked) -> new Thread(this::updateUI).start());
        penaltyCheck.setOnCheckedChangeListener(
                (buttonView, isChecked) -> new Thread(this::updateUI).start());

        // Set top bar buttons actions
        fieldAvg.setOnClickListener(v -> {
            overall.setEnabled(true);
            fieldAvg.setEnabled(false);
            overallView.setVisibility(View.GONE);
            fragmentView.setVisibility(View.VISIBLE);
        });
        overall.setOnClickListener(v -> {
            overall.setEnabled(false);
            fieldAvg.setEnabled(true);
            overallView.setVisibility(View.VISIBLE);
            fragmentView.setVisibility(View.GONE);
        });

        // Initialize GraphView
        graph.setStrokeWidth(5);
        graph.setTextSize(40);
        graph.setRadius(7);
        graph.setYName("Total Score");

        return root;
    }

    @Override
    public void onAttachFragment(@NonNull Fragment childFragment) {
        if (childFragment instanceof MatchesFragment) {
            mFragment = (MatchesFragment) childFragment;
        }
        super.onAttachFragment(childFragment);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        teamName = FirebaseHandler.snapshot.child(eventsString)
                .child(FirebaseHandler.selfScoringEventName)
                .getChildren()
                .iterator()
                .next()
                .getKey();

        calculator = new ScoreCalculator(FirebaseHandler.selfScoringEventName, teamName);

        matches = FirebaseHandler.snapshot.child(eventsString)
                .child(calculator.event)
                .child(calculator.team)
                .child(FieldsConfig.matches)
                .getValue(String.class)
                .split(";")
                .length;

        new Thread(() -> {
            mFragment.init(selfScoringEventName, teamName, 0, true, 1);
            mFragment.showAvg();
        }).start();

        new Thread(this::updateUI).start();
    }

    private void updateUI() {
        getActivity().runOnUiThread(() -> {
            // Enable all checkboxes
            autoCheck.setEnabled(true);
            teleOpCheck.setEnabled(true);
            penaltyCheck.setEnabled(true);

            // Make sure at least one checkbox is checked all the time
            // If only one is checked, disable it
            if (!autoCheck.isChecked()) {
                if (!teleOpCheck.isChecked()) {
                    penaltyCheck.setEnabled(false);
                } else if (!penaltyCheck.isChecked()) {
                    teleOpCheck.setEnabled(false);
                }
            } else if (!teleOpCheck.isChecked() && !penaltyCheck.isChecked()) {
                autoCheck.setEnabled(false);
            }
        });

        // Reset layout before constructing it
        runOnUiThread(() -> teamsLayout.removeAllViews());

        float[] data = new float[matches];
        for (int i = 0; i < matches; i++) {
            final View team = getLayoutInflater().inflate(R.layout.team, null);

            team.setOnClickListener(this::onClick);

            Date matchDate = new Date(Long.valueOf(getMatchName(i)));

            ((TextView) team.findViewById(R.id.teamName)).setText(format.format(matchDate));

            int auto = calculator.getScore(FieldsConfig.auto, i),
                    teleOp = calculator.getScore(FieldsConfig.teleOp, i),
                    penalty = calculator.getScore(FieldsConfig.penalty, i);

            if (autoCheck.isChecked())
                data[i] += auto;
            if (teleOpCheck.isChecked())
                data[i] += teleOp;
            if (penaltyCheck.isChecked())
                data[i] += penalty;

            ((TextView) team.findViewById(R.id.autoScore)).setText(String.format("%d", auto));
            ((TextView) team.findViewById(R.id.teleOpScore)).setText(String.format("%d", teleOp));
            ((TextView) team.findViewById(R.id.penaltyScore)).setText(String.format("%d", penalty));

            ((TextView) team.findViewById(R.id.rank)).setText(String.valueOf((int) data[i]));
            runOnUiThread(() -> teamsLayout.addView(team));
        }

        graph.clear();
        graph.addData(data);
    }

    private String getMatchName(int index) {
        return FirebaseHandler.snapshot
                .child(eventsString)
                .child(selfScoringEventName)
                .child(teamName)
                .child(FieldsConfig.matches)
                .getValue(String.class)
                .split(";")
                [index];
    }

    public void onClick(View v) {
        if (prevView != null) {
            final View finalPrev = prevView;
            Animation anim1 = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    ConstraintLayout.LayoutParams params =
                            (ConstraintLayout.LayoutParams) finalPrev.getLayoutParams();
                    params.height = (int) (scoreHeight * (1 - interpolatedTime));
                    finalPrev.setLayoutParams(params);
                    super.applyTransformation(interpolatedTime, t);
                }
            };
            anim1.setDuration(100);
            finalPrev.startAnimation(anim1);
            prevView = null;
        }

        final ConstraintLayout score = v.findViewById(R.id.scoreContainer);

        if (score.getMeasuredHeight() != 0) return;

        score.setVisibility(View.VISIBLE);

        Animation anim2 = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) score.getLayoutParams();
                params.height = (int) (scoreHeight * interpolatedTime);
                score.setLayoutParams(params);
                super.applyTransformation(interpolatedTime, t);
            }
        };
        anim2.setDuration(100);
        v.startAnimation(anim2);

        prevView = score;
    }

    private void runOnUiThread(Runnable action) {
        getActivity().runOnUiThread(action);
    }
}