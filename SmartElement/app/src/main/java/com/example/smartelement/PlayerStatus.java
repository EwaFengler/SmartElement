package com.example.smartelement;

public class PlayerStatus {

    private static float MAX_HEALTH = 100;
    private static float BASIC_ATTACK = 5;
    private static float BASIC_SHIELD = 5;

    private float health = MAX_HEALTH;
    private int attackLoaded = 0;
    private int shieldLoaded = 0;
    private float shieldStrength = 0;
    private boolean over = false;

    public void loadAttack() {
        attackLoaded++;
        if (shieldLoaded > 0) {
            shieldLoaded = 0;
        }
    }

    public void loadShield() {
        shieldLoaded++;
        if (attackLoaded > 0) {
            attackLoaded = 0;
        }
    }

    public float execute() {
        float attackStrength = 0;

        if (attackLoaded > 0) {
            attackStrength = getAttackStrength();
        } else if (shieldLoaded > 0) {
            fortifyShield();
        }

        resetAttackLoaded();
        resetShieldLoaded();

        return attackStrength;
    }

    public void receiveDamage(float damage) {
        if (damage > shieldStrength) {
            shieldStrength = 0;
            health -= (damage - shieldStrength);

            if (health <= 0) {
                health = 0;
                over = true;
            }
        } else {
            shieldStrength = shieldStrength - damage;
        }
    }

    public float getDamagePercentage() {
        return 1 - (health / MAX_HEALTH);
    }

    synchronized public float getShieldStrength() {
        return shieldStrength;
    }

    public boolean isOver() {
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
