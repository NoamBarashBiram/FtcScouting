package com.noam.ftcscouting.ui.ranks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
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
        autoCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateUI());
        telOpCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateUI());
        penaltyCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateUI());

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
        updateUI();
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
            ranks[i] = new TeamScore(teams.get(i).team, 0f);
            if (autoCheck.isChecked()) {
                ranks[i].value += teams.get(i).getAvg(FieldsConfig.auto);
            }
            if (telOpCheck.isChecked()) {
                ranks[i].value += teams.get(i).getAvg(FieldsConfig.telOp);
            }
            if (penaltyCheck.isChecked()) {
                ranks[i].value += teams.get(i).getAvg(FieldsConfig.penalty);
            }
        }

        // Sort the teams by their score
        Arrays.sort(ranks, (o1, o2) -> (int) (100 * (o2.value - o1.value)));

        // Reset layout before constructing it
        runOnUiThread(() -> teamsLayout.removeAllViews());

        // Add teams one by one, in their order since the array is sorted
        for (int i = 0; i < ranks.length; i++) {
            TeamScore rank = ranks[i];
            final TextView team = new TextView(getContext());
            team.setText(String.format(getString(R.string.rank_format), i + 1, unFireKey(rank.key), rank.value));
            team.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            team.setTextSize(20);
            runOnUiThread(() -> teamsLayout.addView(team));
        }
    }

    // Class to hold the teams and their score
    private static class TeamScore {
        public final String key;
        public float value;

        public TeamScore(String key, float value) {
            this.key = key;
            this.value = value;
        }
    }
}