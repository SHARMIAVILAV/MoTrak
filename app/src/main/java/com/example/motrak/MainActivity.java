package com.example.motrak;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MoTrak";
    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 1001;

    private Spinner sensorTypeSpinner;
    private MaterialButton startButton;
    private MaterialButton stopButton;
    private MaterialButton exportButton;
    private TextView timerTextView;
    private TextView maxPointsValueText;
    private SeekBar maxPointsSeekBar;
    private SwitchMaterial darkModeSwitch;
    private SwitchMaterial zoomEnableSwitch;

    private boolean isMonitoring = false;
    private int seconds = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private String selectedSensorType;

    private GraphView graphView;
    private SensorDataManager sensorDataManager;
    private String lastExportPath = null;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize graphView
        graphView = findViewById(R.id.graph_view);
        sensorDataManager = new SensorDataManager(this);

        // Set data listener for live updates
        sensorDataManager.setDataListener(new SensorDataManager.SensorDataListener() {
            @Override
            public void onSensorDataUpdated(float x, float y, float z) {
                graphView.updateData(x, y, z); // Update graph directly
            }
        });

        // Initialize views
        sensorTypeSpinner = findViewById(R.id.sensor_type_spinner);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        exportButton = findViewById(R.id.export_button);
        timerTextView = findViewById(R.id.timer_text_view);
        maxPointsSeekBar = findViewById(R.id.max_points_seekbar);
        maxPointsValueText = findViewById(R.id.max_points_value);
        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        zoomEnableSwitch = findViewById(R.id.zoom_enable_switch);

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
                if (isMonitoring) {
                    // Change sensor type on the fly if we're already monitoring
//                    sensorDataManager.changeSensor(selectedSensorType);
                    graphView.setSensorType(selectedSensorType);
                    graphView.clearData(); // Clear previous data
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSensorType = "Accelerometer"; // Default
            }
        });

        // Setup max points seekbar
        maxPointsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxPointsValueText.setText(String.valueOf(progress));
                graphView.setMaxDataPoints(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Dark mode switch
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            graphView.setDarkMode(isChecked);
        });

        // Zoom enable switch
        zoomEnableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            graphView.setZoomEnabled(isChecked);
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

        // Export button click listener
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkStoragePermission()) {
                    exportData();
                } else {
                    requestStoragePermission();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            showAppInfo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAppInfo() {
        // Show app info dialog/activity
        Toast.makeText(this, "MoTrak - Mobile Motion Tracking App", Toast.LENGTH_SHORT).show();
    }

    private void startMonitoring() {
        isMonitoring = true;
        seconds = 0;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        exportButton.setEnabled(false);

        // Don't disable spinner anymore to allow on-the-fly sensor switching

        // Start timer
        startTimer();

        Log.d(TAG, "Started Monitoring: " + selectedSensorType);

        // Start Monitoring
        sensorDataManager.startMonitoring(selectedSensorType);
        graphView.setSensorType(selectedSensorType); // Update graph title
        graphView.clearData(); // Clear previous data
    }

    private void stopMonitoring() {
        isMonitoring = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        exportButton.setEnabled(true);

        // Stop timer
        timerHandler.removeCallbacks(timerRunnable);

        // Stop sensor monitoring directly
        sensorDataManager.unregisterListeners();

        // Don't clear the graph data to allow export after stopping
        Log.d(TAG, "Stopped Monitoring after " + seconds + " seconds");
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

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, we don't need explicit storage permission for app-specific files
            return true;
        }
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_WRITE_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportData();
            } else {
                Toast.makeText(this, "Storage permission is required to export data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportData() {
        // Get CSV data from GraphView
        String csvData = graphView.exportDataAsCsv();

        if (csvData.isEmpty() || csvData.equals("Time (ms),X,Y,Z\n")) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create file in app-specific directory
            File directory;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                directory = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MoTrak");
            } else {
                directory = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS), "MoTrak");
            }

            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create filename with timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String fileName = selectedSensorType + "_" + timestamp + ".csv";

            File file = new File(directory, fileName);
            FileWriter writer = new FileWriter(file);
            writer.append(csvData);
            writer.flush();
            writer.close();

            lastExportPath = file.getAbsolutePath();

            // Show success message with option to share
            Toast.makeText(this, "Data exported to " + fileName, Toast.LENGTH_LONG).show();
            showShareOption(file);

        } catch (IOException e) {
            Log.e(TAG, "Error exporting data", e);
            Toast.makeText(this, "Error exporting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showShareOption(File file) {
        Uri fileUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share sensor data"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isMonitoring) {
            // Temporarily unregister listeners to save battery
            sensorDataManager.unregisterListeners();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMonitoring) {
            // Re-register listeners if we were monitoring
            sensorDataManager.startMonitoring(selectedSensorType);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        sensorDataManager.unregisterListeners();
    }
}