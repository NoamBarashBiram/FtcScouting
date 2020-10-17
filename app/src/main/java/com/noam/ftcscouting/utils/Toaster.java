package com.noam.ftcscouting.utils;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class Toaster {
    public static void toast(Context context, Object... objects) {
        StringBuilder s = new StringBuilder();
        for (Object object : objects) {
            if (object != null) {
                s.append(object.toString());
            } else {
                s.append("null");
            }
            s.append(" ");
        }
        new Handler(context.getMainLooper()).post(() ->
                Toast.makeText(context, s.toString(), Toast.LENGTH_LONG).show()
        );
    }

    public static Snackbar snack(View mainView, Object... objects) {
        StringBuilder s = new StringBuilder();
        for (Object object : objects) {
            if (object != null) {
                s.append(object.toString());
            } else {
                s.append("null");
            }
            s.append(" ");
        }
        Snackbar bar = Snackbar.make(mainView, s.toString(), Snackbar.LENGTH_LONG);
        bar.show();
        return bar;
    }
}
