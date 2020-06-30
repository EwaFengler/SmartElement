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

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_DEVICES_ACTIVITY = 2;

    public static boolean opponentAlreadyStarted = false;

    private BluetoothChatService bluetoothChatService = null;

    private Button newGameButton;
    private Button chooseOpponentButton;

    private boolean DEV_MODE = false; //TODO wywalić

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        newGameButton = findViewById(R.id.newGameButton);
        chooseOpponentButton = findViewById(R.id.chooseOponent);

        if (sensorsAvailable()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!bluetoothAdapter.isEnabled()) {
                activateBluetooth();
            } else {
                setupChat();
            }
        } else {
            alertSensorNotAvailable();
        }
    }

    private boolean sensorsAvailable() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        boolean linearAcc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null;
        boolean gravityAcc = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null;

        return linearAcc && gravityAcc;
    }

    private void alertSensorNotAvailable() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Sensor niedostępny");
        alertDialog.setMessage("Przepraszamy, nie możesz zagrać w tą grę. Twoje urządzenie nie posiada odpowiednich sensorów.");

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Zamknij grę", (dialog, which) -> closeGame());

        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupChat();
        if (bluetoothChatService != null) {
            if (bluetoothChatService.getState() == BluetoothChatService.STATE_NONE) {
                bluetoothChatService.start();
            }

            if (bluetoothChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
                newGameButton.setEnabled(false);
                chooseOpponentButton.setText("Wybierz przeciwnika");
            }
        }

        if (DEV_MODE) {
            newGameButton.setEnabled(true);
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
        if (DEV_MODE) {
            Intent i = new Intent(this, GameActivity.class);
            startActivity(i);
        } else {
            bluetoothChatService.sendMessage(CountdownActivity.MESSAGE_START_GAME);
            Intent i = new Intent(this, CountdownActivity.class);
            i.putExtra("opponentAlreadyStarted", opponentAlreadyStarted);
            opponentAlreadyStarted = false;
            startActivity(i);
        }
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

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothChatService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    toast(readMessage);
                    if (readMessage.equals(CountdownActivity.MESSAGE_START_GAME)) {
                        opponentAlreadyStarted = true;
                    }
                    break;
                case BluetoothChatService.MESSAGE_DEVICE_NAME:
                    String opponentName = msg.getData().getString("device_name");
                    toast("Twój przeciwnik to " + opponentName);
                    newGameButton.setEnabled(true);
                    chooseOpponentButton.setText("Zmień przeciwnika");
                    break;
                case BluetoothChatService.MESSAGE_CONNECTION_FAILED:
                    toast("Nie udało się połączyć z przeciwnikiem");
                    break;
                case BluetoothChatService.MESSAGE_CONNECTION_LOST:
                    toast("Połączenie zostało zerwane");
                    break;
            }
        }
    };

    private void alertBluetoothRequired() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Bluetooth");
        alertDialog.setMessage("Proszę, aktywuj Bluetooth");

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, which) -> activateBluetooth());
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Zamknij grę", (dialog, which) -> closeGame());

        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_BLUETOOTH_DEVICES_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    String opponentAddress = data.getStringExtra("MAC_address");
                    bluetoothChatService.connect(opponentAddress, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    setupChat();
                } else {
                    alertBluetoothRequired();
                }
        }
    }
}
