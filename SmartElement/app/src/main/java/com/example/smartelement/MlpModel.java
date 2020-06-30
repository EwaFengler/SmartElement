package com.example.smartelement;

import android.util.Log;

import org.tensorflow.lite.Interpreter;

public class MlpModel {


    private Interpreter interpreter;

    public MlpModel(Interpreter interpreter) {
        if (interpreter == null) {
            Log.d("asdf", "NULL interpreter");
        }
        this.interpreter = interpreter;
    }

    public float[] run(float[] input) {
        float[][] output = {{0, 0, 0}};
        interpreter.run(input, output);
        return output[0];
    }
}
