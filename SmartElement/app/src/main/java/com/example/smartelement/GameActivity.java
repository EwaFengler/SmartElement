package com.example.smartelement;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Guideline;


public class GameActivity extends AppCompatActivity implements SensorEventListener {

    private MlpModel mlpModel;

    private SensorManager sensorManager;
    private Sensor linearAccSensor;
    private Sensor gravitySensor;

    private SensorData sensorData;
    private float[] linearAccelerationReading = new float[3];
    private float[] gravityReading = new float[3];

    private String MODEL_FILENAME = "ConvertedModel/hopefully_final_model_xy_zgravity.tflite";


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
                                gameWrapper.onExecute();
                            } else if (output[0] > 0.90 || output[1] > 0.90) {
                                move = horizontalOrVertical(input);
                                certainty = Math.max(output[0], output[1]);
                                if (move.equals("horizontal ")) {
                                    gameWrapper.onShield();
                                } else {
                                    gameWrapper.onAttack();
                                }
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onBackPressed() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Powrót");
        alertDialog.setMessage("Jesteś pewien, że chcesz zrezygnować z tej rozgrywki? Twój przeciwnik wygra walkowerem");

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Gram dalej", ((dialog, which) -> {
        }));
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Rezygnuję", (dialog, which) -> gameWrapper.finishGame(GameResult.LOSE));

        alertDialog.show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long moment = event.timestamp / 62500000;// 1:16s

        if (moment != lastMoment) {
            synchronized (dataMonitor) {
                sensorData.update();
            }
            sensorData.resetSensorValues();
            lastMoment = moment;
        }

        Sensor curSensor = event.sensor;

        if (curSensor.getType() == Sensor.TYPE_GRAVITY) {
            System.arraycopy(event.values, 0, gravityReading, 0, gravityReading.length);
            sensorData.addGravityValues(gravityReading);
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            System.arraycopy(event.values, 0, linearAccelerationReading, 0, linearAccelerationReading.length);
            sensorData.addAccelerationValues(linearAccelerationReading);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not implemented
    }

    public void finishGame(GameResult gameResult) {
        Intent i = new Intent(this, FinishGameActivity.class);
        i.putExtra("gameResult", gameResult);
        startActivity(i);

        SharedPreferences sharedPreferences = getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (gameResult == GameResult.WIN) {
            int win = sharedPreferences.getInt("win", 0);
            editor.putInt("win", win + 1);

        } else {
            int lose = sharedPreferences.getInt("lose", 0);
            editor.putInt("lose", lose + 1);
        }
        editor.apply();
        finish();
    }

    public void updateHealth(float damagePercentage) {
        Guideline damageGuideline = findViewById(R.id.damageGuideline);
        damageGuideline.setGuidelinePercent(damagePercentage);

        LinearLayout healthLayout = findViewById(R.id.healthLayout);
        if (damagePercentage > 0.5 && damagePercentage <= 0.75) {
            healthLayout.setBackgroundColor(Color.parseColor("#FF9800"));
        } else if (damagePercentage > 0.75) {
            healthLayout.setBackgroundColor(Color.parseColor("#F44336"));
        }
    }

    public void updateShield(float shieldStrength) {
        TextView shieldTextView = findViewById(R.id.shieldTextView);
        shieldTextView.setText(String.valueOf((int) shieldStrength));
    }

    public void finishGameConnectionLost() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Połączenie zerwane");
        alertDialog.setMessage("Połączenie z przeciwnikiem zostało przerwane.");

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Ok", (dialog, which) -> finish());
        alertDialog.show();
    }
}
