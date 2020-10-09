package com.noam.ftcscouting.ui.views;

import android.content.Context;
import android.util.Pair;
import android.view.View;

import androidx.appcompat.widget.AppCompatCheckBox;

import java.util.ArrayList;

public class DependableCheckBox extends AppCompatCheckBox {

    private final ArrayList<Pair<View, Boolean>> dependents = new ArrayList<>();

    public DependableCheckBox(Context context) {
        super(context);
    }

    public void addDependency(View dependent, boolean mode){
        dependents.add(new Pair<>(dependent, mode));
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (dependents == null) return;
        for (Pair<View, Boolean> dependent : dependents){
            dependent.first.setEnabled(dependent.second == checked);
        }
    }
}
