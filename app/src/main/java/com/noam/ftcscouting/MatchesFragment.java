package com.noam.ftcscouting;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import com.noam.ftcscouting.database.FieldsConfig;
import com.noam.ftcscouting.database.FirebaseHandler;
import com.noam.ftcscouting.database.ScoreCalculator;
import com.noam.ftcscouting.ui.views.DependableCheckBox;
import com.noam.ftcscouting.ui.views.HorizontalNumberPicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.noam.ftcscouting.ui.events.EventsFragment.eventsString;

public class MatchesFragment extends Fragment {

    public static final String TAG = "MatchesFragment";

    private int matchIndex = 0, matchesLen;
    private final HashMap<String, ArrayList<Pair<String, ? extends View>>> fieldObjects =
            new HashMap<String, ArrayList<Pair<String, ? extends View>>>() {{
                put(FieldsConfig.auto, new ArrayList<>());
                put(FieldsConfig.telOp, new ArrayList<>());
                put(FieldsConfig.penalty, new ArrayList<>());
            }};
    private volatile boolean enabled = false;
    private LinearLayout rootView;
    private String event, team;
    private OnScoreChangeListener listener;
    private ScoreCalculator calc;

    public int autoScore, telOpScore;
    volatile boolean constructedUI = false;
    private final static List<FieldsConfig.Field.Type> scorableTypes =
            Arrays.asList(
                    FieldsConfig.Field.Type.BOOLEAN,
                    FieldsConfig.Field.Type.INTEGER,
                    FieldsConfig.Field.Type.TITLE
            );

    public MatchesFragment() {
        // Required empty public constructor
    }

    public void setOnScoreChangeListener(OnScoreChangeListener listener) {
        this.listener = listener;
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

    public void init(String event, String team, int matchIndex, boolean enabled, int matchesLen) {
        this.event = event;
        this.team = team;
        this.matchIndex = matchIndex;
        this.enabled = enabled;
        this.matchesLen = matchesLen;
        calc = new ScoreCalculator(event, team);
    }

    public void showAvg() {
        runOnUiThread(() -> rootView.removeAllViews());  // clear view before UI construction
        TextView title;
        TextView dataView;

        TextView example = new TextView(getContext());
        example.setTextSize(18);
        example.setText(R.string.avg_example);
        example.setGravity(Gravity.CENTER_VERTICAL);
        example.setTextColor(Color.BLACK);

        rootView.addView(example);

        for (String kind : FieldsConfig.kinds) {
            TextView kindView = new TextView(getContext());
            kindView.setTextSize(22);
            kindView.setTextColor(Color.BLACK);
            kindView.setTypeface(null, Typeface.BOLD);
            kindView.setPaddingRelative(0, 16, 0, 8);

            runOnUiThread(() -> rootView.addView(kindView));

            float totalKind = 0;

            for (FieldsConfig.Field field : FirebaseHandler.configuration.fields(kind)) {
                if (!scorableTypes.contains(field.type))
                    continue;

                final LinearLayout fieldLayout = new LinearLayout(getContext());
                fieldLayout.setOrientation(LinearLayout.HORIZONTAL);

                title = new TextView(getContext());
                title.setTextSize(18);
                title.setText(field.name + ":");
                title.setGravity(Gravity.CENTER_VERTICAL);
                title.setTextColor(Color.BLACK);
                dataView = null;

                if (field.type == FieldsConfig.Field.Type.BOOLEAN) {
                    float[] avg = calc.getAvg(kind, field.name);
                    TextView text = new TextView(getContext());
                    text.setText(String.format(getString(R.string.bool_avg_format), (int) (avg[0] * avg[2]), (int) avg[2], avg[1]));
                    dataView = text;
                    totalKind += avg[1];
                } else if (field.type == FieldsConfig.Field.Type.INTEGER) {
                    float[] avg = calc.getAvg(kind, field.name);
                    TextView text = new TextView(getContext());
                    text.setText(String.format(getString(R.string.int_avg_format), avg[0], avg[1]));
                    dataView = text;
                    totalKind += avg[1];
                } else if (field.type == FieldsConfig.Field.Type.TITLE) {
                    title.setTypeface(null, Typeface.BOLD);
                    title.setTextSize(20);
                    title.setPaddingRelative(0, 16, 0, 8);
                }

                title.setEnabled(enabled);
                fieldLayout.addView(title);
                if (dataView != null) {
                    dataView.setPaddingRelative(16, 0, 0, 0);
                    dataView.setTextSize(18);
                    dataView.setTextColor(Color.BLACK);
                    fieldLayout.addView(dataView);
                }
                runOnUiThread(() -> rootView.addView(fieldLayout));
            }

            kindView.setText(String.format(getString(R.string.avg_kind), kind, totalKind));
        }
        constructedUI = false;
    }

    public void updateUI() {
        if (!constructedUI) {
            constructUI();
            constructedUI = true;
        } else {
            for (String kind : FieldsConfig.kinds) {
                for (int i = 0; i < rootView.getChildCount(); i++) {
                    View child = rootView.getChildAt(i);
                    if (child instanceof TextView) {
                        runOnUiThread(() -> {
                            child.setEnabled(enabled);
                            ((TextView) child).setTextColor(enabled ? Color.BLACK : 0xffcccccc);
                        });
                    } else if (child instanceof ViewGroup) {
                        ViewGroup layout = (ViewGroup) child;
                        for (int i2 = 0; i2 < layout.getChildCount(); i2++) {
                            final int finalI = i2;
                            runOnUiThread(() -> layout.getChildAt(finalI).setEnabled(enabled));
                        }
                    }
                }
                for (Pair<String, ? extends View> pair : fieldObjects.get(kind)) {
                    if (pair.second instanceof HorizontalNumberPicker) {
                        runOnUiThread(() -> ((HorizontalNumberPicker) pair.second).setValue(getValue(kind, pair.first)));
                    } else if (pair.second instanceof DependableCheckBox) {
                        final boolean checked  = getValue(kind, pair.first).equals("1");
                        runOnUiThread(() -> ((DependableCheckBox) pair.second).setChecked(checked));
                    } else if (pair.second instanceof EditText) {
                        final String val = getValue(kind, pair.first);
                        runOnUiThread(() -> ((EditText) pair.second).setText(val));
                    } else if (pair.second instanceof Spinner) {
                        int entry = 0;
                        String val = getValue(kind, pair.first);
                        if (!val.equals("")) {
                            try {
                                entry = Integer.parseInt(val);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "updateUI: ", e);
                            }
                        }
                        final int finalEntry = entry;
                        runOnUiThread(() -> ((Spinner) pair.second).setSelection(finalEntry));
                    }
                }
            }
        }
    }

