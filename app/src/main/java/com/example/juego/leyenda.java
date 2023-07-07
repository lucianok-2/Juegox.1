package com.example.juego;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class leyenda extends AppCompatActivity {

    private TextView levelTextView;
    private TextView scoreTextView;
    private ImageView heartImageView;
    private TextView instructionTextView;
    private Button startButton;

    private Random random;
    private List<String> instructions;
    private int currentLevel;
    private int currentScore;
    private int lives;
    private boolean gameStarted;

    private static final int MAX_TIMER_SECONDS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leyenda);

        levelTextView = findViewById(R.id.levelTextView);
        scoreTextView = findViewById(R.id.scoreTextView);

        instructionTextView = findViewById(R.id.instructionTextView);
        startButton = findViewById(R.id.startButton_2);
        ImageView heart1ImageView = findViewById(R.id.heart1ImageView);
        ImageView heart2ImageView = findViewById(R.id.heart2ImageView);
        ImageView heart3ImageView = findViewById(R.id.heart3ImageView);


        heart2ImageView.setImageResource(R.drawable.heart_empty);
        heart3ImageView.setImageResource(R.drawable.heart_empty);
        random = new Random();
        instructions = new ArrayList<>();
        instructions.add(getString(R.string.instruction_press));
        instructions.add(getString(R.string.instruction_swipe));
        instructions.add(getString(R.string.instruction_shake));

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!gameStarted) {
                    startGame();
                }
            }
        });
    }

    private void startGame() {
        currentLevel = 1;
        currentScore = 0;
        lives = 1;
        gameStarted = true;

        updateLevelTextView();
        updateScoreTextView();
        updateLivesImageView();

        startButton.setEnabled(false);

        startNewRound();
    }

    private void startNewRound() {
        String instruction = instructions.get(random.nextInt(instructions.size()));
        instructionTextView.setText(instruction);

        // Actualizar el nivel y la puntuación en la interfaz de usuario
        updateLevelTextView();
        updateScoreTextView();

        // Comenzar un nuevo nivel
        startTimer();
    }

    private void checkAction() {
        currentScore++;
        updateScoreTextView();

        // Actualizar el nivel en la interfaz de usuario
        currentLevel++;
        updateLevelTextView();

        // Comenzar un nuevo nivel
        startNewRound();
    }

    private void updateLevelTextView() {
        levelTextView.setText(getString(R.string.level, currentLevel));
    }

    private void updateScoreTextView() {
        scoreTextView.setText(getString(R.string.score, currentScore));
    }

    private void updateLivesImageView() {
        if (lives == 1) {
            heartImageView.setImageResource(R.drawable.heart_full);
        } else {
            heartImageView.setImageResource(R.drawable.heart_empty);
        }
    }

    private void startTimer() {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAction();
            }
        }, MAX_TIMER_SECONDS * 1000);
    }
}
