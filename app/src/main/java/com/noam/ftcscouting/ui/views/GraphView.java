package com.noam.ftcscouting.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class GraphView extends View {

    private static final String TAG = "GraphView";

    private final Paint linePaint = new Paint(), circlePaint = new Paint();
    private final ArrayList<Float> pts = new ArrayList<>();
    private final Path linePath = new Path(), circlePath = new Path();
    private float radius = 10;

    public GraphView(Context context) {
        this(context, null);
    }

    public GraphView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(10);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        circlePaint.setColor(Color.BLACK);
        circlePaint.setStrokeWidth(10);
        circlePaint.setStyle(Paint.Style.FILL);
    }

    public void setStrokeWidth(float width) {
        linePaint.setStrokeWidth(width);
        invalidate();
    }

    public void setColor(int color) {
        linePaint.setColor(color);
        circlePaint.setColor(color);
        invalidate();
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void clear() {
        pts.clear();
        linePath.reset();
        circlePath.reset();
        invalidate();
    }

    public void addPoint(float x, float y) {
        pts.add(x);
        pts.add(y);

        invalidate();
    }

    public void setData(float[] x, float[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException("X length does not equal Y length");

        for (int i = 0; i < x.length; i++) {
            pts.add(x[i]);
            pts.add(y[i]);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (getBackground() != null)
            getBackground().draw(canvas);

        generatePath();
        canvas.drawPath(linePath, linePaint);
        canvas.drawPath(circlePath, circlePaint);
    }

    private void generatePath() {
        if (pts.size() == 0) return;

        float minX = pts.get(0);
        float max = minX;

        for (int i = 2; i < pts.size(); i += 2) {
            if (pts.get(i) > max) max = pts.get(i);
            if (pts.get(i) < minX) minX = pts.get(i);
        }

        float xFactor = max - minX;
        xFactor = (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) / xFactor;

        float minY = pts.get(1);
        max = minY;

        for (int i = 3; i < pts.size(); i += 2) {
            if (pts.get(i) > max) max = pts.get(i);
            if (pts.get(i) < minY) minY = pts.get(i);
        }

        float yFactor = max - minY;
        yFactor = (getMeasuredHeight() - getPaddingBottom() - getPaddingTop()) / yFactor;


        float x  = getPaddingLeft() + (pts.get(0) - minX) * xFactor;
        float y = getPaddingTop() + (max - pts.get(1)) * yFactor;
        linePath.moveTo(x, y);
        circlePath.addCircle(x, y, radius, Path.Direction.CW);

        for (int i = 2; i < pts.size(); i += 2) {
            x = getPaddingLeft() + (pts.get(i) - minX) * xFactor;
            y = getPaddingTop() + (max - pts.get(i + 1)) * yFactor;
            circlePath.addCircle(x, y, radius, Path.Direction.CW);
            linePath.lineTo(x, y);
        }
    }
}
