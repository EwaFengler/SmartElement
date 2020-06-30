package com.example.smartelement;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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

        GameResult gameResult = (GameResult) getIntent().getExtras().get("gameResult");
        if (gameResult.equals(GameResult.WIN)) {
            gameResultTextView.setText("Wygrałeś");
        } else {
            gameResultTextView.setText("Przegrałeś");
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BluetoothChatService.MESSAGE_READ) {
                byte[] readBuf = (byte[]) msg.obj;
                String message = new String(readBuf, 0, msg.arg1);
                Toast.makeText(FinishGameActivity.this, message, Toast.LENGTH_SHORT).show();
                if (message.equals(CountdownActivity.MESSAGE_START_GAME)) {
                    MainActivity.opponentAlreadyStarted = true;
                }
            } else if (msg.what == BluetoothChatService.MESSAGE_CONNECTION_LOST) {
                AlertDialog alertDialog = new AlertDialog.Builder(FinishGameActivity.this).create();
                alertDialog.setTitle("Połączenie zerwane");
                alertDialog.setMessage("Połączenie z przeciwnikiem zostało przerwane.");

                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Ok", (dialog, which) -> finish());
                alertDialog.show();
            }
        }
    };

    public void okButtonClicked(View view) {
        finish();
    }
}