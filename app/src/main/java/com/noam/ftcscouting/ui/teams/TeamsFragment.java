package com.noam.ftcscouting.ui.teams;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.noam.ftcscouting.R;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.utils.StaticSync;

import java.util.ArrayList;

import static com.noam.ftcscouting.database.FirebaseHandler.unFireKey;


public class TeamsFragment extends Fragment implements StaticSync.Notifiable {

    public static final String EXTRA_EVENT = "com.noam.ftcscouting.ui.myMatchesMyMatchesFragment.EXTRA_EVENT";
    private static final String eventString = "Events";
    private static final int BTN_HEIGHT = 200, BTN_TXT_SIZE = 18;
    private String event;
    private ArrayList<String> teams = null;
    private View root;
    private LinearLayout rightColumn, leftColumn;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_teams, container, false);
        rightColumn = root.findViewById(R.id.rightColumn);
        leftColumn = root.findViewById(R.id.leftColumn);
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
            for (int i = 0; i < teams.size(); i += 2) {
                Button btn = new Button(getContext());
                btn.setText(unFireKey(teams.get(i)));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                BTN_HEIGHT
                        );
                params.setMargins(32, 16, 32, 16);
                btn.setLayoutParams(params);
                btn.setTextSize(BTN_TXT_SIZE);
                leftColumn.addView(btn);
            }
            for (int i = 1; i < teams.size(); i += 2) {
                Button btn = new Button(getContext());
                btn.setText(unFireKey(teams.get(i)));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                BTN_HEIGHT
                        );
                params.setMargins(32, 16, 32, 16);
                btn.setLayoutParams(params);
                btn.setTextSize(BTN_TXT_SIZE);
                rightColumn.addView(btn);
            }
        });
    }
}