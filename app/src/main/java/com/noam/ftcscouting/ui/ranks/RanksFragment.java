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

        teamsLayout = root.findViewById(R.id.teams);

        autoCheck = root.findViewById(R.id.autoCheck);
        telOpCheck = root.findViewById(R.id.telOpCheck);
        penaltyCheck = root.findViewById(R.id.penaltyCheck);

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


        autoCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateUI());
        telOpCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateUI());
        penaltyCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateUI());

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        event = getActivity().getIntent().getStringExtra(EXTRA_EVENT);
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
        // make sure at least one checkbox is
        autoCheck.setEnabled(true);
        telOpCheck.setEnabled(true);
        penaltyCheck.setEnabled(true);

        if (!autoCheck.isChecked()) {
            if (!telOpCheck.isChecked()) {
                penaltyCheck.setEnabled(false);
            } else if (!penaltyCheck.isChecked()) {
                telOpCheck.setEnabled(false);
            }
        } else if (!telOpCheck.isChecked() && !penaltyCheck.isChecked()) {
            autoCheck.setEnabled(false);
        }

        Node<Float>[] ranks = new Node[teams.size()];
        for (int i = 0; i < ranks.length; i++) {
            ranks[i] = new Node<>(teams.get(i).team, 0f);
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

        Arrays.sort(ranks, (o1, o2) -> (int) (100 * (o2.value - o1.value)));

        runOnUiThread(() -> teamsLayout.removeAllViews());

        for (int i = 0; i < ranks.length; i++) {
            Node<Float> rank = ranks[i];
            final TextView team = new TextView(getContext());
            team.setText(String.format(getString(R.string.rank_format), i + 1, unFireKey(rank.key), rank.value));
            team.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            team.setTextSize(22);
            runOnUiThread(() -> teamsLayout.addView(team));
        }
    }

    private static class Node<V> {
        public final String key;
        public V value;

        public Node(String key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}