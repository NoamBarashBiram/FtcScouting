package com.noam.ftcscouting.ui.ranks;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.noam.ftcscouting.R;
import com.noam.ftcscouting.database.FieldsConfig;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.database.ScoreCalculator;
import com.noam.ftcscouting.utils.StaticSync;

import java.util.ArrayList;
import java.util.Arrays;

import static com.noam.ftcscouting.database.FirebaseHandler.unFireKey;
import static com.noam.ftcscouting.ui.teams.TeamsFragment.EXTRA_EVENT;
import static com.noam.ftcscouting.ui.teams.TeamsFragment.eventString;

public class RanksFragment extends Fragment implements StaticSync.Notifiable {

    public static final String TAG = "RanksFragment";

    private CheckBox autoCheck, telOpCheck, penaltyCheck;

    private LinearLayout teamsLayout;
    private String event;
    private ArrayList<ScoreCalculator> teams;
    private LayoutInflater inflater;

    private static final int scoreHeight = (int) (75 * Resources.getSystem().getDisplayMetrics().scaledDensity); // translate 75 sp to dp
    private View prevView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.inflater = inflater;
        View root = inflater.inflate(R.layout.fragment_ranks, container, false);

        // Initialize all views variables
        teamsLayout = root.findViewById(R.id.teams);

        autoCheck = root.findViewById(R.id.autoCheck);
        telOpCheck = root.findViewById(R.id.telOpCheck);
        penaltyCheck = root.findViewById(R.id.penaltyCheck);

        // Set text near the CheckBoxes OnClickListener to toggle the CheckBox near it,
        // but only when the CheckBox is enabled
        root.findViewById(R.id.autoText).setOnClickListener(v -> {
            if (autoCheck.isEnabled())
                autoCheck.setChecked(!autoCheck.isChecked());
        });
        root.findViewById(R.id.telOpText).setOnClickListener(v -> {
            if (telOpCheck.isEnabled())
                telOpCheck.setChecked(!telOpCheck.isChecked());
        });
        root.findViewById(R.id.penaltyText).setOnClickListener(v -> {
            if (penaltyCheck.isEnabled())
                penaltyCheck.setChecked(!penaltyCheck.isChecked());
        });

        // Set CheckBoxes to updateUI when toggled
        autoCheck.setOnCheckedChangeListener(
                (buttonView, isChecked) -> new Thread(this::updateUI).start());
        telOpCheck.setOnCheckedChangeListener(
                (buttonView, isChecked) -> new Thread(this::updateUI).start());
        penaltyCheck.setOnCheckedChangeListener(
                (buttonView, isChecked) -> new Thread(this::updateUI).start());

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get event name from parent activity Intent
        event = getActivity().getIntent().getStringExtra(EXTRA_EVENT);

