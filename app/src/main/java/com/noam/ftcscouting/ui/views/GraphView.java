package com.noam.ftcscouting.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class GraphView extends View {
    private final Paint linePaint = new Paint(),
            shapePaint = new Paint(),
            textPaint = new Paint();
    private final ArrayList<Float> pts = new ArrayList<>();
    private final Path linePath = new Path(), circlePath = new Path(), polygonPath = new Path();
    private float radius = 10, textSize = 20;
    private float leftOffset = 40;
    private String yName = null;
    private final PointF clicked = new PointF();
    private Float point = null;

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

        shapePaint.setColor(Color.BLACK);
        shapePaint.setStrokeWidth(10);
        shapePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(textSize);

        setTextSize(40);
    }

    public void setStrokeWidth(float width) {
        linePaint.setStrokeWidth(width);
        invalidate();
    }

    public void setColor(int color) {
        linePaint.setColor(color);
        shapePaint.setColor(color);
        textPaint.setColor(color);
        invalidate();
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
        textPaint.setTextSize(textSize);
        if (yName != null) leftOffset = textSize + 40;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setYName(String yName) {
        this.yName = yName;
        leftOffset = yName == null ? 40 : textSize + 40;
        invalidate();
    }

    public void clear() {
        pts.clear();
        linePath.reset();
        circlePath.reset();
        invalidate();
    }

    public void addPoint(float yVal) {
        pts.add(yVal);
        invalidate();
    }

    public void addData(float[] data) {
        for (float datum : data) pts.add(datum);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                clicked.x = event.getX();
                clicked.y = event.getY();
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getBackground() != null)
            getBackground().draw(canvas);
        else
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        float[] minMax = generatePath();
        canvas.drawPath(linePath, linePaint);
        canvas.drawPath(circlePath, shapePaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(String.format("%.2f", minMax[0]), 2 + getPaddingLeft(),
                2 * textSize + getPaddingTop(), textPaint);
        canvas.drawText(String.format("%.2f", minMax[1]), 2 + getPaddingLeft(),
                getHeight() - 2 - getPaddingBottom(), textPaint);

        canvas.drawLine(textSize + 8 + getPaddingLeft(), textSize * 2.5f + 4 + getPaddingTop(),
                textSize + 8 + getPaddingLeft(), getHeight() - textSize * 1.5f - getPaddingBottom(),
                linePaint);

        textPaint.setTextAlign(Paint.Align.CENTER);

        // down arrow
        polygonPath.moveTo(textSize - 4 + getPaddingLeft(), getHeight() - textSize * 1.25f - 16 - getPaddingBottom());
        polygonPath.lineTo(textSize + 20 + getPaddingLeft(), getHeight() - textSize * 1.25f - 16 - getPaddingBottom());
        polygonPath.lineTo(textSize + 8 + getPaddingLeft(), getHeight() - textSize * 1.25f + 4 - getPaddingBottom());
        canvas.drawPath(polygonPath, shapePaint);
        polygonPath.reset();

        // upper arrow
        polygonPath.moveTo(textSize - 4 + getPaddingLeft(), textSize * 2.25f + 20 + getPaddingTop());
        polygonPath.lineTo(textSize + 20 + getPaddingLeft(), textSize * 2.25f + 20 + getPaddingTop());
        polygonPath.lineTo(textSize + 8 + getPaddingLeft(), textSize * 2.25f + getPaddingTop());
        canvas.drawPath(polygonPath, shapePaint);
        polygonPath.reset();

        if (yName != null) {
            canvas.rotate(-90);
            canvas.drawText(yName, -getHeight() / 2f + getPaddingTop() - getPaddingBottom(), textSize + getPaddingLeft(), textPaint);
        }

        if (point != null) {
            canvas.rotate(90);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(String.format("point = %.2f", point), (getWidth() - getPaddingRight() - getPaddingLeft()) / 2f, getPaddingTop() + textSize * 2, textPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }
    }

    private float[] generatePath() {
        circlePath.reset();
        linePath.reset();
        point = null;
        if (pts.size() == 0) return new float[]{0, 0};

        float xFactor =
                (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - leftOffset - textSize) /
                        (pts.size() - 1);

        float min = pts.get(0);
        float max = min;

        for (int i = 1; i < pts.size(); i ++) {
            if (pts.get(i) > max) max = pts.get(i);
            if (pts.get(i) < min) min = pts.get(i);
        }

        if (min == max){
            min -= 1;
            max += 1;
        }

        float yFactor = max - min;
        yFactor = (getMeasuredHeight() - getPaddingBottom() - getPaddingTop() - textSize * 4) / yFactor;


        float x = leftOffset + getPaddingLeft();
        float y = getPaddingTop() + textSize * 2.5f + (max - pts.get(0)) * yFactor;
        linePath.moveTo(x, y);

        float thisRadius = radius;
        if (Math.pow(clicked.x - x, 2) + Math.pow(clicked.y - y, 2) <= 1000){
            point = pts.get(0);
            thisRadius *= 2;
        }
        circlePath.addCircle(x, y, thisRadius, Path.Direction.CW);

        for (int i = 1; i < pts.size(); i++) {
            x = leftOffset + getPaddingLeft() + i * xFactor;
            y = getPaddingTop() + textSize * 2.5f + (max - pts.get(i)) * yFactor;
            linePath.lineTo(x, y);
            thisRadius = radius;
            if (Math.pow(clicked.x - x, 2) + Math.pow(clicked.y - y, 2) <= 1000){
                point = pts.get(i);
                thisRadius *= 2;
            }
            circlePath.addCircle(x, y, thisRadius, Path.Direction.CW);
        }

        return new float[]{max, min};
    }
}
