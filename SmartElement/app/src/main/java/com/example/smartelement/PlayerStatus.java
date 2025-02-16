package com.example.smartelement;

public class PlayerStatus {

    private static final float MAX_HEALTH = 100;
    private static final float BASIC_ATTACK = 5;
    private static final float BASIC_SHIELD = 5;

    private static final float MIN_ATTACK = 3;
    private static final float MIN_SHIELD = 3;

    private float health = MAX_HEALTH;
    private int attackLoaded = 0;
    private int shieldLoaded = 0;
    private float shieldStrength = 0;
    private boolean over = false;

    public synchronized void loadAttack() {
        attackLoaded++;
    }

    public synchronized void loadShield() {
        shieldLoaded++;
    }

    public synchronized float execute() {
        float attackStrength = 0;

        if (attackLoaded > MIN_ATTACK) {
            attackStrength = getAttackStrength();
        } else {
            resetAttackLoaded();
        }
        if (shieldLoaded > MIN_SHIELD) {
            fortifyShield();
        } else {
            resetShieldLoaded();
        }

        return attackStrength;
    }

    public synchronized void receiveDamage(float damage) {
        if (damage > shieldStrength) {
            health -= (damage - shieldStrength);
            shieldStrength = 0;

            if (health <= 0) {
                health = 0;
                over = true;
            }
        } else {
            shieldStrength = shieldStrength - damage;
        }
    }

    public synchronized float getDamagePercentage() {
        return 1 - (health / MAX_HEALTH);
    }

    public synchronized float getShieldStrength() {
        return shieldStrength;
    }

    public int getShieldLoaded() {
        return shieldLoaded;
    }

    public void reset() {
        resetAttackLoaded();
        resetShieldLoaded();
    }

    public synchronized boolean isOver() {
        return over;
    }

    private float getAttackStrength() {
        float attackBase = BASIC_ATTACK * attackLoaded;
        return applyAttackBonus(attackBase);
    }

    private void fortifyShield() {
        float shieldBase = BASIC_SHIELD * shieldLoaded;
        shieldStrength += applyShieldBonus(shieldBase);
    }

    private void resetAttackLoaded() {
        attackLoaded = 0;
    }

    private void resetShieldLoaded() {
        shieldLoaded = 0;
    }

    private float applyAttackBonus(float attackBase) {
        return attackBase + attackLoaded - 1;
    }

    private float applyShieldBonus(float shieldBase) {
        return shieldBase + shieldLoaded - 1;
    }
}