    private void constructUI() {
        runOnUiThread(() -> rootView.removeAllViews());  // clear view before UI construction
        TextView title;
        View dataView;
        ConstraintSet constraintSet;
        for (String kind : FieldsConfig.kinds) {
            fieldObjects.get(kind).clear();
            TextView kindView = new TextView(getContext());
            kindView.setText(kind);
            kindView.setTextSize(22);
            kindView.setGravity(Gravity.CENTER_VERTICAL);
            kindView.setTextColor(enabled ? Color.BLACK : 0xffcccccc);
            kindView.setEnabled(enabled);
            kindView.setTypeface(null, Typeface.BOLD);
            kindView.setPaddingRelative(0, 16, 0, 8);

            runOnUiThread(() -> rootView.addView(kindView));

            for (FieldsConfig.Field field : FirebaseHandler.configuration.fields(kind)) {
                final ConstraintLayout fieldLayout = new ConstraintLayout(getContext());
                dataView = null;
                title = new TextView(getContext());
                title.setText(field.name + ":");
                title.setTextSize(18);
                title.setGravity(Gravity.CENTER_VERTICAL);

                title.setEnabled(enabled);

                switch (field.type) {
                    case TITLE:
                        // A title is bold and with underline. It also requires some space, above and below
                        title.setTypeface(null, Typeface.BOLD);
                        title.setTextSize(20);
                        fieldLayout.setPaddingRelative(0, 16, 0, 8);
                        break;
                    case STRING:
                        // A string has an EditText to hold it's value
                        EditText edit = new EditText(getContext());
                        edit.setSingleLine();

                        edit.setText(getValue(kind, field.name));

                        dataView = edit;
                        break;
                    case BOOLEAN:
                        DependableCheckBox check = new DependableCheckBox(getContext());

                        check.setChecked(getValue(kind, field.name).equals("1"));

                        if (listener != null) {
                            check.setOnCheckedChangeListener((buttonView, isChecked) -> computeScore());
                        }
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
                            itemIndex = Integer.parseInt(getValue(kind, field.name));
                        } catch (NumberFormatException | NullPointerException ignored) {
                        }

                        spinner.setSelection(itemIndex);
                        dataView = spinner;
                        break;
                    case INTEGER:
                        HorizontalNumberPicker picker = new HorizontalNumberPicker(getContext());
                        // picker.enableLongClick(handler);
                        picker.setMaxValue(field.get(FieldsConfig.Field.max));
                        picker.setMinValue(field.get(FieldsConfig.Field.min));
                        picker.setValue(getValue(kind, field.name));
                        picker.setStep(field.get(FieldsConfig.Field.step));

                        if (listener != null) {
                            picker.setOnValueChangeListener((oldVal, newVal) -> computeScore());
                        }

                        dataView = picker;
                }

                fieldLayout.addView(title);
                if (dataView != null) {
                    dataView.setEnabled(enabled);

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

                    fieldObjects.get(kind).add(new Pair<>(field.name, dataView));
                }
                runOnUiThread(() -> rootView.addView(fieldLayout));
            }

            for (Pair<String, ? extends View> field : fieldObjects.get(kind)) {
                if (FirebaseHandler.configuration.getField(kind, field.first).type ==
                        FieldsConfig.Field.Type.BOOLEAN) {
                    for (FieldsConfig.DependencyRule rule : FirebaseHandler.configuration.dependencies.get(kind)) {
                        if (rule.parent.equals(field.first)) {
                            for (Pair<String, ? extends View> possibleDependent : fieldObjects.get(kind)) {
                                if (possibleDependent.first.equals(rule.dependent)) {
                                    ((DependableCheckBox) field.second)
                                            .addDependency(possibleDependent.second, rule.mode);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        computeScore();
    }

    private void computeScore() {
        Map<String, Integer> scores = new HashMap<String, Integer>() {{
            put(FieldsConfig.auto, 0);
            put(FieldsConfig.telOp, 0);
            put(FieldsConfig.penalty, 0);
        }};
        for (String kind : FieldsConfig.kinds) {
            for (Pair<String, ? extends View> pair : fieldObjects.get(kind)) {
                FieldsConfig.Field field = FirebaseHandler.configuration.getField(kind, pair.first);
                if (!(field.type == FieldsConfig.Field.Type.BOOLEAN ||
                        field.type == FieldsConfig.Field.Type.INTEGER))
                    continue;
                String dependency = field.get(FieldsConfig.Field.dependency);
                boolean disabled = false;
                if (dependency != null && !"".equals(dependency)) {
                    disabled = getValueNow(kind, dependency.substring(1)).equals("1") == (dependency.charAt(0) == '!');
                }
                if (!disabled) {
                    int score = Integer.parseInt(field.get(FieldsConfig.Field.score));
                    try {
                        score *= Integer.parseInt(getValue(pair.second));
                    } catch (NumberFormatException ignored) {
                        score *= 0;
                    }
                    scores.put(kind, scores.get(kind) + score);
                }
            }
        }

        autoScore = scores.get(FieldsConfig.auto);
        telOpScore = scores.get(FieldsConfig.telOp);

        if (listener != null)
            listener.onScoreChanged(autoScore, telOpScore);
    }

    private String getValue(String kind, String field) {
        try {
            return getValues(kind, field)[matchIndex];
        } catch (NullPointerException | IndexOutOfBoundsException ignored) {
        } // happens on Strings when the last matches are empty
        return "";
    }

    public Map<String, Object> getChanges(@FieldsConfig.FieldKind String kind) {
        HashMap<String, Object> changes = new HashMap<>();
        boolean isDifferent = false;
        for (Pair<String, ? extends View> field : fieldObjects.get(kind)) {
            String[] oldValues = getValues(kind, field.first);
            FieldsConfig.Field f =
                    FirebaseHandler.configuration.getField(kind, field.first);
            if (oldValues.length != matchesLen) {
                oldValues = Arrays.copyOf(oldValues, matchesLen);
                for (int i = 0; i < matchesLen; i++) {
                    if (oldValues[i] == null) {
                        switch (f.type) {
                            case CHOICE:
                            case BOOLEAN:
                                oldValues[i] = "0";
                                break;
                            case INTEGER:
                                oldValues[i] = f.get(FieldsConfig.Field.min);
                                break;
                            default:
                                oldValues[i] = "";
                        }
                    }
                }
            }
            String[] values = oldValues.clone();
            values[matchIndex] = getValue(field.second);
            String newVal = TextUtils.join(";", values);
            changes.put(field.first, newVal);
            isDifferent |= !newVal.equals(TextUtils.join(";", oldValues));
        }
        return isDifferent ? changes : null;
    }

    private String getValueNow(String kind, String fieldName) {
        View dataView = null;
        for (Pair<String, ? extends View> field : fieldObjects.get(kind)) {
            if (field.first.equals(fieldName)) {
                dataView = field.second;
                break;
            }
        }

        if (dataView == null)
            throw new IllegalArgumentException("Cannot find field " + fieldName + " from kind " + kind);

        return getValue(dataView);
    }

    private String getValue(View dataView) {
        if (dataView instanceof EditText) {
            return ((EditText) dataView).getText().toString();
        }
        if (dataView instanceof DependableCheckBox) {
            return ((DependableCheckBox) dataView).isChecked() ? "1" : "0";
        }
        if (dataView instanceof Spinner) {
            return String.valueOf(((Spinner) dataView).getSelectedItemPosition());
        }
        if (dataView instanceof HorizontalNumberPicker) {
            return String.valueOf(((HorizontalNumberPicker) dataView).getValue());
        }
        return null;
    }

    private String[] getValues(String kind, String field) {
        return FirebaseHandler.snapshot
                .child(eventsString)
                .child(event)
                .child(team)
                .child(kind)
                .child(field)
                .getValue(String.class)
                .split(";");
    }

    private void runOnUiThread(Runnable runnable) {
        mGetActivity().runOnUiThread(runnable);
    }

    private Activity mGetActivity() {
        Activity ret = getActivity();
        if (ret == null) {
            Fragment parent = getParentFragment();
            if (parent == null){
                return null;
            }
            return parent.getActivity();
        }
        return ret;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMatchIndex(int matchIndex) {
        this.matchIndex = matchIndex;
    }

    public interface OnScoreChangeListener {
        void onScoreChanged(int autoScore, int telOpScore);
    }
}