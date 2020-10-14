package com.noam.ftcscouting.ui.views;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.noam.ftcscouting.R;

public class HorizontalNumberPicker extends ConstraintLayout {

    public static final String TAG = "HorizontalNumberPicker";

    private OnValueChangeListener listener = null;

    private TextView mainView, plus, minus;
    private int maxValue = 10, minValue = 0, value = 0;

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
        this.minValue = Integer.parseInt(s);
    }

    public void increment() {
        if (maxValue > value)
            setValue(++value);
    }

    public void decrement() {
        if (minValue < value)
            setValue(--value);
    }

    public void increment(View v) {
        increment();
    }

    public void decrement(View v) {
        decrement();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mainView.setEnabled(enabled);
        if (value > minValue)
            minus.setEnabled(enabled);

        if (value < maxValue)
            plus.setEnabled(enabled);
    }

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.listener = listener;
    }

    public int getValue() {
        return value;
    }

    public void setValue(String value) {
        try {
            setValue(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            setValue(0);
        }
    }

    public void setValue(int value) {
        if (value < minValue || value > maxValue)
            throw new IllegalArgumentException(
                    "Value " + value + " is not in range " + minValue + ", " + maxValue
            );

        mainView.setText(String.valueOf(value));
        if (value <= minValue) {
            minus.setEnabled(false);
        } else {
            minus.setEnabled(true);
        }

        if (value >= maxValue) {
            plus.setEnabled(false);
        } else {
            plus.setEnabled(true);
        }

        if (listener != null)
            listener.onValueChange(this.value, value);

        this.value = value;
    }

    public interface OnValueChangeListener {
        void onValueChange(int oldVal, int newVal);
    }
}
