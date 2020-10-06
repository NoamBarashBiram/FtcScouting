package com.noam.ftcscouting.database;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.noam.ftcscouting.utils.StaticSync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FieldsConfig implements StaticSync.Notifiable {
    public static final String auto = "Autonomous";
    public static final String config = "config";
    public static final String matches = "matches";
    public static final String name = "name";
    public static final String placeholder = "PLACEHOLDER_DO_NOT_TOUCH";

    public static final String
            telOp = "TelOp",
            type = "type",
            unPlayed = "unplayed";
    private final HashMap<Integer, Field>
            autoFields = new HashMap<>(),
            telOpFields = new HashMap<>();

    @Override
    public void onNotified(Object message) {
        if (message instanceof ArrayList) {
            ArrayList<String> realMessage = (ArrayList<String>) message;
            if (config.equals(realMessage.get(0))) {
                int lastIndex = realMessage.size() - 1;
                if (FirebaseHandler.ADD.equals(realMessage.get(lastIndex))){

                }
            }
        }
    }

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
            BOOLEAN("bol"),
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
        int indexInt;
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
                    if (!index.matches("[0-9]+")) {
                        indexInt = fieldsConfig.fields(fieldType).size();
                        Log.e("Problem", "Non-Integer Index!");
                    } else {
                        indexInt = Integer.parseInt(index);
                    }
                    if (fieldsConfig.fields(fieldType).containsKey(indexInt)) {
                        indexInt = fieldsConfig.fields(fieldType).size();
                        Log.e("Problem", "Identical indexes!");
                    }
                    if (!fieldSnapshot.hasChild(type)) {
                        return null;
                    }
                    if (!fieldSnapshot.hasChild(name)) {
                        return null;
                    }
                    String nameStr = fieldSnapshot.child(name).getValue(String.class);
                    Field.Type t = Field.Type.parse(fieldSnapshot.child(type).getValue(String.class));
                    if (t == Field.Type.UNKNOWN) {
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
                            return null;
                        }
                        if (!attributes.containsKey(Field.min) || !attributes.containsKey(Field.score)) {
                            return null;
                        }
                        if (Integer.parseInt(attributes.get(Field.min)) >= Integer.parseInt(attributes.get(Field.max))) {
                            return null;
                        }
                    } else if (t != Field.Type.CHOICE) {
                        if (t == Field.Type.BOOLEAN && !attributes.containsKey(Field.score)) {
                            return null;
                        }
                    } else if (!attributes.containsKey(Field.entries)) {
                        return null;
                    }
                    fieldsConfig.fields(fieldType).put(indexInt, new Field(nameStr, t, attributes));
                }
            }
        }
        StaticSync.register(fieldsConfig);
        return fieldsConfig;
    }

    public Map<String, Object> getCleanAuto(int matchCount) {
        HashMap<String, Object> result = new HashMap<>();
        String defStr = mulStr(";", matchCount - 1) +
                " ";
        String defNum = mulStr("0;", matchCount - 1) +
                "0";
        for (Field f : autoFields.values()) {
            if (f.type != Field.Type.TITLE) {
                result.put(f.name, f.type == Field.Type.STRING ? defStr : defNum);
            }
        }
        return result;
    }

    public Map<String, Object> getCleanTelOp(int matchCount) {
        HashMap<String, Object> result = new HashMap<>();
        String defStr = mulStr(";", matchCount - 1);
        String defNum = mulStr("0;", matchCount - 1) +
                "0";
        for (Field f : telOpFields.values()) {
            if (f.type != Field.Type.TITLE) {
                result.put(f.name, f.type == Field.Type.STRING ? defStr : defNum);
            }
        }
        return result;
    }

    private String mulStr(String s, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(s);
        }
        return result.toString();
    }

    public boolean hasAuto(String field) {
        for (Field f : autoFields.values()) {
            if (f.name.equals(field)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTel(String field) {
        for (Field f : telOpFields.values()) {
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

    public HashMap<Integer, Field> fields(String fieldType) {
        if (fieldType.equals(auto)) {
            return autoFields;
        }
        if (fieldType.equals(telOp)) {
            return telOpFields;
        }
        return null;
    }

    public HashMap<Integer, Field> getAutoFields() {
        return autoFields;
    }

    public HashMap<Integer, Field> getTelOpFields() {
        return telOpFields;
    }

    public Field getAutoField(String key) {
        for (Field f : autoFields.values()) {
            if (f.name.equals(key)) {
                return f;
            }
        }
        return null;
    }

    public Field getAutoField(int index) {
        if (!autoFields.containsKey(index)) {
            return null;
        }
        return autoFields.get(index);
    }

    public Field getTelOpField(String key) {
        for (Field f : telOpFields.values()) {
            if (f.name.equals(key)) {
                return f;
            }
        }
        return null;
    }

    public Field getTelOpField(int index) {
        if (!telOpFields.containsKey(index)) {
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
}