        // Set title
        getActivity().setTitle(unFireKey(event) + " Ranks");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Thread(this::init).start();
    }

    private void init() {
        StaticSync.register(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        teams = new ArrayList<>();
        for (DataSnapshot team : FirebaseHandler.snapshot.child(eventString).child(event).getChildren()) {
            teams.add(new ScoreCalculator(event, team.getKey()));
        }
        new Thread(this::updateUI).start();
    }

    @Override
    public void onNotified(Object message) {
        if (message instanceof ArrayList) {
            if (teams == null) {
                teams = new ArrayList<>();
                for (DataSnapshot team : FirebaseHandler.snapshot.child(eventString).child(event).getChildren()) {
                    teams.add(new ScoreCalculator(event, team.getKey()));
                }
                updateUI();
            }
            ArrayList<String> realMessage = (ArrayList<String>) message;
            if (eventString.equals(realMessage.get(0)) && realMessage.size() == 4 && event.equals(realMessage.get(1))) {
                if (FirebaseHandler.ADD.equals(realMessage.get(3))) {
                    teams.add(new ScoreCalculator(event, realMessage.get(2)));
                } else if (FirebaseHandler.DEL.equals(realMessage.get(3))) {
                    removeTeam(realMessage.get(2));
                } else
                    return;
                updateUI();
            }
        }
    }

    private synchronized void removeTeam(String teamName) {
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).team.equals(teamName)) {
                teams.remove(i);
                return;
            }
        }
    }

    private void runOnUiThread(Runnable action) {
        getActivity().runOnUiThread(action);
    }


    private void updateUI() {
        // Enable all checkboxes
        autoCheck.setEnabled(true);
        telOpCheck.setEnabled(true);
        penaltyCheck.setEnabled(true);

        // Make sure at least one checkbox is checked all the time
        // If only one is checked, disable it
        if (!autoCheck.isChecked()) {
            if (!telOpCheck.isChecked()) {
                penaltyCheck.setEnabled(false);
            } else if (!penaltyCheck.isChecked()) {
                telOpCheck.setEnabled(false);
            }
        } else if (!telOpCheck.isChecked() && !penaltyCheck.isChecked()) {
            autoCheck.setEnabled(false);
        }

        // Create an array of Nodes containing TeamName as the key and average score as the value
        // taking only the checked fieldKinds, Autonomous TelOp and Penalty
        TeamScore[] ranks = new TeamScore[teams.size()];
        for (int i = 0; i < ranks.length; i++) {
            Float autoScore = null, telOpScore = null, penaltyScore = null;
            if (autoCheck.isChecked()) {
                autoScore = teams.get(i).getAvg(FieldsConfig.auto);
            }
            if (telOpCheck.isChecked()) {
                telOpScore = teams.get(i).getAvg(FieldsConfig.telOp);
            }
            if (penaltyCheck.isChecked()) {
                penaltyScore = teams.get(i).getAvg(FieldsConfig.penalty);
            }
            ranks[i] = new TeamScore(teams.get(i).team, autoScore, telOpScore, penaltyScore);
        }

        // Sort the teams by their score
        Arrays.sort(ranks, (o1, o2) -> (int) (100 * (o2.getScore() - o1.getScore())));

        // Reset layout before constructing it
        runOnUiThread(() -> teamsLayout.removeAllViews());

        // Add teams one by one, in their order since the array is sorted
        for (int i = 0; i < ranks.length; i++) {
            TeamScore rank = ranks[i];
            final View team = inflater.inflate(R.layout.team, null);

            team.setOnClickListener(this::onClick);

            ((TextView) team.findViewById(R.id.rank)).setText(String.format("#%d", i + 1));
            ((TextView) team.findViewById(R.id.teamName)).setText(unFireKey(rank.key));


            ((TextView) team.findViewById(R.id.autoScore)).setText(String.format("%.2f", rank.getAutoScore()));
            ((TextView) team.findViewById(R.id.telOpScore)).setText(String.format("%.2f", rank.getTelOoScore()));
            ((TextView) team.findViewById(R.id.penaltyScore)).setText(String.format("%.2f", rank.getPenaltyScore()));

            runOnUiThread(() -> teamsLayout.addView(team));
        }
    }

    private String toString(Float score) {
        if (score == null) return "N/A";
        return score.toString();
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

    // Class to hold the teams and their score
    private static class TeamScore {
        public final String key;
        private final Float[] scores;

        public TeamScore(String key, Float autoScore, Float telOpScore, Float penaltyScore) {
            this.key = key;
            this.scores = new Float[]{autoScore, telOpScore, penaltyScore};
        }

        public float getScore() {
            float score = 0;
            for (Float fieldScore : scores) {
                if (fieldScore != null) {
                    score += fieldScore;
                }
            }
            return score;
        }

        public Float getAutoScore() {
            return scores[0];
        }

        public Float getTelOoScore() {
            return scores[1];
        }

        public Float getPenaltyScore() {
            return scores[2];
        }
    }
}