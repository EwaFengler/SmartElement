package com.example.smartelement;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class GameActivity extends AppCompatActivity implements SensorEventListener {

    private MlpModel mlpModel;

    private SensorManager sensorManager;
    private Sensor linearAccSensor;
    private Sensor accelerationSensor;
    private Sensor magneticSensor;

    private SensorData sensorData;
    private float[] linearAccelerationReading = new float[3];
    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];

    private long lastMoment = 0;

    private final Object dataMonitor = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        try {
            loadModel();
        } catch (IOException e) {
            String errorText = "Wczytanie modelu nie powiodło się.";
            Toast.makeText(getApplicationContext(), errorText, Toast.LENGTH_SHORT).show();
            finish();
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorData = new SensorData();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (dataMonitor) {
                    if (sensorData.getData().size() == 100) {
                        float[] input = sensorData.getDataArray();
                        float[] output = mlpModel.run(input);

                        runOnUiThread(() -> {
                            if (output[0] > 0.90) {
                                Log.d("mlp", "horizontal " + output[0]);
                            }
                            if (output[1] > 0.90) {
                                Log.d("mlp", "vertical " + output[1]);
                            }
                            if (output[2] > 0.90) {
                                Log.d("mlp", "forward " + output[2]);
                            }
                        });
                    }
                }
            }
        }, new Date(), 200);//TODO może być częściej (próbka co 62,5) ale żeby nie stwierdzało dwa razy tego samego
    }

    private void loadModel() throws IOException {
        String modelFilename = "ConvertedModel/converted_model.tflite";
        Interpreter tflite = new Interpreter(loadModelFile(modelFilename));
        mlpModel = new MlpModel(tflite);
    }

    private MappedByteBuffer loadModelFile(String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, linearAccSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accelerationSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long moment = event.timestamp / 62500000;// 1:16s

        if (moment != lastMoment) {
            synchronized (dataMonitor) {
                sensorData.addAveragedValues();
                sensorData.removeOldestValues();
            }
            sensorData.resetSensorValues();
            lastMoment = moment;
        }

        Sensor curSensor = event.sensor;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            addMagneticSensorValues(event);
        } else if (curSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            System.arraycopy(event.values, 0, linearAccelerationReading, 0, linearAccelerationReading.length);
            sensorData.addAccelerationValues(linearAccelerationReading);
        }
    }

    private void addMagneticSensorValues(SensorEvent event) {
        System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);

        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        float[] orientationAngles = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        sensorData.addMagneticValues(orientationAngles);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not implemented
    }
}
