package com.example.smartelement;


import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class MlpModel {


    private Interpreter interpreter;

    public MlpModel(FileInputStream inputStream) throws IOException {
        FileChannel fileChannel = inputStream.getChannel();
        MappedByteBuffer tfliteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        interpreter = new Interpreter(tfliteModel);
    }

    public void run(){
        Object[] input = {4.6208150e-01,5.7939900e-01,9.6055375e+00,-6.0142500e-02,
                -4.8036500e-02,2.3941000e-02,6.5601300e-01,9.4930115e+00,
                -6.8888500e-02,-2.3410000e-03,2.9687500e-01,1.7237900e-01,
                1.0074799e+01,-1.7101000e-02,-2.9459000e-02,2.1545500e-02,
                2.2984300e-01,9.6342695e+00,-2.7264000e-02,-2.9650000e-03,
                1.6711580e+00,3.9121250e+00,8.8393860e+00,-4.1020500e-01,
                -1.8685300e-01,-7.4220300e-01,8.7915115e+00,1.1710037e+01,
                -6.4051950e-01,5.7878000e-02,-3.1363980e+00,1.2631805e+01,
                1.1793839e+01,-8.0263200e-01,2.5991900e-01,-8.9064000e-01,
                1.2871231e+01,1.2081139e+01,-8.0445500e-01,7.4513000e-02,
                6.3973080e+00,-9.0836030e+00,1.0929527e+01,5.7765200e-01,
                -5.2038350e-01,3.1363980e+00,-1.5011642e+01,9.2464140e+00,
                9.9412600e-01,-3.2702300e-01,-1.9345170e+00,-9.8138355e+00,
                1.2696457e+01,6.4065200e-01,1.6057350e-01,-1.0151370e+00,
                -3.5625760e+00,7.8098910e+00,4.2481200e-01,1.2925600e-01,
                1.1468200e+00,-1.4556805e+00,7.2424625e+00,1.9302050e-01,
                -1.5734150e-01,9.4331400e-01,1.2928800e-01,7.4028780e+00,
                -1.7323000e-02,-1.2674200e-01,7.4699400e-01,4.1419250e-01,
                9.0429005e+00,-4.4286000e-02,-8.1265500e-02,4.7910000e-03,
                1.9150000e-02,9.8162230e+00,-1.9510000e-03,-4.8800000e-04,
                2.3941800e-01,1.2449650e-01,9.6677855e+00,-1.2705500e-02,
                -2.4491000e-02,4.8123200e-01,4.9799350e-01,1.0060440e+01,
                -4.9343500e-02,-4.7742500e-02,6.2256000e-02,2.5378400e-01,
                9.7874910e+00,-2.5923000e-02,-6.3610000e-03,-1.8435650e-01,
                1.5322850e-01,1.0062828e+01,-1.5071500e-02,1.8699000e-02};
        Map<Integer, Object> output = new HashMap<>();

        interpreter.runForMultipleInputsOutputs(input, output);
        Log.d("asdf", output.toString());
    }
}
