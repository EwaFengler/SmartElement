package com.example.smartelement;

import org.tensorflow.lite.Interpreter;

public class MlpModel {
    private final Interpreter interpreter;

    public MlpModel(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public float[] run(float[] input) {
        float[][] output = {{0, 0, 0}};
        interpreter.run(input, output);
        return output[0];
    }
}
