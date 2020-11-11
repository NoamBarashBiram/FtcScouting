package com.noam.ftcscouting.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.noam.ftcscouting.R;
import com.noam.ftcscouting.database.FieldsConfig;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.database.ScoreCalculator;
import com.noam.ftcscouting.ui.events.EventsFragment;
import com.noam.ftcscouting.ui.views.GraphView;

public class StatsFragment extends Fragment {

    public static final String TAG = "StatsFragment";

    private CheckBox autoCheck, telOpCheck, penaltyCheck;

    private GraphView graph;
    private int matches;
    private ScoreCalculator calculator;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_stats, container, false);
        graph = root.findViewById(R.id.graph);
        graph.setStrokeWidth(5);
        graph.setTextSize(40);
        graph.setRadius(7);
        graph.setYName("Total Score");

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String teamName = FirebaseHandler.snapshot.child(EventsFragment.eventsString)
                .child(FirebaseHandler.selfScoringEventName)
                .getChildren()
                .iterator()
                .next()
                .getKey();

        calculator = new ScoreCalculator(FirebaseHandler.selfScoringEventName, teamName);

        matches = FirebaseHandler.snapshot.child(EventsFragment.eventsString)
                .child(calculator.event)
                .child(calculator.team)
                .child(FieldsConfig.matches)
                .getValue(String.class)
                .split(";")
                .length;

        new Thread(this::updateUI).start();
    }

    private void updateUI() {
        getActivity().runOnUiThread(() -> {
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
        });

        float[] data = new float[matches];
        for (int i = 0; i < matches; i++){
            if (autoCheck.isChecked())
                data[i] += calculator.getScore(FieldsConfig.auto, i);
            if (telOpCheck.isChecked())
                data[i] += calculator.getScore(FieldsConfig.telOp, i);
            if (penaltyCheck.isChecked())
                data[i] += calculator.getScore(FieldsConfig.penalty, i);
        }
        graph.clear();
        graph.addData(data);
    }
}