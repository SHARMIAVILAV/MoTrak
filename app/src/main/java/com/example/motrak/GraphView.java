package com.example.motrak;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GraphView extends View {
    private List<Float> xData = new ArrayList<>();
    private List<Float> yData = new ArrayList<>();
    private List<Float> zData = new ArrayList<>();

    private Paint xLinePaint;
    private Paint yLinePaint;
    private Paint zLinePaint;
    private Paint gridPaint;
    private Paint textPaint;

    private float minValue = -15;
    private float maxValue = 15;
    private int maxDataPoints = 100;
    private String sensorType = "Sensor Data";

    public GraphView(Context context) {
        super(context);
        init();
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Initialize paints
        xLinePaint = new Paint();
        xLinePaint.setColor(Color.RED);
        xLinePaint.setStrokeWidth(3f);
        xLinePaint.setStyle(Paint.Style.STROKE);
        xLinePaint.setAntiAlias(true);

        yLinePaint = new Paint();
        yLinePaint.setColor(Color.GREEN);
        yLinePaint.setStrokeWidth(3f);
        yLinePaint.setStyle(Paint.Style.STROKE);
        yLinePaint.setAntiAlias(true);

        zLinePaint = new Paint();
        zLinePaint.setColor(Color.BLUE);
        zLinePaint.setStrokeWidth(3f);
        zLinePaint.setStyle(Paint.Style.STROKE);
        zLinePaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30f);
    }

    public void setSensorType(String type) {
        this.sensorType = type;
        // Adjust min/max based on sensor type
        if (type.equals("Gyroscope")) {
            minValue = -10;
            maxValue = 10;
        } else if (type.equals("Accelerometer") || type.equals("Gravity")) {
            minValue = -15;
            maxValue = 15;
        } else if (type.equals("Rotation Vector")) {
            minValue = -1;
            maxValue = 1;
        }
        invalidate();
    }

    public void updateData(float x, float y, float z) {
        // Add new data points
        xData.add(x);
        yData.add(y);
        zData.add(z);

        // Limit data points to prevent memory issues
        if (xData.size() > maxDataPoints) {
            xData.remove(0);
        }
        if (yData.size() > maxDataPoints) {
            yData.remove(0);
        }
        if (zData.size() > maxDataPoints) {
            zData.remove(0);
        }

        // Redraw the view
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float padding = 50;
        float graphHeight = height - 2 * padding;
        float graphWidth = width - 2 * padding;

        // Draw title
        canvas.drawText(sensorType, padding, padding - 10, textPaint);

        // Draw legend
        canvas.drawText("X-axis", width - 200, padding - 10, xLinePaint);
        canvas.drawText("Y-axis", width - 200, padding + 30, yLinePaint);
        canvas.drawText("Z-axis", width - 200, padding + 70, zLinePaint);

        // Draw grid
        for (int i = 0; i <= 10; i++) {
            float y = padding + (i * graphHeight / 10);
            canvas.drawLine(padding, y, width - padding, y, gridPaint);

            // Draw y-axis labels
            float value = maxValue - i * (maxValue - minValue) / 10;
            canvas.drawText(String.format("%.1f", value), 5, y + 10, textPaint);
        }

        // Draw zero line with different color
        float zeroY = padding + ((maxValue / (maxValue - minValue)) * graphHeight);
        Paint zeroLinePaint = new Paint(gridPaint);
        zeroLinePaint.setColor(Color.DKGRAY);
        zeroLinePaint.setStrokeWidth(2f);
        canvas.drawLine(padding, zeroY, width - padding, zeroY, zeroLinePaint);

        // Draw data lines if we have data
        if (!xData.isEmpty()) {
            drawDataLine(canvas, xData, xLinePaint, padding, graphWidth, graphHeight, padding);
        }

        if (!yData.isEmpty()) {
            drawDataLine(canvas, yData, yLinePaint, padding, graphWidth, graphHeight, padding);
        }

        if (!zData.isEmpty()) {
            drawDataLine(canvas, zData, zLinePaint, padding, graphWidth, graphHeight, padding);
        }
    }

    private void drawDataLine(Canvas canvas, List<Float> data, Paint paint,
                              float startX, float graphWidth, float graphHeight, float topPadding) {
        Path path = new Path();
        float xInterval = graphWidth / (maxDataPoints - 1);

        for (int i = 0; i < data.size(); i++) {
            float x = startX + i * xInterval;
            // Map data value to y-coordinate (invert because y-axis goes down in Android)
            float normalizedValue = (data.get(i) - minValue) / (maxValue - minValue);
            float y = topPadding + graphHeight - (normalizedValue * graphHeight);

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, paint);
    }

    public void clearData() {
        xData.clear();
        yData.clear();
        zData.clear();
        invalidate();
    }
}