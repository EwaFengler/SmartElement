package com.example.moverecorder;

import androidx.appcompat.app.AppCompatActivity;

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

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accSensor;

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
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

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
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
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

    public void moveRandom(View view) {
        createFile("random", false);
        stopButton.setEnabled(true);
    }

    public void stopRandom(View view) {
        closeStream();
    }

    public void moveForward(View view) {
        createFile("forward",true);
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
                }, 2 * 1000);
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