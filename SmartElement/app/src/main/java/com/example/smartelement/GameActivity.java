package com.example.smartelement;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import java.util.Locale;

public class GameActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Pobieramy menadżer odpowiedni dla kontekstu naszej aplikacji.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Sensor curSensor = event.sensor;

        if (curSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            if (dataOutStream != null) {
                if (startTimestamp == 0) {
                    startTimestamp = event.timestamp;
                }
                long curTimestamp = event.timestamp;

                dataOutStream.printf(Locale.US, "%d %f %f %f", curTimestamp - startTimestamp, x, y, z);
                dataOutStream.print(System.getProperty("line.separator"));
            }
        }
    }
}
