package com.noam.ftcscouting.database;

import android.util.Log;
import android.util.Pair;

import com.google.firebase.database.DataSnapshot;

import java.util.Arrays;
import java.util.HashMap;

import static com.noam.ftcscouting.ui.events.EventsFragment.eventsString;

public class ScoreCalculator {

    private static final String TAG = "ScoreCalculator";

    private final String event;
    private final String team;

    public ScoreCalculator(String event, String team) {
        this.event = event;
        this.team = team;
    }

    public int getScore(String kind, int match) {
        int score = 0;
        DataSnapshot fields = FirebaseHandler.snapshot
                .child(eventsString)
                .child(event)
                .child(team)
                .child(kind);

        HashMap<String, Pair<Integer, Integer>> scores = new HashMap<>();

        for (DataSnapshot fieldSnap : fields.getChildren()) {
            FieldsConfig.Field field = FirebaseHandler.configuration
                    .getField(kind, fieldSnap.getKey());
            if (field.type == FieldsConfig.Field.Type.BOOLEAN ||
                    field.type == FieldsConfig.Field.Type.INTEGER) {
                try {
                    int fieldsScore = Integer.parseInt(field.get(FieldsConfig.Field.score));
                    int value = Integer.parseInt(getValue(kind, field.name, match));
                    scores.put(field.name, new Pair<>(fieldsScore, value));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "getScore: ", e);
                    scores.put(field.name, new Pair<>(0, 0));
                }
            }
        }

        for (FieldsConfig.DependencyRule rule : FirebaseHandler.configuration.dependencies.get(kind)) {
            Pair<Integer, Integer> parent = scores.get(rule.parent);
            if (parent != null && ((parent.second == 1) != rule.mode)) {
                scores.remove(rule.dependent);
            }
        }

        for (Pair<Integer, Integer> values : scores.values()) {
            score += values.first * values.second;
        }
        return score;
    }

    public float getAvg(@FieldsConfig.FieldKind String kind) {
        float score = 0;
        int matches = getMatches();
        int playedMatches = matches;
        for (int match = 0; match < matches; match++) {
            if (!isPlayed(match)) {
                playedMatches--;
                continue;
            }
            score += getScore(kind, match);
        }
        return score / playedMatches;
    }

    public int getMatches() {
        String matches = FirebaseHandler.snapshot
                .child(eventsString)
                .child(event)
                .child(team)
                .child(FieldsConfig.matches)
                .getValue(String.class);
        return matches == null || matches.equals("") ? 0 : matches.split(";").length;
    }

    public float[] getAvg(@FieldsConfig.FieldKind String kind, String field) {
        return getAvg(kind, field, true);
    }

    public float[] getAvg(@FieldsConfig.FieldKind String kind, String field, boolean hasScoutingPit) {
        int score = 0;
        int totalValue = 0;

        int matchesToIgnore = (hasScoutingPit ? 1 : 0);

        int scorePerUnit = 0;
        try {
            scorePerUnit = Integer.parseInt(FirebaseHandler.configuration
                    .getField(kind, field)
                    .get(FieldsConfig.Field.score));
        } catch (NumberFormatException e) {
            Log.e(TAG, "getAvg: ", e);
        }

        boolean mode = true;
        String[] stringValues = getValues(kind, field);
        String[] parentValues = new String[stringValues.length];
        Arrays.fill(parentValues, null);

        for (FieldsConfig.DependencyRule rule : FirebaseHandler.configuration.dependencies.get(kind)) {
            if (rule.dependent.equals(field)) {
                parentValues = getValues(kind, rule.parent);
                mode = rule.mode;
            }
        }

        int matches = stringValues.length - matchesToIgnore;

        int playedMatches = matches;

        for (int i = 0; i < matches; i++) {
            if (!isPlayed(i)) {
                playedMatches--;
                continue;
            }
            try {
                int val = Integer.parseInt(stringValues[i]);
                int par = parentValues[i] == null ? 1 : Integer.parseInt(parentValues[i]);
                totalValue += val;
                if ((par == 1) == mode) {
                    score += val * scorePerUnit;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "getAvg: ", e);
            }
        }

        return new float[]{(float) totalValue / playedMatches, (float) score / playedMatches, playedMatches};
    }

    private String getValue(@FieldsConfig.FieldKind String kind, String field, int match) {
        try {
            return getValues(kind, field)[match];
        } catch (NullPointerException | IndexOutOfBoundsException ignored) {
        } // happens on Strings when the last matches are empty
        return "";
    }

    private boolean isPlayed(int match) {
        String[] notPlayed = FirebaseHandler.snapshot
                .child(eventsString)
                .child(event)
                .child(team)
                .child(FieldsConfig.unPlayed)
                .getValue(String.class)
                .split(";");

        String matchStr = String.valueOf(match);
        for (String matchName : notPlayed) {
            if (matchName.equals(matchStr)) {
                return false;
            }
        }
        return true;
    }

    private String[] getValues(@FieldsConfig.FieldKind String kind, String field) {
        return FirebaseHandler.snapshot
                .child(eventsString)
                .child(event)
                .child(team)
                .child(kind)
                .child(field)
                .getValue(String.class)
                .split(";");
    }
}
