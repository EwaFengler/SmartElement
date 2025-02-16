package com.example.smartelement;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

public class GameWrapper {


    private final String MESSAGE_FINISH = "finish";
    private final String MESSAGE_ATTACK_PREFIX = "attack_";

    final PlayerStatus playerStatus = new PlayerStatus();
    private final GameActivity gameActivity;
    private final BluetoothChatService bluetoothChatService;

    boolean gameOver = false;

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
                onBluetooth(message);
            } else if (msg.what == BluetoothChatService.MESSAGE_CONNECTION_LOST) {
                gameActivity.finishGameConnectionLost();
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
        int shieldLoaded = playerStatus.getShieldLoaded();

        if (attackStrength > 0) {
            sendAttack(attackStrength);
        }
        if (shieldLoaded > 0) {
            gameActivity.playShieldSound();
            updateShield(playerStatus.getShieldStrength());
        }

        playerStatus.reset();
    }

    public synchronized void onBluetooth(String message) {
        if (message.equals(MESSAGE_FINISH) && !gameOver) {
            gameOver = true;
            gameActivity.playWinSound();
            finishGame(GameResult.WIN);
        } else if (message.startsWith(MESSAGE_ATTACK_PREFIX)) {
            float damage = 0;
            try {
                damage = Float.parseFloat(message.split(MESSAGE_ATTACK_PREFIX)[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            playerStatus.receiveDamage(damage);
            updateShield(playerStatus.getShieldStrength());
            updateHealth(playerStatus.getDamagePercentage());

            if (playerStatus.isOver() && !gameOver) {
                gameOver = true;
                gameActivity.playLoseSound();
                finishGame(GameResult.LOSE);
            } else {
                gameActivity.playDamageSound();
            }
        }
    }

    public void sendAttack(float attackStrength) {
        if (!gameOver) {
            bluetoothChatService.sendMessage(MESSAGE_ATTACK_PREFIX + attackStrength);
        }
    }

    public synchronized void finishGame(GameResult gameResult) {
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
