package com.noam.ftcscouting.database;

import android.util.Log;

import androidx.annotation.StringDef;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FieldsConfig {

    public static final String TAG = "FieldsConfig";
    public static final String auto = "Autonomous",
            teleOp = "TeleOp",
            penalty = "Penalty",
            type = "type",
            unPlayed = "unplayed",
            config = "config",
            matches = "matches",
            name = "name",
            placeholder = "PLACEHOLDER_DO_NOT_TOUCH";

    public static final String[] kinds = new String[]{auto, teleOp, penalty};

    @StringDef({auto, teleOp, penalty})
    public @interface FieldKind {
    }

    private final ArrayList<Field>
            autoFields = new ArrayList<>(),
            teleOpFields = new ArrayList<>(),
            penaltyFields = new ArrayList<>();

    public HashMap<String, ArrayList<DependencyRule>> dependencies = new HashMap<String, ArrayList<DependencyRule>>() {{
        put(auto, new ArrayList<>());
        put(teleOp, new ArrayList<>());
        put(penalty, new ArrayList<>());
    }};

    public static class Field {
        public static final String
                entries = "entries",
                max = "max",
                min = "min",
                score = "score",
                dependency = "dependency",
                step = "step";
        private final Map<String, String> configurations;
        public final String name;
        public final Type type;

        public enum Type {
            INTEGER("int"),
            STRING("str"),
            CHOICE("cho"),
            TITLE("tit"),
            BOOLEAN("bool"),
            UNKNOWN("???");


            private final String n;

            Type(String n) {
                this.n = n;
            }

            public static Type parse(String type) {

                for (Type t : values()) {
                    if (t != UNKNOWN && (t.getName().equals(type) || t.name().equals(type.toUpperCase()))) {
                        return t;
                    }
                }
                return UNKNOWN;
            }

            public String getName() {
                return n;
            }
        }

        public Field(String name, Type type, Map<String, String> configurations) {
            this.name = name;
            this.type = type;
            this.configurations = configurations;
        }

        public String get(String key) {
            if (configurations.containsKey(key)) {
                return configurations.get(key);
            }
            return null;
        }

        public boolean hasValue(String key) {
            return configurations.containsKey(key);
        }
    }

    private FieldsConfig() {
    }

    public static FieldsConfig readConfig(DataSnapshot configSnapshot) {
        FieldsConfig fieldsConfig = new FieldsConfig();
        if (!configSnapshot.exists()) {
            Log.e(TAG, "readConfig: Config does not exist");
            return null;
        }
        for (String fieldKind : kinds) {
            if (!configSnapshot.hasChild(fieldKind)) {
                Log.e(TAG, "readConfig: No Kind " + fieldKind);
                return null;
            }
            DataSnapshot child = configSnapshot.child(fieldKind);
            if (!child.hasChild(placeholder)) {
                Log.e(TAG, "readConfig: No Placeholder for " + fieldKind);
                return null;
            }
            for (DataSnapshot fieldSnapshot : configSnapshot.child(fieldKind).getChildren()) {
                String index = fieldSnapshot.getKey();
                if (!index.equals(placeholder)) {
                    if (!fieldSnapshot.hasChild(type)) {
                        Log.e(TAG, "readConfig: No type for " + index);
                        return null;
                    }
                    if (!fieldSnapshot.hasChild(name)) {
                        Log.e(TAG, "readConfig: No name for " + index);
                        return null;
                    }
                    String nameStr = fieldSnapshot.child(name).getValue(String.class);
                    Field.Type t = Field.Type.parse(fieldSnapshot.child(type).getValue(String.class));
                    if (t == Field.Type.UNKNOWN) {
                        Log.e(TAG, "readConfig: Unknown type for " + index);
                        return null;
                    }
                    HashMap<String, String> attributes = new HashMap<>();
                    for (DataSnapshot attribute : fieldSnapshot.getChildren()) {
                        if (!attribute.getKey().equals(type) && !attribute.getKey().equals(name)) {
                            attributes.put(attribute.getKey(), attribute.getValue().toString());
                        }
                    }
                    if (t == Field.Type.INTEGER) {
                        if (!attributes.containsKey(Field.max)) {
                            Log.e(TAG, "readConfig: No max for " + index);
                            return null;
                        }
                        if (!attributes.containsKey(Field.min) || !attributes.containsKey(Field.score)) {
                            Log.e(TAG, "readConfig: No score for " + index);
                            return null;
                        }
                        if (Integer.parseInt(attributes.get(Field.min)) >= Integer.parseInt(attributes.get(Field.max))) {
                            Log.e(TAG, "readConfig: Min >= max in " + index);
                            return null;
                        }
                    } else if (t != Field.Type.CHOICE) {
                        if (t == Field.Type.BOOLEAN && !attributes.containsKey(Field.score)) {
                            Log.e(TAG, "readConfig: No score for " + index);
                            return null;
                        }
                    } else if (!attributes.containsKey(Field.entries)) {
                        Log.e(TAG, "readConfig: No Entries for " + index);
                        return null;
                    }
                    String parentName = attributes.get(Field.dependency);
                    if (parentName != null && !parentName.equals("")) {
                        char modeString = parentName.charAt(0);
                        parentName = parentName.substring(1);
                        fieldsConfig.dependencies.get(fieldKind).add(new DependencyRule(parentName, nameStr, modeString == '_'));
                    }
                    fieldsConfig.fields(fieldKind).add(new Field(nameStr, t, attributes));
                }
            }
        }
        return fieldsConfig;
    }

    public ArrayList<Field> fields(@FieldKind String fieldType) {
        if (auto.equals(fieldType)) {
            return autoFields;
        }
        if (teleOp.equals(fieldType)) {
            return teleOpFields;
        }
        if (penalty.equals(fieldType)) {
            return penaltyFields;
        }

        return null;
    }

    public Field getAutoField(String key) {
        for (Field f : autoFields) {
            if (f.name.equals(key)) {
                return f;
            }
        }
        return null;
    }

    public Field getAutoField(int index) {
        if (index >= autoFields.size()) {
            return null;
        }
        return autoFields.get(index);
    }

    public Field getTeleOpField(String key) {
        for (Field f : teleOpFields) {
            if (f.name.equals(key)) {
                return f;
            }
        }
        return null;
    }

    public Field getTeleOpField(int index) {
        if (index >= teleOpFields.size()) {
            return null;
        }
        return teleOpFields.get(index);
    }

    public Field getPenaltyField(String key) {
        for (Field f : penaltyFields) {
            if (f.name.equals(key)) {
                return f;
            }
        }
        return null;
    }

    public Field getPenaltyField(int index) {
        if (index >= autoFields.size()) {
            return null;
        }
        return autoFields.get(index);
    }

    public Field getField(@FieldKind String fieldKind, String key) {
        if (auto.equals(fieldKind)) {
            return getAutoField(key);
        }
        if (teleOp.equals(fieldKind)) {
            return getTeleOpField(key);
        }

        if (penalty.equals(fieldKind)) {
            return getPenaltyField(key);
        }
        return null;
    }

    public Field getField(@FieldKind String fieldKind, int index) {
        if (auto.equals(fieldKind)) {
            return getAutoField(index);
        }
        if (teleOp.equals(fieldKind)) {
            return getTeleOpField(index);
        }
        if (penalty.equals(fieldKind)) {
            return getPenaltyField(index);
        }
        return null;
    }

    public static final class DependencyRule {
        public final String parent, dependent;
        public final boolean mode;

        public DependencyRule(String parent, String dependent, boolean mode) {
            this.parent = parent;
            this.dependent = dependent;
            this.mode = mode;
        }
    }
}