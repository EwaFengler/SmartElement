package com.example.smartelement;

import java.util.ArrayDeque;
import java.util.Deque;

public class SensorData {
    private final Deque<Float> data = new ArrayDeque<>();
    private float xSum = 0;
    private float ySum = 0;
    private float zgSum = 0;

    private float xPrev = 0;
    private float yPrev = 0;
    private float zgPrev = 0;

    private int accelerationCounter = 0;
    private int gravityCounter = 0;

    private final int DATA_SERIES = 3;
    private final int SIZE = DATA_SERIES * 20;
    private boolean ready = false;

    public void addAccelerationValues(float[] accelerometerReading) {
        xSum += accelerometerReading[0];
        ySum += accelerometerReading[1];
        accelerationCounter++;
    }

    public void addGravityValues(float[] gravityReading) {
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

            zgPrev = zgSum / gravityCounter;
        }
        data.add(xPrev);
        data.add(yPrev);
        data.add(zgPrev);
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
        zgSum = 0;

        accelerationCounter = 0;
        gravityCounter = 0;
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
