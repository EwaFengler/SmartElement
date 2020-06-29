package com.example.smartelement;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FinishGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_game);

        TextView gameResultTextView = findViewById(R.id.gameResultTextView);

        GameResult gameResult = (GameResult) getIntent().getExtras().get("gameResult");
        if (gameResult.equals(GameResult.WIN)) {
            gameResultTextView.setText("Wygrałeś");
        } else {
            gameResultTextView.setText("Przegrałeś");
        }
    }

    public void okButtonClicked(View view) {
        finish();
    }
}