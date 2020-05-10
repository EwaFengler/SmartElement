package com.example.smartelement;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            AssetFileDescriptor assetFileDescriptor = getAssets().openFd("model.tflite");
            MlpModel mlpModel = new MlpModel(assetFileDescriptor.createInputStream());
            mlpModel.run();

        } catch (IOException e) {
            Log.e("asdf", e.getMessage());
        }
    }
}
