package com.example.smartelement;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_DEVICES_ACTIVITY = 2;

    public static boolean opponentAlreadyStarted = false;

    private BluetoothChatService bluetoothChatService = null;

    private Button newGameButton;
    private Button chooseOpponentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        newGameButton = findViewById(R.id.newGameButton);
        chooseOpponentButton = findViewById(R.id.chooseOpponent);


    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (sensorsAvailable()) {
//            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//            if (!bluetoothAdapter.isEnabled()) {
//                activateBluetooth();
//            } else {
//                setupChat();
//            }
//        } else {
//            alertSensorNotAvailable();
//        }
//    }

    private boolean sensorsAvailable() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        boolean linearAcc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null;
        boolean gravityAcc = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null;

        return linearAcc && gravityAcc;
    }

    private void alertSensorNotAvailable() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(getString(R.string.alert_title_sensor_unavailable));
        alertDialog.setMessage(getString(R.string.alert_message_sensor_unavailable));

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.alert_btn_close_game), (dialog, which) -> closeGame());

        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sensorsAvailable()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!bluetoothAdapter.isEnabled()) {
                alertBluetoothRequired();
            } else {
                setupChat();
            }
        } else {
            alertSensorNotAvailable();
        }

        if (bluetoothChatService != null) {
            if (bluetoothChatService.getState() == BluetoothChatService.STATE_NONE) {
                bluetoothChatService.start();
            }

            if (bluetoothChatService.getState() == BluetoothChatService.STATE_CONNECTED) {
                opponentConnected();
            } else {
                opponentDisconnected();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothChatService != null) {
            bluetoothChatService.stop();
        }
    }

    private void activateBluetooth() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    public void chooseOpponent(View view) {
        bluetoothChatService.start();
        opponentAlreadyStarted = false;
        Intent i = new Intent(this, BluetoothDevicesActivity.class);
        startActivityForResult(i, REQUEST_BLUETOOTH_DEVICES_ACTIVITY);
    }

    public void startNewGame(View view) {
        bluetoothChatService.sendMessage(CountdownActivity.MESSAGE_START_GAME);
        Intent i = new Intent(this, CountdownActivity.class);
        i.putExtra("opponentAlreadyStarted", opponentAlreadyStarted);
        opponentAlreadyStarted = false;
        startActivity(i);

    }

    private void closeGame() {
        this.finish();
    }

    private void setupChat() {
        bluetoothChatService = BluetoothChatService.getInstance();
        bluetoothChatService.setHandler(handler);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void opponentConnected() {
        newGameButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_background));
        newGameButton.setEnabled(true);
        chooseOpponentButton.setText(R.string.change_opponent);
    }

    private void opponentDisconnected() {
        newGameButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_background_disabled));
        newGameButton.setText(R.string.btn_new_game);
        newGameButton.setEnabled(false);
        chooseOpponentButton.setText(R.string.choose_opponent);
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothChatService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    if (readMessage.equals(CountdownActivity.MESSAGE_START_GAME)) {
                        opponentAlreadyStarted = true;
                    }
                    break;
                case BluetoothChatService.MESSAGE_DEVICE_NAME:
                    String opponentName = msg.getData().getString("device_name");
                    newGameButton.setText(getString(R.string.btn_new_game_with_name, opponentName));
                    opponentConnected();
                    break;
                case BluetoothChatService.MESSAGE_CONNECTION_FAILED:
                    toast(getString(R.string.toast_connection_failed));
                    break;
                case BluetoothChatService.MESSAGE_CONNECTION_LOST:
                    toast(getString(R.string.toast_connection_lost));
                    opponentDisconnected();
                    break;
            }
        }
    };

    private void alertBluetoothRequired() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.alert_title_bluetooth_request));
        alertDialog.setMessage(getString(R.string.alert_message_bluetooth_request));

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.OK), (dialog, which) -> activateBluetooth());
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.alert_btn_close_game), (dialog, which) -> closeGame());

        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_BLUETOOTH_DEVICES_ACTIVITY:
                if (resultCode == BluetoothDevicesActivity.RESULT_ADDRESS) {
                    String opponentAddress = data.getStringExtra("MAC_address");
                    bluetoothChatService.connect(opponentAddress, false);
                } else if (resultCode == BluetoothDevicesActivity.RESULT_OPPONENT_NAME) {
                    String opponentName = data.getStringExtra("opponentName");
                    newGameButton.setText(getString(R.string.btn_new_game_with_name, opponentName));
                    opponentConnected();
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    setupChat();
                }
        }
    }

    public void showProfile(View view) {
        Intent i = new Intent(this, ProfileActivity.class);
        startActivity(i);
    }
}
