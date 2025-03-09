package com.example.motrak;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.animation.ValueAnimator;

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
    private Paint backgroundPaint;

    // For filled area under graph
    private Paint xFillPaint;
    private Paint yFillPaint;
    private Paint zFillPaint;

    private float minValue = -15;
    private float maxValue = 15;
    private int maxDataPoints = 100;
    private String sensorType = "Sensor Data";

    // For zooming and panning
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor = 1f;
    private float xOffset = 0f;
    private boolean isZoomEnabled = true;

    // Animation properties
    private ValueAnimator dataAnimator;
    private static final int ANIMATION_DURATION = 300;

    // Theme options
    private boolean isDarkMode = false;

    // Timestamp for data points
    private List<Long> timestamps = new ArrayList<>();
    private long startTime = 0;

    public GraphView(Context context) {
        super(context);
        init(context);
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // Initialize paints
        xLinePaint = new Paint();
        xLinePaint.setColor(Color.parseColor("#FF5252")); // Brighter red
        xLinePaint.setStrokeWidth(4f);
        xLinePaint.setStyle(Paint.Style.STROKE);
        xLinePaint.setAntiAlias(true);

        yLinePaint = new Paint();
        yLinePaint.setColor(Color.parseColor("#4CAF50")); // Brighter green
        yLinePaint.setStrokeWidth(4f);
        yLinePaint.setStyle(Paint.Style.STROKE);
        yLinePaint.setAntiAlias(true);

        zLinePaint = new Paint();
        zLinePaint.setColor(Color.parseColor("#2196F3")); // Brighter blue
        zLinePaint.setStrokeWidth(4f);
        zLinePaint.setStyle(Paint.Style.STROKE);
        zLinePaint.setAntiAlias(true);

        // Fill paints for area under curves
        xFillPaint = new Paint();
        xFillPaint.setStyle(Paint.Style.FILL);
        xFillPaint.setAntiAlias(true);

        yFillPaint = new Paint();
        yFillPaint.setStyle(Paint.Style.FILL);
        yFillPaint.setAntiAlias(true);

        zFillPaint = new Paint();
        zFillPaint.setStyle(Paint.Style.FILL);
        zFillPaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));

        textPaint = new Paint();
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(36f);
        textPaint.setAntiAlias(true);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);

        // Initialize gesture detectors
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        // Initialize animation
        dataAnimator = ValueAnimator.ofFloat(0f, 1f);
        dataAnimator.setDuration(ANIMATION_DURATION);
        dataAnimator.setInterpolator(new DecelerateInterpolator());
    }

    public void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
        if (darkMode) {
            backgroundPaint.setColor(Color.parseColor("#121212"));
            textPaint.setColor(Color.LTGRAY);
            gridPaint.setColor(Color.parseColor("#333333"));
        } else {
            backgroundPaint.setColor(Color.WHITE);
            textPaint.setColor(Color.DKGRAY);
            gridPaint.setColor(Color.LTGRAY);
        }
        invalidate();
    }

    public void setZoomEnabled(boolean enabled) {
        isZoomEnabled = enabled;
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

        // Animate the change
        animateRedraw();
    }

    public void updateData(float x, float y, float z) {
        // Record timestamp
        long currentTime = System.currentTimeMillis();
        if (startTime == 0) {
            startTime = currentTime;
        }
        timestamps.add(currentTime - startTime);

        // Add new data points
        xData.add(x);
        yData.add(y);
        zData.add(z);

        // Limit data points to prevent memory issues
        if (xData.size() > maxDataPoints) {
            xData.remove(0);
            yData.remove(0);
            zData.remove(0);
            timestamps.remove(0);
        }

        // Animate the new data point
        animateRedraw();
    }

    private void animateRedraw() {
        dataAnimator.removeAllUpdateListeners();
        dataAnimator.addUpdateListener(animation -> {
            // Just trigger redraw during animation
            invalidate();
        });
        dataAnimator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isZoomEnabled) {
            return super.onTouchEvent(event);
        }

        boolean handled = scaleDetector.onTouchEvent(event);
        handled = gestureDetector.onTouchEvent(event) || handled;
        return handled || super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        float width = getWidth();
        float height = getHeight();
        float padding = 80;
        float graphHeight = height - 2 * padding;
        float graphWidth = width - 2 * padding;

        // Apply zoom and pan
        float effectiveWidth = graphWidth * scaleFactor;
        float visibleStartX = Math.max(0, Math.min(xOffset, effectiveWidth - graphWidth));

        // Draw title with shadow
        textPaint.setShadowLayer(3, 1, 1, isDarkMode ? Color.BLACK : Color.LTGRAY);
        canvas.drawText(sensorType, padding, padding - 30, textPaint);
        textPaint.clearShadowLayer();

        // Draw legend with better spacing and colored boxes
        float legendX = width - 250;
        float legendY = padding - 30;
        float boxSize = 20;

        // X-axis legend
        canvas.drawRect(legendX, legendY - boxSize + 5, legendX + boxSize, legendY + 5, xLinePaint);
        canvas.drawText("X-axis", legendX + boxSize + 10, legendY + 5, textPaint);

        // Y-axis legend
        legendY += 40;
        canvas.drawRect(legendX, legendY - boxSize + 5, legendX + boxSize, legendY + 5, yLinePaint);
        canvas.drawText("Y-axis", legendX + boxSize + 10, legendY + 5, textPaint);

        // Z-axis legend
        legendY += 40;
        canvas.drawRect(legendX, legendY - boxSize + 5, legendX + boxSize, legendY + 5, zLinePaint);
        canvas.drawText("Z-axis", legendX + boxSize + 10, legendY + 5, textPaint);

        // Draw grid with dashed lines
        for (int i = 0; i <= 10; i++) {
            float y = padding + (i * graphHeight / 10);
            canvas.drawLine(padding, y, width - padding, y, gridPaint);

            // Draw y-axis labels with better formatting
            float value = maxValue - i * (maxValue - minValue) / 10;
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.format("%.1f", value), padding - 10, y + 10, textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
        }

        // Draw vertical grid lines
        for (int i = 0; i <= 5; i++) {
            float x = padding + (i * graphWidth / 5);
            canvas.drawLine(x, padding, x, height - padding, gridPaint);

            // Draw x-axis time labels (seconds)
            if (!timestamps.isEmpty()) {
                float fraction = (float) i / 5;
                int dataIndex = Math.min(timestamps.size() - 1, Math.round(fraction * (timestamps.size() - 1)));
                float seconds = timestamps.get(dataIndex) / 1000f;
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(String.format("%.1fs", seconds), x, height - padding + 30, textPaint);
                textPaint.setTextAlign(Paint.Align.LEFT);
            }
        }

        // Draw zero line with different color
        float zeroY = padding + ((maxValue / (maxValue - minValue)) * graphHeight);
        Paint zeroLinePaint = new Paint(gridPaint);
        zeroLinePaint.setColor(isDarkMode ? Color.parseColor("#777777") : Color.DKGRAY);
        zeroLinePaint.setStrokeWidth(2f);
        zeroLinePaint.setPathEffect(null); // No dash for zero line
        canvas.drawLine(padding, zeroY, width - padding, zeroY, zeroLinePaint);

        // Draw data area and lines if we have data
        canvas.save();
        canvas.clipRect(padding, padding, width - padding, height - padding);

        if (!xData.isEmpty()) {
            // Setup gradient for X
            xFillPaint.setAlpha(70);
            xFillPaint.setShader(new LinearGradient(0, zeroY, 0, height - padding,
                    xLinePaint.getColor(), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            drawDataArea(canvas, xData, xFillPaint, padding, graphWidth, graphHeight, padding, zeroY);
            drawDataLine(canvas, xData, xLinePaint, padding, graphWidth, graphHeight, padding);
        }

        if (!yData.isEmpty()) {
            // Setup gradient for Y
            yFillPaint.setAlpha(70);
            yFillPaint.setShader(new LinearGradient(0, zeroY, 0, height - padding,
                    yLinePaint.getColor(), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            drawDataArea(canvas, yData, yFillPaint, padding, graphWidth, graphHeight, padding, zeroY);
            drawDataLine(canvas, yData, yLinePaint, padding, graphWidth, graphHeight, padding);
        }

        if (!zData.isEmpty()) {
            // Setup gradient for Z
            zFillPaint.setAlpha(70);
            zFillPaint.setShader(new LinearGradient(0, zeroY, 0, height - padding,
                    zLinePaint.getColor(), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            drawDataArea(canvas, zData, zFillPaint, padding, graphWidth, graphHeight, padding, zeroY);
            drawDataLine(canvas, zData, zLinePaint, padding, graphWidth, graphHeight, padding);
        }

        canvas.restore();
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

    private void drawDataArea(Canvas canvas, List<Float> data, Paint paint,
                              float startX, float graphWidth, float graphHeight, float topPadding, float zeroY) {
        Path path = new Path();
        float xInterval = graphWidth / (maxDataPoints - 1);

        // Start at the x axis
        path.moveTo(startX, zeroY);

        // Draw to each data point
        for (int i = 0; i < data.size(); i++) {
            float x = startX + i * xInterval;
            float normalizedValue = (data.get(i) - minValue) / (maxValue - minValue);
            float y = topPadding + graphHeight - (normalizedValue * graphHeight);
            path.lineTo(x, y);
        }

        // Close the path back to the x axis
        if (!data.isEmpty()) {
            path.lineTo(startX + (data.size() - 1) * xInterval, zeroY);
        }
        path.close();

        canvas.drawPath(path, paint);
    }

    public void clearData() {
        xData.clear();
        yData.clear();
        zData.clear();
        timestamps.clear();
        startTime = 0;
        invalidate();
    }

    // Zoom listener
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large
            scaleFactor = Math.max(1f, Math.min(scaleFactor, 5.0f));

            invalidate();
            return true;
        }
    }

    // Pan listener
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (scaleFactor > 1.0f) {
                xOffset += distanceX;
                invalidate();
                return true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Reset zoom on double tap
            scaleFactor = 1.0f;
            xOffset = 0f;
            invalidate();
            return true;
        }
    }

    // Method to export data to CSV
    public String exportDataAsCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("Time (ms),X,Y,Z\n");

        for (int i = 0; i < xData.size(); i++) {
            csv.append(timestamps.get(i)).append(",")
                    .append(xData.get(i)).append(",")
                    .append(yData.get(i)).append(",")
                    .append(zData.get(i)).append("\n");
        }

        return csv.toString();
    }

    // Add option to set max data points
    public void setMaxDataPoints(int points) {
        this.maxDataPoints = Math.max(50, points);
        // Trim existing data if needed
        while (xData.size() > maxDataPoints) {
            xData.remove(0);
            yData.remove(0);
            zData.remove(0);
            if (!timestamps.isEmpty()) timestamps.remove(0);
        }
        invalidate();
    }
}