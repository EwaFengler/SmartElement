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
import android.media.MediaPlayer;
import android.os.Bundle;
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
    private final float[] linearAccelerationReading = new float[3];
    private final float[] gravityReading = new float[3];

    private long lastMoment = 0;

    private final Object dataMonitor = new Object();
    private GameWrapper gameWrapper;

    private MediaPlayer shieldSound;
    private MediaPlayer damageSound;
    private MediaPlayer winSound;
    private MediaPlayer loseSound;

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

        shieldSound = MediaPlayer.create(this, R.raw.shield);
        damageSound = MediaPlayer.create(this, R.raw.damage);
        winSound = MediaPlayer.create(this, R.raw.win);
        loseSound = MediaPlayer.create(this, R.raw.lose);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (dataMonitor) {
                    if (sensorData.isDataReady()) {
                        float[] input = sensorData.getDataArray();
                        float[] output = mlpModel.run(input);

                        runOnUiThread(() -> {
                            if (output[0] > 0.90) {
                                gameWrapper.onShield();
                            }
                            if (output[1] > 0.90) {
                                gameWrapper.onAttack();
                            }
                            if (output[2] > 0.90) {
                                gameWrapper.onExecute();
                            }
                        });
                    }
                }
            }
        }, new Date(), 200);
    }

    private void loadModel() throws IOException {
        String modelFilename = "ConvertedModel/model_xy_zgravity.tflite";
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
        alertDialog.setTitle(getString(R.string.alert_title_return));
        alertDialog.setMessage(getString(R.string.alert_message_return_are_you_sure));

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.btn_keep_playing), ((dialog, which) -> {
        }));
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.alert_resign), (dialog, which) -> {
            if (!gameWrapper.gameOver)
                gameWrapper.gameOver = true;
            gameWrapper.finishGame(GameResult.LOSE);
        });

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

    public synchronized void finishGame(GameResult gameResult) {
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

    public void playShieldSound() {
        shieldSound.start();
    }

    public void playDamageSound() {
        damageSound.start();
    }

    public void playWinSound() {
        winSound.start();
    }

    public void playLoseSound() {
        loseSound.start();
    }

    public synchronized void finishGameConnectionLost() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.alert_title_conncetion_lost));
        alertDialog.setMessage(getString(R.string.alert_message_connection_lost));

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Ok", (dialog, which) -> finish());
        alertDialog.show();
    }
}
