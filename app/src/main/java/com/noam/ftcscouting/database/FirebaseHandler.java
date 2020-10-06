package com.noam.ftcscouting.database;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.noam.ftcscouting.utils.StaticSync;

import java.util.ArrayList;

public class FirebaseHandler {

    public static DataSnapshot snapshot = null, last = null;
    public static ArrayList<ArrayList<String>> changes = new ArrayList<>();
    public static DatabaseReference reference;
    public static ArrayList<ArrayList<String>> silent = new ArrayList<>();
    public static FieldsConfig configuration = null;

    private static volatile boolean computing = false;
    private static final Object lock = new Object();

    public static final Object DATABASE_OPENED = "DATABASE_IS_OPEN", DATABASE_CLOSED = "DATABASE_IS_CLOSED";
    public static final String ADD = "#ADD", DEL = "#DEL";

    public static void openDataBase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference().child(user.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                while (computing) {
                    synchronized (lock) {
                        try {
                            lock.wait(0, 1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (last == null){
                    StaticSync.send(DATABASE_OPENED);
                }
                if (configuration == null){
                    configuration = FieldsConfig.readConfig(snapshot.child(FieldsConfig.config));
                }
                last = FirebaseHandler.snapshot;
                FirebaseHandler.snapshot = snapshot;
                computeChanges();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                StaticSync.send(DATABASE_CLOSED);
            }
        });
    }

    private static void computeChanges() {
        computing = true;
        changes.clear();
        iterate(new ArrayList<>(), snapshot, last);
        StaticSync.sync();
        computing = false;
    }

    private static void iterate(ArrayList<String> path, DataSnapshot snapshot, DataSnapshot last) {
        if (snapshot.hasChildren()) {
            for (DataSnapshot child : snapshot.getChildren()) {
                ArrayList<String> tmp = new ArrayList<>(path);
                tmp.add(child.getKey());
                if (silent.contains(tmp)){
                    silent.remove(tmp);
                    continue;
                }
                if (last == null || last.hasChild(child.getKey())) {
                    iterate(tmp, child, last == null ? null : last.child(child.getKey()));
                } else {
                    tmp.add(ADD);
                    queue(tmp);
                }
            }
            if (last != null){
                for (DataSnapshot child : last.getChildren()) {
                    if (!snapshot.hasChild(child.getKey())) {
                        ArrayList<String> tmp = new ArrayList<>(path);
                        tmp.add(child.getKey());
                        tmp.add(DEL);
                        queue(tmp);
                    }
                }
            }
        } else {
            if (silent.contains(path)){
                silent.remove(path);
                return;
            }
            if (last == null || last.getValue() != snapshot.getValue()) {
                ArrayList<String> tmp = new ArrayList<>(path);
                queue(tmp);
            }
        }
    }

    private static void queue(ArrayList<String> change){
        changes.add(change);
        StaticSync.queue(change);
    }

    public static String fireKey(CharSequence sequence) {
        return sequence.toString()
                .replace("$", "{dollar}")
                .replace("[", "{bracketS}")
                .replace("]", "{bracketE}")
                .replace("#", "{hash}")
                .replace(".", "{dot}");
    }

    public static String unFireKey(CharSequence sequence) {
        return sequence.toString().replace("{dollar}", "$")
                .replace("{bracketS}", "[")
                .replace("{bracketE}", "]")
                .replace("{hash}", "#")
                .replace("{dot}", ".");
    }
}
