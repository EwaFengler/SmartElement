package com.example.smartelement;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;

public class BluetoothDevicesActivity extends AppCompatActivity {

    public static final int RESULT_ADDRESS = 1;
    public static final int RESULT_OPPONENT_NAME = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_devices);

        BluetoothChatService bluetoothChatService = BluetoothChatService.getInstance();
        bluetoothChatService.setHandler(handler);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ArrayAdapter<String> pairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(chooseDevice);


        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesArrayAdapter.add(getString(R.string.no_paired_devices));
        }
    }

    private final AdapterView.OnItemClickListener chooseDevice
            = (av, v, arg2, arg3) -> {
        String info = ((TextView) v).getText().toString();
        String address = info.substring(info.length() - 17);

        Intent intent = new Intent();
        intent.putExtra("MAC_address", address);
        setResult(RESULT_ADDRESS, intent);
        finish();
    };

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BluetoothChatService.MESSAGE_DEVICE_NAME) {
                String opponentName = msg.getData().getString("device_name");
                Intent intent = new Intent();
                intent.putExtra("opponentName", opponentName);
                setResult(RESULT_OPPONENT_NAME, intent);
                finish();
            }
        }
    };

    public void openBluetoothSettings(View view) {
        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intentOpenBluetoothSettings);
    }
}
