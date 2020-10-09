package com.noam.ftcscouting.ui.views;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.noam.ftcscouting.R;

public class HorizontalNumberPicker extends ConstraintLayout {

    private TextView mainView, plus, minus;
    private int maxValue, minValue, value;

    public HorizontalNumberPicker(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.horizontal_number_picker_layout, this);
        plus = findViewById(R.id.plus);
        minus = findViewById(R.id.minus);
        mainView = findViewById(R.id.text);
        plus.setOnClickListener(this::increment);
        minus.setOnClickListener(this::decrement);
        mainView.setText("0");
    }

    public void setMaxValue(String s) {
        this.maxValue = Integer.parseInt(s);
    }

    public void setMinValue(String s) {
        this.minValue =Integer. parseInt(s);
    }

    public void setValue(String value) {
        setValue(Integer.parseInt(value));
    }

    public void setValue(int value) {
        this.value = value;
        mainView.setText(String.valueOf(value));
        if (value <= minValue){
            minus.setEnabled(false);
        } else {
            minus.setEnabled(true);
        }

        if (value >= maxValue){
            plus.setEnabled(false);
        } else {
            plus.setEnabled(true);
        }
    }

    public void increment(){
        if (maxValue > value)
            setValue(++value);
    }

    public void decrement(){
        if (minValue < value)
            setValue(--value);
    }

    public void increment(View v){
        increment();
    }

    public void decrement(View v){
        decrement();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mainView.setEnabled(enabled);
        minus.setEnabled(enabled);
        plus.setEnabled(enabled);
    }

    public int getValue() {
        return value;
    }
}
