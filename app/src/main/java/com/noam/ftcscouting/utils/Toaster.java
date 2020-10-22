package com.noam.ftcscouting.utils;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

public class Toaster {
    public static void toast(Context context, Object... objects) {
        String s = parse(objects);
        new Handler(context.getMainLooper()).post(() ->
                Toast.makeText(context, s, Toast.LENGTH_LONG).show()
        );
    }

    private static String parse(Object[] objects) {
        StringBuilder s = new StringBuilder();
        for (Object object : objects) {
            if (object != null) {
                s.append(object.toString());
            } else {
                s.append("null");
            }
            s.append(" ");
        }
        return s.toString();
    }

    public static Snackbar snack(View mainView, Object... objects) {
        Snackbar bar = snack(null, null, mainView, objects);
        bar.show();
        return bar;
    }

    public static Snackbar snack(@Nullable BaseTransientBottomBar.BaseCallback<Snackbar> callback, View mainView, Object... objects) {
        return snack(null, callback, mainView, objects);
    }

    public static Snackbar snack(@Nullable BaseTransientBottomBar.Behavior behavior, View mainView, Object... objects) {
        return snack(behavior, null, mainView, objects);
    }

    public static Snackbar snack(@Nullable BaseTransientBottomBar.Behavior behavior, @Nullable BaseTransientBottomBar.BaseCallback<Snackbar> callback, View mainView, Object... objects) {
        String s = parse(objects);
        Snackbar bar = Snackbar.make(mainView, s, Snackbar.LENGTH_LONG)
                .addCallback(callback);
        if (behavior != null)
            bar.setBehavior(behavior);
        return bar;
    }
}
