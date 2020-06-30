package com.example.smartelement;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView winTextView = findViewById(R.id.winTextView);
        TextView loseTextView = findViewById(R.id.loseTextView);
        TextView pointsTextView = findViewById(R.id.pointsTextView);

        SharedPreferences sharedPreferences = getSharedPreferences("pref", Context.MODE_PRIVATE);

        int win = sharedPreferences.getInt("win", 0);
        int lose = sharedPreferences.getInt("lose", 0);

        winTextView.setText(String.valueOf(win));
        loseTextView.setText(String.valueOf(lose));
        pointsTextView.setText(String.valueOf(win * 10 + lose));
    }
}
