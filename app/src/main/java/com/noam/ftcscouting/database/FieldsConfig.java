package com.noam.ftcscouting.database;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FieldsConfig {

    public static final String TAG = "FieldsConfig";
    public static final String auto = "Autonomous";
    public static final String config = "config";
    public static final String matches = "matches";
    public static final String name = "name";
    public static final String placeholder = "PLACEHOLDER_DO_NOT_TOUCH";

    public static final String
            telOp = "TelOp",
            type = "type",
            unPlayed = "unplayed";

    private final ArrayList<Field>
            autoFields = new ArrayList<>(),
            telOpFields = new ArrayList<>();

    public ArrayList<DependencyRule> dependencies = new ArrayList<>();

    public static class Field {
        public static final String
                entries = "entries",
                max = "max",
                min = "min",
                score = "score",
                dependency = "dependency";
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

        public Field(String name, Type type) {
            this.name = name;
            this.type = type;
            this.configurations = new HashMap<>();
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
            return null;
        }
        String[] fieldsArr = {auto, telOp};
        for (String fieldType : fieldsArr) {
            if (!configSnapshot.hasChild(fieldType)) {
                return null;
            }
            DataSnapshot child = configSnapshot.child(fieldType);
            if (!child.hasChild(placeholder)) {
                return null;
            }
            for (DataSnapshot fieldSnapshot : configSnapshot.child(fieldType).getChildren()) {
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
                    if (parentName != null && !parentName.equals("")){
                        char modeString = parentName.charAt(0);
                        parentName = parentName.substring(1);
                        fieldsConfig.dependencies.add(new DependencyRule(parentName, nameStr, modeString == '_'));
                    }
                    fieldsConfig.fields(fieldType).add(new Field(nameStr, t, attributes));
                }
            }
        }
        return fieldsConfig;
    }

    public boolean hasAuto(String field) {
        for (Field f : autoFields) {
            if (f.name.equals(field)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTel(String field) {
        for (Field f : telOpFields) {
            if (f.name.equals(field)) {
                return true;
            }
        }
        return false;
    }

    public int autoSize() {
        return autoFields.size();
    }

    public int telOpSize() {
        return telOpFields.size();
    }

    public ArrayList<Field> fields(String fieldType) {
        if (fieldType.equals(auto)) {
            return autoFields;
        }
        if (fieldType.equals(telOp)) {
            return telOpFields;
        }
        return null;
    }

    public ArrayList<Field> getAutoFields() {
        return autoFields;
    }

    public ArrayList<Field> getTelOpFields() {
        return telOpFields;
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

    public Field getTelOpField(String key) {
        for (Field f : telOpFields) {
            if (f.name.equals(key)) {
                return f;
            }
        }
        return null;
    }

    public Field getTelOpField(int index) {
        if (index >= telOpFields.size()) {
            return null;
        }
        return telOpFields.get(index);
    }

    public boolean hasField(String fieldKind, String key) {
        if (auto.equals(fieldKind)) {
            return hasAuto(key);
        }
        if (telOp.equals(fieldKind)) {
            return hasTel(key);
        }
        return false;
    }

    public Field getField(String fieldKind, String key) {
        if (auto.equals(fieldKind)) {
            return getAutoField(key);
        }
        if (telOp.equals(fieldKind)) {
            return getTelOpField(key);
        }
        return null;
    }

    public Field getField(String fieldKind, int index) {
        if (auto.equals(fieldKind)) {
            return getAutoField(index);
        }
        if (telOp.equals(fieldKind)) {
            return getTelOpField(index);
        }
        return null;
    }

    public static final class DependencyRule {
        public final String parent, dependent;
        public final boolean mode;

        public DependencyRule(String parent, String dependent, boolean mode){
            this.parent = parent;
            this.dependent = dependent;
            this.mode = mode;
        }
    }
}