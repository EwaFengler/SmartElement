package com.example.smartelement;


import android.util.Log;
import org.tensorflow.lite.Interpreter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MlpModel {


    private Interpreter interpreter;

    public MlpModel(Interpreter interpreter) {
        if (interpreter == null) {
            Log.d("asdf", "NULL interpreter");
        }
        this.interpreter = interpreter;
    }

    public void run(double[] input) {
        float[][] output = {{0, 0, 0}};
        interpreter.run(input, output);

        Log.d("asdf", Arrays.deepToString(output));

    }
}
