package com.example.motrak;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Spinner sensorTypeSpinner;
    private Button startButton;
    private Button stopButton;
    private TextView timerTextView;

    private boolean isMonitoring = false;
    private int seconds = 0;
    private Handler timerHandler = new Handler();
    private String selectedSensorType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("MoTrak created!");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        sensorTypeSpinner = findViewById(R.id.sensor_type_spinner);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        timerTextView = findViewById(R.id.timer_text_view);

        // Setup spinner with sensor options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sensor_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sensorTypeSpinner.setAdapter(adapter);

        // Sensor selection listener
        sensorTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSensorType = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSensorType = "Accelerometer"; // Default
            }

        });

        // Start button click listener
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMonitoring) {
                    if (selectedSensorType != null) {
                        startMonitoring();
                    } else {
                        Toast.makeText(MainActivity.this, "Please select a sensor type", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Stop button click listener
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMonitoring) {
                    stopMonitoring();
                }
            }
        });
    }

    private void startMonitoring() {
        isMonitoring = true;
        seconds = 0;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        sensorTypeSpinner.setEnabled(false);

        // Start timer
        startTimer();

        System.out.println("MoTrak started Monitoring!");
        Log.d("MoTrakDebug", "Debug Message");

        // Start GraphActivity
        Intent intent = new Intent(MainActivity.this, GraphActivity.class);
        intent.putExtra("SENSOR_TYPE", selectedSensorType);
        startActivity(intent);
    }

    private void stopMonitoring() {
        isMonitoring = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        sensorTypeSpinner.setEnabled(true);

        // Stop timer
        timerHandler.removeCallbacks(timerRunnable);

        // Send broadcast to stop sensor monitoring
        Intent stopIntent = new Intent("STOP_SENSOR_MONITORING");
        sendBroadcast(stopIntent);
    }

    private void startTimer() {
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            seconds++;
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            int secs = seconds % 60;

            String time = String.format("%02d:%02d:%02d", hours, minutes, secs);
            timerTextView.setText(time);

            if (isMonitoring) {
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}