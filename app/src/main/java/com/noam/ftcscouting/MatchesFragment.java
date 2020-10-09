package com.noam.ftcscouting;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.internal.StringResourceValueReader;
import com.noam.ftcscouting.MatchesActivity.Kind;
import com.noam.ftcscouting.database.FieldsConfig;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.ui.views.DependableCheckBox;
import com.noam.ftcscouting.ui.views.HorizontalNumberPicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MatchesFragment extends Fragment {

    public static final String TAG = "MatchesFragment";

    private int matchIndex = 0, matchesLen;
    private ArrayList<Pair<String, ? extends View>> fieldObjects = new ArrayList<>();
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
        fieldObjects.clear();
        runOnUiThread(() -> rootView.removeAllViews());  // clear view before UI construction
        TextView title;
        View dataView;
        ConstraintSet constraintSet;
        for (FieldsConfig.Field field : FirebaseHandler.configuration.fields(kindNow.val)) {
            final ConstraintLayout fieldLayout = new ConstraintLayout(getContext());
            dataView = null;
            title = new TextView(getContext());
            title.setText(field.name + ":");
            title.setTextSize(18);
            title.setGravity(Gravity.CENTER_VERTICAL);

            title.setEnabled(holdsLock);

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

                    edit.setText(getValue(field.name));

                    dataView = edit;
                    break;
                case BOOLEAN:
                    DependableCheckBox check = new DependableCheckBox(getContext());

                    check.setChecked(getValue(field.name).equals("1"));

                    dataView = check;
                    break;
                case CHOICE:
                    Spinner spinner = new Spinner(getContext());
                    spinner.setAdapter(new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            field.get(FieldsConfig.Field.entries).split(",")
                    ));

                    int itemIndex = 0;
                    try {
                        itemIndex = Integer.parseInt(getValue(field.name));
                    } catch (NumberFormatException | NullPointerException ignored) {
                    }

                    spinner.setSelection(itemIndex);
                    dataView = spinner;
                    break;
                case INTEGER:
                    HorizontalNumberPicker picker = new HorizontalNumberPicker(getContext());
                    picker.setMaxValue(field.get(FieldsConfig.Field.max));
                    picker.setMinValue(field.get(FieldsConfig.Field.min));
                    picker.setValue(getValue(field.name));
                    dataView = picker;
            }

            fieldLayout.addView(title);
            if (dataView != null) {
                dataView.setEnabled(holdsLock);

                title.setId(View.generateViewId());
                dataView.setId(View.generateViewId());

                // constrain the data view to start at the end of @var title and end at parent's end
                constraintSet = new ConstraintSet();
                constraintSet.connect(dataView.getId(), ConstraintSet.START, title.getId(), ConstraintSet.END, 16);
                constraintSet.connect(dataView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                // constrain @var title to has the same height as @var dataView
                constraintSet.connect(title.getId(), ConstraintSet.TOP, dataView.getId(), ConstraintSet.TOP);
                constraintSet.connect(title.getId(), ConstraintSet.BOTTOM, dataView.getId(), ConstraintSet.BOTTOM);

                // the previous constraining caused both title and check to have 0 height and width
                // which corresponds to MATCH_CONSTRAINT but when both are dependent on each other,
                // title has 0 width and height and dataView has 0 height
                constraintSet.constrainHeight(dataView.getId(), ConstraintSet.WRAP_CONTENT);
                constraintSet.constrainWidth(title.getId(), ConstraintSet.WRAP_CONTENT);

                fieldLayout.addView(dataView);

                constraintSet.applyTo(fieldLayout);

                fieldObjects.add(new Pair<>(field.name, dataView));
            }
            runOnUiThread(() -> rootView.addView(fieldLayout));

        }
    }

    private String getValue(String field) {
        try {
            return getValues(field)[matchIndex];
        } catch (NullPointerException | IndexOutOfBoundsException ignored) {
        } // happens on Strings when the last matches are empty
        return "";
    }

    public Map<String, Object> getChanges() {
        HashMap<String, Object> changes = new HashMap<>();
        boolean isDifferent = false;
        for (Pair<String, ? extends View> field : fieldObjects) {
            String[] oldValues = getValues(field.first);
            String[] values = oldValues.clone();
            if (values.length != matchesLen) {
                values = Arrays.copyOf(values, matchesLen);
                for (int i = 0; i < matchesLen; i++) {
                    if (values[i] == null)
                        values[i] = "";
                }
            }
            values[matchIndex] = getValue(field.second);
            String newVal = TextUtils.join(";", values);
            changes.put(field.first, newVal);
            isDifferent |= !newVal.equals(TextUtils.join(";", oldValues));
        }
        return isDifferent ? changes : null;
    }

    private String getValue(View dataView) {
        if (dataView instanceof EditText){
            return ((EditText) dataView).getText().toString();
        }
        if (dataView instanceof DependableCheckBox){
            return ((DependableCheckBox) dataView).isChecked() ? "1" : "0";
        }
        if (dataView instanceof Spinner){
            return String.valueOf(((Spinner) dataView).getSelectedItemPosition());
        }
        if (dataView instanceof HorizontalNumberPicker){
            return String.valueOf(((HorizontalNumberPicker) dataView).getValue());
        }
        return null;
    }

    private String[] getValues(String field) {
        return FirebaseHandler.snapshot
                .child("Events")
                .child(event)
                .child(team)
                .child(kindNow.val)
                .child(field)
                .getValue(String.class)
                .split(";");
    }

    private void runOnUiThread(Runnable runnable) {
        getActivity().runOnUiThread(runnable);
    }

    public void setHoldsLock(boolean holdsLock) {
        this.holdsLock = holdsLock;
    }

    public void setMatchIndex(int matchIndex) {
        this.matchIndex = matchIndex;
    }

    public void setKind(Kind newKind) {
        this.kindNow = newKind;
    }
}