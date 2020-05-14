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
import java.util.Date;
import java.util.Deque;
import java.util.Timer;
import java.util.TimerTask;


public class GameActivity extends AppCompatActivity implements SensorEventListener {

    private MlpModel mlpModel;

    private SensorManager sensorManager;
    private Sensor accelerationSensor;
    private Sensor magneticSensor;

    private SensorData sensorData;
    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];

    private long lastMoment = 0;

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
        accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorData = new SensorData();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Deque<Float> data = sensorData.getData();
//                if (data.size() == 100) {
//                    double[] dataArray = data.stream().mapToDouble(i -> i).toArray();
//                    mlpModel.run(dataArray);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long zeros = data.stream().filter(i -> i == 0).count();
                            Log.d("zeros", String.valueOf(zeros));
                        }
                    });
//                }
            }
        }, new Date(), 500);//TODO może być częściej (próbka co 62,5) ale żeby nie stwierdzało dwa razy tego samego
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
        sensorManager.registerListener(this, accelerationSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        long moment = event.timestamp / 63000000;// 1:16s
        long moment = event.timestamp /   80000000;// 1:16s

        if (moment != lastMoment) {
            sensorData.addAveragedValues();
            sensorData.resetSensorValues();
            sensorData.removeOldestValues();
            lastMoment = moment;
        }

        Sensor curSensor = event.sensor;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            addMagneticSensorValues(event);

        } else if (curSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
            sensorData.addAccelerationValues(accelerometerReading);
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
