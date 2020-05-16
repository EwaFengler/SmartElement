package com.example.smartelement;

import java.util.ArrayDeque;
import java.util.Deque;

public class SensorData {
    private final Deque<Float> data = new ArrayDeque<>();
    private float xSum = 0;
    private float ySum = 0;
    private float zSum = 0;
    private float xgSum = 0;
    private float ygSum = 0;
    private float zgSum = 0;

    private float pitchSum = 0;
    private float rollSum = 0;

    private float xPrev = 0;
    private float yPrev = 0;
    private float zPrev = 0;
    private float xgPrev = 0;
    private float ygPrev = 0;
    private float zgPrev = 0;
    private float pitchPrev = 0;
    private float rollPrev = 0;

    private int accelerationCounter = 0;
    private int gravityCounter = 0;
    private int magneticCounter = 0;

    private int DATA_SERIES = 3;
    private int SIZE = DATA_SERIES * 20;
    private boolean ready = false;

    public void addMagneticValues(float[] orientationAngles) {
        pitchSum += orientationAngles[1];
        rollSum += orientationAngles[2];
        magneticCounter++;
    }

    public void addAccelerationValues(float[] accelerometerReading) {
        xSum += accelerometerReading[0];
        ySum += accelerometerReading[1];
        zSum += accelerometerReading[2];
        accelerationCounter++;
    }

    public void addGravityValues(float[] gravityReading) {
        xgSum += gravityReading[0];
        ygSum += gravityReading[1];
        zgSum += gravityReading[2];
        gravityCounter++;
    }

    public void update() {
        synchronized (data) {
            addAveragedValues();
            removeOldestValues();
            if (data.size() == SIZE) {
                ready = true;
            }
        }
    }

    public void addAveragedValues() {
        if (accelerationCounter > 0) {
            xPrev = xSum / accelerationCounter;
            yPrev = ySum / accelerationCounter;
            zPrev = zSum / accelerationCounter;

            xgPrev = xgSum / gravityCounter;
            ygPrev = ygSum / gravityCounter;
            zgPrev = zgSum / gravityCounter;
        }
        data.add(xPrev);
        data.add(yPrev);
//        data.add(zPrev);

//        data.add(xgPrev);
//        data.add(ygPrev);
        data.add(zgPrev);

//        if (magneticCounter > 0) {
//            pitchPrev = pitchSum / magneticCounter;
//            rollPrev = rollSum / magneticCounter;
//        }
//        data.add(pitchPrev);
//        data.add(rollPrev);
    }

    public void removeOldestValues() {
        if (data.size() > SIZE) {
            for (int i = 0; i < DATA_SERIES; i++) {
                data.removeFirst();
            }
        }
    }

    public void resetSensorValues() {
        xSum = 0;
        ySum = 0;
        zSum = 0;

        xgSum = 0;
        ygSum = 0;
        zgSum = 0;

        pitchSum = 0;
        rollSum = 0;

        accelerationCounter = 0;
        gravityCounter = 0;
        magneticCounter = 0;
    }

    public Deque<Float> getData() {
        return data;
    }

    public boolean isDataReady() {
        return ready;
    }

    public float[] getDataArray() {
        float[] array = new float[data.size()];
        synchronized (data) {
            int i = 0;
            for (Float f : data) {
                array[i++] = f;
            }
            ready = false;
        }
        return array;
    }

}
