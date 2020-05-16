package com.example.moverecorder;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private float[] linearAccelerationReading = new float[3];
    private float[] gravityReading = new float[3];
    private float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    private Sensor linearAccSensor;
    private Sensor gravitySensor;
    private Sensor magneticField;

    private Button deleteLastButton;
    private String lastPath = "";

    private PrintStream dataOutStream = null;
    private long startTimestamp = 0;
    private View startButton;
    private View stopButton;
    private View forwardMoveButton;
    private View horizontalMoveButton;
    private View verticalMoveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pobieramy menadżer odpowiedni dla kontekstu naszej aplikacji.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        forwardMoveButton = findViewById(R.id.forwardMoveButton);
        horizontalMoveButton = findViewById(R.id.horizontalMoveButton);
        verticalMoveButton = findViewById(R.id.verticalMoveButton);
        deleteLastButton = findViewById(R.id.deleteLastButton);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Wartość SensorManager.SENSOR_DELAY_NORMAL oznacza domyślną częstotliwość napływania danych
        sensorManager.registerListener(this, linearAccSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
                break;
            case Sensor.TYPE_GRAVITY:
                System.arraycopy(event.values, 0, gravityReading, 0, gravityReading.length);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                System.arraycopy(event.values, 0, linearAccelerationReading, 0, linearAccelerationReading.length);
                sendToStream(event.timestamp);
                break;
        }
    }

    private void sendToStream(long timestamp) {
        float x = linearAccelerationReading[0];
        float y = linearAccelerationReading[1];
        float z = linearAccelerationReading[2];

        float xg = gravityReading[0];
        float yg = gravityReading[1];
        float zg = gravityReading[2];

        calculateOrientationAngles();

        float azimuth = orientationAngles[0];
        float pitch = orientationAngles[1];
        float roll = orientationAngles[2];


        if (dataOutStream != null) {
            if (startTimestamp == 0) {
                startTimestamp = timestamp;
            }
            long curTimestamp = timestamp;

            dataOutStream.printf(Locale.US, "%d %f %f %f %f %f %f %f %f %f",
                    curTimestamp - startTimestamp,
                    x, y, z,
                    xg, yg, zg,
                    azimuth, pitch, roll);
            dataOutStream.print(System.getProperty("line.separator"));
        }
    }


    public void calculateOrientationAngles() {
        SensorManager.getRotationMatrix(rotationMatrix, null,
                gravityReading, magnetometerReading);

        SensorManager.getOrientation(rotationMatrix, orientationAngles);
    }

    public void moveRandom(View view) {
        createFile("random", false);
        stopButton.setEnabled(true);
    }

    public void stopRandom(View view) {
        closeStream();
    }

    public void moveForward(View view) {
        createFile("forward", true);
    }

    public void moveHorizontal(View view) {
        createFile("horizontal", true);
    }

    public void moveVertical(View view) {
        createFile("vertical", true);
    }

    public void deleteLast(View view) {
        new File(lastPath).delete();
        deleteLastButton.setEnabled(false);
    }

    private void createFile(String type, boolean timeout) {
        String dirPath = String.format("%s/moves/%s", this.getExternalFilesDir(null), type);

        File dir = new File(dirPath);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        String dataOutPath = String.format("%s/%2$td-%2$tm_%2$tH-%2$tM-%2$tS", dirPath, new Date());
        lastPath = dataOutPath;

        try {
            dataOutStream = new PrintStream(new FileOutputStream(dataOutPath));
            setMovesEnabled(false);
            deleteLastButton.setEnabled(false);

            if (timeout) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        closeStream();
                    }
                }, 1500);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void closeStream() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataOutStream.close();
                dataOutStream = null;
                startTimestamp = 0;
                setMovesEnabled(true);
                deleteLastButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });
    }

    private void setMovesEnabled(boolean enabled) {
        startButton.setEnabled(enabled);
        forwardMoveButton.setEnabled(enabled);
        horizontalMoveButton.setEnabled(enabled);
        verticalMoveButton.setEnabled(enabled);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not implemented
    }
}