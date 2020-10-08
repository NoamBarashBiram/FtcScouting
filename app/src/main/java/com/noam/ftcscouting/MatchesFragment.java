package com.noam.ftcscouting;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import com.noam.ftcscouting.MatchesActivity.Kind;
import com.noam.ftcscouting.database.FieldsConfig;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.utils.Toaster;

public class MatchesFragment extends Fragment {

    private int matchIndex = 0, matchesLen;
    private volatile boolean holdsLock = false;
    private Kind kindNow = Kind.Auto;
    private LinearLayout rootView;
    private String event, team;

    public MatchesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = (LinearLayout) inflater.inflate(R.layout.fragment_matches, container, false);
        return rootView;
    }

    public void init(String event, String team, int matchIndex, boolean holdsLock, int matchesLen) {
        this.event = event;
        this.team = team;
        this.matchIndex = matchIndex;
        this.holdsLock = holdsLock;
        this.matchesLen = matchesLen;
    }

    public void updateUI() {
        runOnUiThread(() -> rootView.removeAllViews());  // clear view before UI construction
        TextView title;
        View dataView;
        ConstraintSet constraintSet;
        for (FieldsConfig.Field field : FirebaseHandler.configuration.fields(kindNow.val)) {
            final ConstraintLayout fieldLayout = new ConstraintLayout(getContext());
            constraintSet = null;
            dataView = null;
            title = new TextView(getContext());
            title.setText(field.name + ":");
            title.setTextSize(18);
            title.setGravity(Gravity.CENTER_VERTICAL);

            switch (field.type) {
                case TITLE:
                    // A title is bold and with underline. It also requires some space, above and below
                    title.setTypeface(null, Typeface.BOLD);
                    title.setPaintFlags(title.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    fieldLayout.setPaddingRelative(0, 16, 0, 8);
                    break;
                case STRING:
                    // A string has an EditText to hold it's value
                    EditText edit = new EditText(getContext());
                    edit.setSingleLine();

                    edit.setText(getValue(field));

                    title.setId(View.generateViewId());
                    edit.setId(View.generateViewId());

                    // constrain the EditText to start at the end of @var title and end at parent's end
                    constraintSet = new ConstraintSet();
                    constraintSet.connect(edit.getId(), ConstraintSet.START, title.getId(), ConstraintSet.END, 16);
                    constraintSet.connect(edit.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                    // constrain @var title to has the same height as @var edit
                    constraintSet.connect(title.getId(), ConstraintSet.TOP, edit.getId(), ConstraintSet.TOP);
                    constraintSet.connect(title.getId(), ConstraintSet.BOTTOM, edit.getId(), ConstraintSet.BOTTOM);

                    // the previous constraining caused both title and edit to have 0 height and width
                    // which corresponds to MATCH_CONSTRAINT but when both are dependent on each other,
                    // title has 0 width and height and edit has 0 height
                    constraintSet.constrainHeight(edit.getId(), ConstraintSet.WRAP_CONTENT);
                    constraintSet.constrainWidth(title.getId(), ConstraintSet.WRAP_CONTENT);

                    dataView = edit;
                case BOOLEAN:

            }

            fieldLayout.addView(title);
            if (dataView != null) {
                fieldLayout.addView(dataView);
            }

            if (constraintSet != null) {
                constraintSet.applyTo(fieldLayout);
            }
            runOnUiThread(() -> rootView.addView(fieldLayout));

        }
    }

    private String getValue(FieldsConfig.Field field) {
        try {
            return FirebaseHandler.snapshot
                    .child("Events")
                    .child(event)
                    .child(team)
                    .child(kindNow.val)
                    .child(field.name)
                    .getValue(String.class)
                    .split(";")
                    [matchIndex];
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void runOnUiThread(Runnable runnable) {
        getActivity().runOnUiThread(runnable);
    }

    public void setHoldsLock(boolean holdsLock) {
        this.holdsLock = holdsLock;
        updateUI();
    }

    public void setMatchIndex(int matchIndex) {
        this.matchIndex = matchIndex;
    }

    public void setKind(Kind newKind) {
        this.kindNow = newKind;
    }
}