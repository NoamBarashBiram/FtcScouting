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
import android.widget.TableLayout;
import android.widget.TableRow;
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
                put(FieldsConfig.teleOp, new ArrayList<>());
                put(FieldsConfig.penalty, new ArrayList<>());
            }};
    private volatile boolean enabled = false;
    private LinearLayout matchLayout;
    private TableLayout fieldTable;
    private String event, team;
    private OnScoreChangeListener listener;
    private ScoreCalculator calc;

    public int autoScore, teleOpScore;
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
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.fragment_matches, container, false);
        fieldTable = root.findViewById(R.id.fieldTable);
        matchLayout = root.findViewById(R.id.matchLayout);
        return root;
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
        runOnUiThread(() -> {
            matchLayout.setVisibility(View.GONE);
            fieldTable.setVisibility(View.VISIBLE);
            fieldTable.removeAllViews();
        });
        // replace viewed view and clear it before UI construction

        TableRow titleRow = new TableRow(getContext());
        titleRow.setBackgroundColor(Color.BLACK);
        titleRow.setLayoutParams(
                new TableLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1)
        );

        TextView[] rowViews = new TextView[3];
        rowViews[0] = new TextView(getContext());
        rowViews[1] = new TextView(getContext());
        rowViews[2] = new TextView(getContext());

        rowViews[0].setText(R.string.field_name);
        rowViews[1].setText(R.string.avg_val);
        rowViews[2].setText(R.string.avg_score);

        for (int i = 0; i < 3; i++) {
            TableRow.LayoutParams params =
                    new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins(1, 1, i == 2 ? 2 : 1, 1);
            rowViews[i].setLayoutParams(params);
            rowViews[i].setPaddingRelative(20, 20, 20, 20);
            rowViews[i].setTextSize(20);
            rowViews[i].setTypeface(null, Typeface.BOLD);
            rowViews[i].setGravity(Gravity.CENTER_VERTICAL);
            rowViews[i].setTextColor(Color.BLACK);
            rowViews[i].setBackgroundColor(0xFFFAFAFA);
            titleRow.addView(rowViews[i]);
        }

        runOnUiThread(() -> fieldTable.addView(titleRow));

        for (String kind : FieldsConfig.kinds) {
            TableRow kindRow = new TableRow(getContext());

            kindRow.setBackgroundColor(Color.BLACK);
            kindRow.setLayoutParams(
                    new TableLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1)
            );

            rowViews[0] = new TextView(getContext());
            rowViews[1] = new TextView(getContext());
            rowViews[2] = new TextView(getContext());

            rowViews[0].setText(kind);

            for (int i = 0; i < 3; i++) {
                TableRow.LayoutParams params =
                        new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);
                params.setMargins(1, 1, i == 2 ? 2 : 1, 1);
                rowViews[i].setLayoutParams(params);
                rowViews[i].setPaddingRelative(20, 20, 20, 20);
                rowViews[i].setTextSize(22);
                rowViews[i].setTypeface(null, Typeface.BOLD);
                rowViews[i].setGravity(Gravity.CENTER_VERTICAL);
                rowViews[i].setTextColor(Color.BLACK);
                rowViews[i].setBackgroundColor(0xFFFAFAFA);
                kindRow.addView(rowViews[i]);
            }
            TextView avgKind = rowViews[2];

            runOnUiThread(() -> fieldTable.addView(kindRow));

            float totalKind = 0;

            for (FieldsConfig.Field field : FirebaseHandler.configuration.fields(kind)) {
                if (!scorableTypes.contains(field.type))
                    continue;

                final TableRow row = new TableRow(getContext());
                row.setBackgroundColor(Color.BLACK);

                rowViews = new TextView[3];
                rowViews[0] = new TextView(getContext());
                rowViews[1] = new TextView(getContext());
                rowViews[2] = new TextView(getContext());

                for (int i = 0; i < 3; i++) {
                    TableRow.LayoutParams params =
                            new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT);
                    params.setMargins(1, 1, i == 2 ? 2 : 1, 1);
                    rowViews[i].setLayoutParams(params);
                    rowViews[i].setPaddingRelative(20, 20, 20, 20);
                    rowViews[i].setTextSize(18);
                    rowViews[i].setTypeface(null, Typeface.BOLD);
                    rowViews[i].setGravity(Gravity.CENTER_VERTICAL);
                    rowViews[i].setTextColor(Color.BLACK);
                    rowViews[i].setBackgroundColor(0xFFFAFAFA);
                    row.addView(rowViews[i]);
                }

                rowViews[0].setTextSize(18);
                rowViews[0].setText(field.name);
                rowViews[0].setGravity(Gravity.CENTER_VERTICAL);
                rowViews[0].setTextColor(Color.BLACK);
                String avgVal = "", avgScore = "";

                if (field.type == FieldsConfig.Field.Type.BOOLEAN) {
                    float[] avg = calc.getAvg(kind, field.name);
                    avgVal = String.format("%d/%d", (int) (avg[0] * avg[2]), (int) avg[2]);
                    avgScore = String.format("%.2f", avg[1]);
                    totalKind += avg[1];
                } else if (field.type == FieldsConfig.Field.Type.INTEGER) {
                    float[] avg = calc.getAvg(kind, field.name);
                    avgVal = String.format("%.2f", avg[0]);
                    avgScore = String.format("%.2f", avg[1]);
                    totalKind += avg[1];
                } else if (field.type == FieldsConfig.Field.Type.TITLE) {
                    rowViews[0].setTypeface(null, Typeface.BOLD);
                    rowViews[0].setTextSize(20);
                }

                rowViews[0].setEnabled(enabled);
                rowViews[1].setText(avgVal);
                rowViews[2].setText(avgScore);
                runOnUiThread(() -> fieldTable.addView(row));

            }

            float finalTotalKind = totalKind;
            runOnUiThread(() -> avgKind.setText(String.format("%.2f", finalTotalKind)));
        }
    }

    public void updateUI() {
        runOnUiThread(() -> {
            matchLayout.setVisibility(View.VISIBLE);
            fieldTable.setVisibility(View.GONE);
        });
        if (!constructedUI) {
            constructUI();
            constructedUI = true;
        } else {
            for (String kind : FieldsConfig.kinds) {
                for (int i = 0; i < matchLayout.getChildCount(); i++) {
                    View child = matchLayout.getChildAt(i);
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
                        final boolean checked = getValue(kind, pair.first).equals("1");
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
        runOnUiThread(() -> matchLayout.removeAllViews());  // clear view before UI construction
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

            runOnUiThread(() -> matchLayout.addView(kindView));

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
                runOnUiThread(() -> matchLayout.addView(fieldLayout));
            }

            for (Pair<String, ? extends View> field : fieldObjects.get(kind)) {
                if (FirebaseHandler.configuration.getField(kind, field.first).type ==
                        FieldsConfig.Field.Type.BOOLEAN) {
                    for (FieldsConfig.DependencyRule rule : FirebaseHandler.configuration.dependencies.get(kind)) {
                        if (rule.parent.equals(field.first)) {
                            for (Pair<String, ? extends View> possibleDependent : fieldObjects.get(kind)) {
                                if (possibleDependent.first.equals(rule.dependent)) {
                                    runOnUiThread(() ->
                                            ((DependableCheckBox) field.second)
                                                    .addDependency(possibleDependent.second, rule.mode));
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
            put(FieldsConfig.teleOp, 0);
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
        teleOpScore = scores.get(FieldsConfig.teleOp);

        if (listener != null)
            listener.onScoreChanged(autoScore, teleOpScore);
    }

    private String getValue(String kind, String field) {
        try {
            return getValues(kind, field)[matchIndex];
        } catch (NullPointerException | IndexOutOfBoundsException ignored) {
        } // happens on Strings when the last matches are empty
        return "";
    }

    public Map<String, Object> getChanges(@FieldsConfig.FieldKind String kind) {
        if (matchIndex >= matchesLen) return null;
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
            if (parent == null) {
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
        void onScoreChanged(int autoScore, int teleOpScore);
    }
}