package com.noam.ftcscouting.ui.events;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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


public class EventsFragment extends Fragment implements StaticSync.Notifiable, TextWatcher {

    public static final String eventsString = "Events", TAG = "EventsFragment";
    private ArrayList<String> events = null;
    private FrameLayout loading;
    private LinearLayout rightColumn, leftColumn;

    private static final int BTN_HEIGHT = 200, BTN_TXT_SIZE = 18;
    private EditText searchView;
    private String filter = ".*";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_events, container, false);
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
            events = new ArrayList<>();
            for (DataSnapshot event : FirebaseHandler.snapshot.child(eventsString).getChildren()) {
                String eventName = event.getKey();
                if (!eventName.equals(FirebaseHandler.selfScoringEventName)) {
                    events.add(eventName);
                }
            }
            updateUI();
        }
    }

    @Override
    public void onNotified(Object message) {
        if (message instanceof ArrayList) {
            if (events == null) {
                loading = getActivity().findViewById(R.id.loading);
                events = new ArrayList<>();
                for (DataSnapshot event : FirebaseHandler.snapshot.child(eventsString).getChildren()) {
                    String eventName = event.getKey();
                    if (!eventName.equals(FirebaseHandler.selfScoringEventName) && !events.contains(eventName)) {
                        events.add(eventName);
                    }
                }
                updateUI();
            }
            ArrayList<String> realMessage = (ArrayList<String>) message;
            if (eventsString.equals(realMessage.get(0)) && realMessage.size() == 3) {
                if (FirebaseHandler.ADD.equals(realMessage.get(2))) {
                    String eventName = realMessage.get(1);
                    if (!eventName.equals(FirebaseHandler.selfScoringEventName) && !events.contains(eventName)) {
                        events.add(eventName);
                    }
                } else if (FirebaseHandler.DEL.equals(realMessage.get(2))) {
                    events.remove(realMessage.get(1));
                } else
                    return;
                updateUI();
            }
        }
    }

    private void openEvent(View v) {
        Button btn = (Button) v;
        Intent i = new Intent(getContext(), EventActivity.class);
        i.putExtra(TeamsFragment.EXTRA_EVENT, FirebaseHandler.unFireKey(btn.getText()));
        startActivity(i);
    }

    private void updateUI() {
        getActivity().runOnUiThread(() -> {
            if (loading != null)
                loading.setVisibility(View.GONE);
            searchView.setEnabled(true);
            rightColumn.removeAllViews();
            leftColumn.removeAllViews();
            ArrayList<String> tempEvents = new ArrayList<>();
            for (String event : events) {
                if (event.toLowerCase().matches(filter))
                    tempEvents.add(event);
            }
            for (int i = 0; i < tempEvents.size(); i += 2) {
                Button btn = new Button(getContext());
                btn.setText(unFireKey((tempEvents.get(i))));
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
            for (int i = 1; i < tempEvents.size(); i += 2) {
                Button btn = new Button(getContext());
                btn.setText(unFireKey(tempEvents.get(i)));
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
            if ((tempEvents.size() & 1) == 1) {
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

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        filter = ".*" + searchView.getText().toString().toLowerCase() + ".*";
        updateUI();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}