package com.noam.ftcscouting.database;

import android.util.Log;

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
    public static DatabaseReference reference;
    private static FirebaseUser user;
    private static volatile boolean computing = false;
    private static final Object lock = new Object();

    public static Object DATABASE_CLOSED = "DATABASE_IS_CLOSED";

    public static void openDataBase() {
        user = FirebaseAuth.getInstance().getCurrentUser();
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
                Log.e("Updated", "Now");
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
        iterate(new ArrayList<>(), snapshot, last);
        StaticSync.sync();
        computing = false;
    }

    private static void iterate(ArrayList<String> path, DataSnapshot snapshot, DataSnapshot last) {
        if (snapshot.hasChildren()) {
            for (DataSnapshot child : snapshot.getChildren()) {
                if (last == null || !last.hasChild(child.getKey())) {
                    ArrayList<String> tmp = new ArrayList<>(path);
                    tmp.add(child.getKey());
                    iterate(tmp, child, null);
                }
            }
            if (last != null){
                for (DataSnapshot child : last.getChildren()) {
                    if (!snapshot.hasChild(child.getKey())) {
                        ArrayList<String> tmp = new ArrayList<>(path);
                        StaticSync.queue(tmp);
                    }
                }
            }
        } else {
            ArrayList<String> tmp = new ArrayList<>(path);
            StaticSync.queue(tmp);
        }
    }
}
