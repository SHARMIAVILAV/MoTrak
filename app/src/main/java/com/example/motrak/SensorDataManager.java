package com.example.motrak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public class SensorDataManager implements SensorEventListener {
    private Context context;
    private SensorManager sensorManager;
    private Sensor selectedSensor;
    private List<Float> xValues = new ArrayList<>();
    private List<Float> yValues = new ArrayList<>();
    private List<Float> zValues = new ArrayList<>();
    private int maxDataPoints = 100; // Limit data points to prevent memory issues

    // Interface for notifying listeners of new data
    public interface SensorDataListener {
        void onSensorDataUpdated(float x, float y, float z);
    }

    private SensorDataListener dataListener;
    private BroadcastReceiver stopReceiver;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public SensorDataManager(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // Register broadcast receiver for stopping sensor monitoring
        stopReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("STOP_SENSOR_MONITORING")) {
                    unregisterListeners();
                }
            }
        };

        IntentFilter filter = new IntentFilter("STOP_SENSOR_MONITORING");
        context.registerReceiver(stopReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

    }

    public void setDataListener(SensorDataListener listener) {
        this.dataListener = listener;
    }

    public void startMonitoring(String sensorType) {
        int sensorTypeId = getSensorTypeFromString(sensorType);
        selectedSensor = sensorManager.getDefaultSensor(sensorTypeId);

        if (selectedSensor != null) {
            sensorManager.registerListener(this, selectedSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private int getSensorTypeFromString(String sensorType) {
        switch (sensorType) {
            case "Accelerometer":
                return Sensor.TYPE_ACCELEROMETER;
            case "Gyroscope":
                return Sensor.TYPE_GYROSCOPE;
            case "Gravity":
                return Sensor.TYPE_GRAVITY;
            case "Rotation Vector":
                return Sensor.TYPE_ROTATION_VECTOR;
            default:
                return Sensor.TYPE_ACCELEROMETER; // Default
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == selectedSensor.getType()) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Add values to lists (with limit)
            addValueWithLimit(xValues, x);
            addValueWithLimit(yValues, y);
            addValueWithLimit(zValues, z);

            // Notify listener of new data
            if (dataListener != null) {
                dataListener.onSensorDataUpdated(x, y, z);
            }
        }
    }

    private void addValueWithLimit(List<Float> list, float value) {
        list.add(value);
        if (list.size() > maxDataPoints) {
            list.remove(0);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this implementation
    }

    public List<Float> getXValues() {
        return new ArrayList<>(xValues);
    }

    public List<Float> getYValues() {
        return new ArrayList<>(yValues);
    }

    public List<Float> getZValues() {
        return new ArrayList<>(zValues);
    }

    public void clearData() {
        xValues.clear();
        yValues.clear();
        zValues.clear();
    }

    public void unregisterListeners() {
        sensorManager.unregisterListener(this);
    }

    public void onDestroy() {
        unregisterListeners();
        context.unregisterReceiver(stopReceiver);
    }
}