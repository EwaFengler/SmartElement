package com.example.smartelement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class CountdownActivity extends AppCompatActivity {

    public static final String MESSAGE_START_GAME = "start";
    private TextView countdownInfoTextView;
    private TextView countdownTimerTextView;

    private boolean countdownStarted = false;
    private CountDownTimer countdownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        countdownInfoTextView = findViewById(R.id.countdownInfoTextView);
        countdownTimerTextView = findViewById(R.id.countdownTimerTextView);

        BluetoothChatService bluetoothChatService = BluetoothChatService.getInstance();
        bluetoothChatService.setHandler(handler);

        Bundle extras = getIntent().getExtras();
        if (extras.getBoolean("opponentAlreadyStarted")) {
            startCountdownTimer();
        }
    }

    @Override
    public void onBackPressed() {
        if (countdownStarted) {
            Toast.makeText(getApplicationContext(), "Z tego miejsca nie ma powrotu", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }


    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BluetoothChatService.MESSAGE_READ) {
                byte[] readBuf = (byte[]) msg.obj;
                String message = new String(readBuf, 0, msg.arg1);
                if (message.equals(MESSAGE_START_GAME)) {
                    startCountdownTimer();
                }
            } else if (msg.what == BluetoothChatService.MESSAGE_CONNECTION_LOST) {
                if(countdownTimer != null) {
                    countdownTimer.cancel();
                }

                AlertDialog alertDialog = new AlertDialog.Builder(CountdownActivity.this).create();
                alertDialog.setTitle("Połączenie zerwane");
                alertDialog.setMessage("Połączenie z przeciwnikiem zostało przerwane.");

                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Ok", (dialog, which) -> finish());
                alertDialog.show();
            }
        }
    };

    private void startCountdownTimer() {
        countdownInfoTextView.setText("Gra rozpocznie się za:");
        countdownStarted = true;

        countdownTimer = new CountDownTimer(5100, 1000) {
            public void onTick(long millisUntilFinished) {
                countdownTimerTextView.setText(String.valueOf(millisUntilFinished / 1000));
            }

            public void onFinish() {
                Intent i = new Intent(CountdownActivity.this, GameActivity.class);
                startActivity(i);
                finish();
            }
        }.start();
    }
}