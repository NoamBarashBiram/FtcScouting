package com.noam.ftcscouting.ui.teams;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.noam.ftcscouting.R;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.MatchesActivity;
import com.noam.ftcscouting.utils.StaticSync;

import java.util.ArrayList;

import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_EVENT;
import static com.noam.ftcscouting.alarm.AlarmReceiver.EXTRA_TEAM;
import static com.noam.ftcscouting.database.FirebaseHandler.fireKey;
import static com.noam.ftcscouting.database.FirebaseHandler.unFireKey;


public class TeamsFragment extends Fragment implements StaticSync.Notifiable, TextWatcher {

    public static final String eventString = "Events";
    private static final int BTN_HEIGHT = 200, BTN_TXT_SIZE = 18;
    private String event; 
    public static ArrayList<String> teams = null;
    private LinearLayout rightColumn, leftColumn;
    private EditText searchView;
    private String filter = ".*";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_teams, container, false);
        rightColumn = root.findViewById(R.id.rightColumn);
        leftColumn = root.findViewById(R.id.leftColumn);
        searchView = root.findViewById(R.id.search);
        searchView.addTextChangedListener(this);
        return root;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StaticSync.register(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (FirebaseHandler.snapshot != null) {
            teams = new ArrayList<>();
            for (DataSnapshot team : FirebaseHandler.snapshot.child(eventString).child(event).getChildren()) {
                teams.add(team.getKey());
            }
            updateUI();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        event = getActivity().getIntent().getStringExtra(EXTRA_EVENT);
        getActivity().setTitle(unFireKey(event));
    }

    @Override
    public void onNotified(Object message) {
        if (message instanceof ArrayList) {
            if (teams == null) {
                teams = new ArrayList<>();
                for (DataSnapshot team : FirebaseHandler.snapshot.child(eventString).child(event).getChildren()) {
                    teams.add(team.getKey());
                }
                updateUI();
            }
            ArrayList<String> realMessage = (ArrayList<String>) message;
            if (eventString.equals(realMessage.get(0)) && realMessage.size() == 4 && event.equals(realMessage.get(1))) {
                if (FirebaseHandler.ADD.equals(realMessage.get(3))) {
                    teams.add(realMessage.get(2));
                } else if (FirebaseHandler.DEL.equals(realMessage.get(3))) {
                    teams.remove(realMessage.get(2));
                } else
                    return;
                updateUI();
            }
        }
    }

    private void updateUI() {
        getActivity().runOnUiThread(() -> {
            rightColumn.removeAllViews();
            leftColumn.removeAllViews();
            ArrayList<String> tempTeams = new ArrayList<>();
            for (String team : teams) {
                if (team.toLowerCase().matches(filter))
                    tempTeams.add(team);
            }
            for (int i = 0; i < tempTeams.size(); i += 2) {
                Button btn = new Button(getContext());
                btn.setText(unFireKey(tempTeams.get(i)));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                BTN_HEIGHT
                        );
                params.setMargins(32, 16, 32, 16);
                btn.setLayoutParams(params);
                btn.setTextSize(BTN_TXT_SIZE);
                int finalI = i;
                btn.setOnClickListener(view -> openMatches(teams.get(finalI)));
                leftColumn.addView(btn);
            }
            for (int i = 1; i < tempTeams.size(); i += 2) {
                Button btn = new Button(getContext());
                btn.setText(unFireKey(tempTeams.get(i)));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                BTN_HEIGHT
                        );
                params.setMargins(32, 16, 32, 16);
                btn.setLayoutParams(params);
                btn.setTextSize(BTN_TXT_SIZE);
                int finalI = i;
                btn.setOnClickListener(view -> openMatches(teams.get(finalI)));
                rightColumn.addView(btn);
            }

            if ((tempTeams.size() & 1) == 1) {
                View spacer = new View(getContext());
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                BTN_HEIGHT
                        );
                params.setMargins(32, 16, 32, 16);
                spacer.setLayoutParams(params);
                rightColumn.addView(spacer);
            }
        });
    }

    private void openMatches(String teamName) {
        Intent intent = new Intent(getContext(), MatchesActivity.class);
        intent.putExtra(EXTRA_EVENT, event);
        intent.putExtra(EXTRA_TEAM, teamName);
        startActivity(intent);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        filter = ".*" +
                searchView.getText()
                        .toString()
                        .toLowerCase()
                        .replace("*", "\\*")
                        .replace(".", "\\.")
                        .replace("[", "\\[")
                        .replace("]", "\\]")
                        .replace("{", "\\{")
                        .replace("}", "\\}") +
                ".*";
        updateUI();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}