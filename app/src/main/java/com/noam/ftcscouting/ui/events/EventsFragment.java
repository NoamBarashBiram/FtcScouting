package com.noam.ftcscouting.ui.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.noam.ftcscouting.EventActivity;
import com.noam.ftcscouting.R;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.ui.teams.TeamsFragment;
import com.noam.ftcscouting.utils.StaticSync;

import java.util.ArrayList;

import static com.noam.ftcscouting.database.FirebaseHandler.unFireKey;


public class EventsFragment extends Fragment implements StaticSync.Notifiable {

    private static final String eventsString = "Events";
    private ArrayList<String> events = null;
    private View root, loading;
    private LinearLayout rightColumn, leftColumn;

    private static final int BTN_HEIGHT = 200, BTN_TXT_SIZE = 18;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_events, container, false);
        rightColumn = root.findViewById(R.id.rightColumn);
        leftColumn = root.findViewById(R.id.leftColumn);
        loading = root.findViewById(R.id.loading);
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
            events = new ArrayList<>();
            for (DataSnapshot event : FirebaseHandler.snapshot.child(eventsString).getChildren()) {
                events.add(event.getKey());
            }
            updateUI();
        }
    }

    @Override
    public void onNotified(Object message) {
        if (message instanceof ArrayList) {
            if (events == null) {
                events = new ArrayList<>();
                for (DataSnapshot event : FirebaseHandler.snapshot.child(eventsString).getChildren()) {
                    events.add(event.getKey());
                }
                updateUI();
            }
            ArrayList<String> realMessage = (ArrayList<String>) message;
            if (eventsString.equals(realMessage.get(0)) && realMessage.size() == 3) {
                if (FirebaseHandler.ADD.equals(realMessage.get(2))) {
                    events.add(realMessage.get(1));
                } else if (FirebaseHandler.DEL.equals(realMessage.get(2))) {
                    events.remove(realMessage.get(1));
                } else
                    return;
                updateUI();
            }
        }
    }

    private void openEvent(View v){
        Button btn = (Button) v;
        Intent i = new Intent(getContext(), EventActivity.class);
        i.putExtra(TeamsFragment.EXTRA_EVENT, FirebaseHandler.unFireKey(btn.getText()));
        startActivity(i);
    }

    private void updateUI() {
        getActivity().runOnUiThread(() -> {
            loading.setVisibility(View.GONE);
            rightColumn.removeAllViews();
            leftColumn.removeAllViews();
            for (int i = 0; i < events.size(); i += 2) {
                Button btn = new Button(getContext());
                btn.setText(unFireKey((events.get(i))));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                BTN_HEIGHT
                        );
                params.setMargins(32, 16, 32, 16);
                btn.setLayoutParams(params);
                btn.setTextSize(BTN_TXT_SIZE);
                btn.setOnClickListener(this::openEvent);
                leftColumn.addView(btn);
            }
            for (int i = 1; i < events.size(); i += 2) {
                Button btn = new Button(getContext());
                btn.setText(unFireKey(events.get(i)));
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                BTN_HEIGHT
                        );
                params.setMargins(32, 16, 32, 16);
                btn.setLayoutParams(params);
                btn.setTextSize(BTN_TXT_SIZE);
                btn.setOnClickListener(this::openEvent);
                rightColumn.addView(btn);
            }
        });
    }
}