package com.example.smartelement;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class GameWrapper {


    private final String MESSAGE_FINISH = "finish";
    private final String MESSAGE_ATTACK_PREFIX = "attack_";

    PlayerStatus playerStatus = new PlayerStatus();
    private GameActivity gameActivity;
    private BluetoothChatService bluetoothChatService;

    public GameWrapper(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
        bluetoothChatService = BluetoothChatService.getInstance();
        bluetoothChatService.setHandler(handler);

    }


    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BluetoothChatService.MESSAGE_READ) {
                byte[] readBuf = (byte[]) msg.obj;
                String message = new String(readBuf, 0, msg.arg1);
                Toast.makeText(gameActivity, message, Toast.LENGTH_SHORT).show();
                onBluetooth(message);
            }
        }
    };

    public void onAttack() {
        playerStatus.loadAttack();
    }

    public void onShield() {
        playerStatus.loadShield();
    }

    public void onExecute() {
        float attackStrength = playerStatus.execute();
        if (attackStrength > 0) {
            sendAttack(attackStrength);
        } else {
            updateShield(playerStatus.getShieldStrength());
        }
    }

    public void onBluetooth(String message) {
        if (message.equals(MESSAGE_FINISH)) {
            finishGame(GameResult.WIN);
        } else if (message.startsWith(MESSAGE_ATTACK_PREFIX)) {
            float damage = Float.parseFloat(message.split(MESSAGE_ATTACK_PREFIX)[1]);
            playerStatus.receiveDamage(damage);
            updateShield(playerStatus.getShieldStrength());
            updateHealth(playerStatus.getDamagePercentage());

            if (playerStatus.isOver()) {
                finishGame(GameResult.LOSE);
            }
        }
    }

    public void sendAttack(float attackStrength) {
        bluetoothChatService.sendMessage(MESSAGE_ATTACK_PREFIX + attackStrength);
    }

    public void finishGame(GameResult gameResult) {
        if (gameResult == GameResult.LOSE) {
            bluetoothChatService.sendMessage(MESSAGE_FINISH);
        }
        gameActivity.finishGame(gameResult);
    }

    private void updateHealth(float damagePercentage) {
        gameActivity.updateHealth(damagePercentage);
    }

    private void updateShield(float shieldStrength) {
        gameActivity.updateShield(shieldStrength);
    }
}
