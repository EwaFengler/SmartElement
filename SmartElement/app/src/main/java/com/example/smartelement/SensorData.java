package com.example.smartelement;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.Deque;

public class SensorData {
    private Deque<Float> data = new ArrayDeque<>();
    private float xSum = 0;
    private float ySum = 0;
    private float zSum = 0;
    private float pitchSum = 0;
    private float rollSum = 0;

    private float xPrev = 0;
    private float yPrev = 0;
    private float zPrev = 0;
    private float pitchPrev = 0;
    private float rollPrev = 0;

    private int accelerationCounter = 0;
    private int magneticCounter = 0;

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

    public void addAveragedValues() {
        if (accelerationCounter > 0) {
            xPrev = xSum / accelerationCounter;
            yPrev = ySum / accelerationCounter;
            zPrev = zSum / accelerationCounter;
        }
        data.add(xPrev);
        data.add(yPrev);
        data.add(zPrev);

        if (magneticCounter > 0) {
            pitchPrev = pitchSum / magneticCounter;
            rollPrev = rollSum / magneticCounter;
        }
        data.add(pitchPrev);
        data.add(rollPrev);
    }

    public void removeOldestValues() {
        if (data.size() > 100) {
            for (int i = 0; i < 5; i++) {
                data.removeFirst();
            }
        }
    }

    public void resetSensorValues() {
        xSum = 0;
        ySum = 0;
        zSum = 0;
        pitchSum = 0;
        rollSum = 0;
        accelerationCounter = 0;
        magneticCounter = 0;
    }

    public Deque<Float> getData() {
        return data;
    }

    public float[] getDataArray(){
        float[] array = new float[data.size()];
        int i = 0;
        for (Float f : data) {
            array[i++] = f;
        }
        return array;
    }
}
