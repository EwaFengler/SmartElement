package com.example.smartelement;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Guideline;


public class GameActivity extends AppCompatActivity implements SensorEventListener {

    private MlpModel mlpModel;

    private SensorManager sensorManager;
    private Sensor linearAccSensor;
    private Sensor gravitySensor;
    private Sensor magneticSensor;

    private SensorData sensorData;
    private float[] linearAccelerationReading = new float[3];
    private float[] gravityReading = new float[3];
    private float[] magnetometerReading = new float[3];

    //    private String MODEL_FILENAME = "ConvertedModel/model_xyzpr.tflite";
    private String MODEL_FILENAME = "ConvertedModel/model_xy_zgravity.tflite";
//    private String MODEL_FILENAME = "ConvertedModel/final_model_xy_zgravity.tflite";


    private long lastMoment = 0;

    private final Object dataMonitor = new Object();
    private GameWrapper gameWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameWrapper = new GameWrapper(this);

        try {
            loadModel();
        } catch (IOException e) {
            String errorText = "Wczytanie modelu nie powiodło się.";
            Toast.makeText(getApplicationContext(), errorText, Toast.LENGTH_SHORT).show();
            finish();
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
//        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorData = new SensorData();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (dataMonitor) {
                    if (sensorData.isDataReady()) {
                        float[] input = sensorData.getDataArray();
                        float[] output = mlpModel.run(input);

                        log_output("mlp", output);

                        runOnUiThread(() -> {
//                            if (output[0] > 0.90) {
//                                Log.d("mlp", "horizontal " + output[0]);
//                            }
//                            if (output[1] > 0.90) {
//                                Log.d("mlp", "vertical " + output[1]);
//                            }
//                            if (output[2] > 0.90) {
//                                Log.d("mlp", "forward " + output[2]);
//                            }
                            String move = "---- ";
                            float certainty = Math.max(output[0], Math.max(output[1], output[2]));
                            certainty = (float) ((int) (100 * certainty)) / 100;


                            if (output[2] > 0.90) {
                                move = "forward ";
                                certainty = output[2];
                            } else if (output[0] > 0.90 || output[1] > 0.90) {
                                move = horizontalOrVertical(input);
                                certainty = Math.max(output[0], output[1]);
                            }
                            certainty = (float) ((int) (100 * certainty)) / 100;
                            Log.d("mlp", move + certainty);
                        });
                    }
                }
            }
        }, new Date(), 200);//TODO może być częściej (próbka co 62,5) ale żeby nie stwierdzało dwa razy tego samego
    }

    private String horizontalOrVertical(float[] input) {
        float sumZG = 0;
        int len = input.length / 3;
        for (int i = 0; i < len; i++) {
            sumZG += input[3 * i + 2];
        }

        float meanZG = sumZG / len;

        return Math.abs(meanZG) > 5 ? "horizontal " : "vertical ";
    }

    private void log_output(String tag, float[] output) {
        StringBuilder stringBuilder = new StringBuilder();

        for (float x : output) {
            int a = (int) (100 * x);
            stringBuilder.append((float) a / 100);
            stringBuilder.append(" ");
        }

        Log.d(tag, "\n" + stringBuilder.toString());
    }

    private void loadModel() throws IOException {
        String modelFilename = MODEL_FILENAME;
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
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
//        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_GAME);
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
                sensorData.update();
//                sensorData.addAveragedValues();
//                sensorData.removeOldestValues();
            }
            sensorData.resetSensorValues();
            lastMoment = moment;
        }

        Sensor curSensor = event.sensor;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//            addMagneticSensorValues(event);
        } else if (curSensor.getType() == Sensor.TYPE_GRAVITY) {
            System.arraycopy(event.values, 0, gravityReading, 0, gravityReading.length);
            sensorData.addGravityValues(gravityReading);
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            System.arraycopy(event.values, 0, linearAccelerationReading, 0, linearAccelerationReading.length);
            sensorData.addAccelerationValues(linearAccelerationReading);
        }
    }

    private void addMagneticSensorValues(SensorEvent event) {
        System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);

        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrix(rotationMatrix, null,
                gravityReading, magnetometerReading);

        float[] orientationAngles = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        sensorData.addMagneticValues(orientationAngles);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not implemented
    }

    public void finishGame(GameResult gameResult) {
        //TODO
    }

    public void updateHealth(float damagePercentage) {
        Guideline damageGuideline = findViewById(R.id.damageGuideline);
        damageGuideline.setGuidelinePercent(damagePercentage);
    }

    public void testSendFinish(View view) {
        gameWrapper.finishGame(GameResult.LOSE);
    }

    public void testSendAttack(View view) {
        gameWrapper.sendAttack((float) 4.8);
    }
}
