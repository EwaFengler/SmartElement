package com.example.smartelement;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class FinishGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_game);

        TextView gameResultTextView = findViewById(R.id.gameResultTextView);

        BluetoothChatService bluetoothChatService = BluetoothChatService.getInstance();
        bluetoothChatService.setHandler(handler);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            GameResult gameResult = (GameResult) extras.get("gameResult");
            if (gameResult != null) {
                if (gameResult.equals(GameResult.WIN)) {
                    gameResultTextView.setText(R.string.finish_won);
                } else {
                    gameResultTextView.setText(R.string.finish_lose);
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BluetoothChatService.MESSAGE_READ) {
                byte[] readBuf = (byte[]) msg.obj;
                String message = new String(readBuf, 0, msg.arg1);
                if (message.equals(CountdownActivity.MESSAGE_START_GAME)) {
                    MainActivity.opponentAlreadyStarted = true;
                }
            } else if (msg.what == BluetoothChatService.MESSAGE_CONNECTION_LOST) {
                AlertDialog alertDialog = new AlertDialog.Builder(FinishGameActivity.this).create();
                alertDialog.setTitle(getString(R.string.alert_title_conncetion_lost));
                alertDialog.setMessage(getString(R.string.alert_message_connection_lost));

                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Ok", (dialog, which) -> finish());
                alertDialog.show();
            }
        }
    };

    public void okButtonClicked(View view) {
        finish();
    }
}