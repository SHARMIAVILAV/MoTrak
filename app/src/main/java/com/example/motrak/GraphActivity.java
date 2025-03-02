package com.example.motrak;


import android.os.Bundle;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

public class GraphActivity extends AppCompatActivity implements SensorDataManager.SensorDataListener {


    private GraphView graphView;
    private TextView xValueText;
    private TextView yValueText;
    private TextView zValueText;
    private SensorDataManager sensorDataManager;
    private String sensorType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        // Get the selected sensor type from intent
        sensorType = getIntent().getStringExtra("SENSOR_TYPE");
        if (sensorType == null) {
            sensorType = "Accelerometer"; // Default
        }
        System.out.println("MoTrak SensorType Choosed!");

        // Initialize views
        graphView = findViewById(R.id.graph_view);
        xValueText = findViewById(R.id.x_value_text);
        yValueText = findViewById(R.id.y_value_text);
        zValueText = findViewById(R.id.z_value_text);
        TextView sensorTitleText = findViewById(R.id.sensor_title_text);

        // Set sensor type to graph and title
        graphView.setSensorType(sensorType);
        sensorTitleText.setText(sensorType + " Data");

        // Initialize sensor data manager
        sensorDataManager = new SensorDataManager(this);
        sensorDataManager.setDataListener(this);
        sensorDataManager.startMonitoring(sensorType);
    }

    @Override
    public void onSensorDataUpdated(float x, float y, float z) {
        // Update graph
        graphView.updateData(x, y, z);

        // Update text displays
        xValueText.setText("X: " + String.format("%.2f", x));
        yValueText.setText("Y: " + String.format("%.2f", y));
        zValueText.setText("Z: " + String.format("%.2f", z));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorDataManager != null) {
            sensorDataManager.onDestroy();
        }
    }

    @Override
    public void onBackPressed() {
        // Clean up resources before going back
        if (sensorDataManager != null) {
            sensorDataManager.unregisterListeners();
        }
        super.onBackPressed();
    }
}