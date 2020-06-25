package com.example.smartelement;

public class GameWrapper {

    PlayerStatus playerStatus;
    private GameActivity gameActivity;

    public GameWrapper(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
    }

    public void onAttack() {
        playerStatus.loadAttack();
    }

    public void onShield() {
        playerStatus.loadShield();
    }

    public void onExecute() {
        playerStatus.execute();
    }

    public void onBluetooth() {
        boolean isAttack = true; //TODO
        boolean isFinish = false; //TODO

        if (isFinish) {
            finishGame(GameResult.WIN);
        }

        if (isAttack) {
            float damage = 0; //TODO
            playerStatus.receiveDamage(damage);
            updateHealth(playerStatus.getDamagePercentage());

            if (playerStatus.isOver()) {
                finishGame(GameResult.LOSE);
            }
        }
    }

    private void finishGame(GameResult gameResult) {
        gameActivity.finishGame(gameResult);
    }

    private void updateHealth(float damagePercentage) {
        gameActivity.updateHealth(damagePercentage);
    }
}
