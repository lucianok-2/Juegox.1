package com.example.juego;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class leyenda extends AppCompatActivity implements SensorEventListener {

    private TextView levelTextView;
    private TextView scoreTextView;
    private ImageView heartImageView;
    private TextView instructionTextView;
    private Button startButton;

    private MediaPlayer mediaPlayerBackground;
    private MediaPlayer mediaPlayerPress;
    private MediaPlayer mediaPlayerSwipe;
    private MediaPlayer mediaPlayerShake;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean shakeDetected;
    private boolean shakeInProgress;
    private boolean shakeActionCompleted;

    private Random random;
    private List<String> instructions;
    private int currentLevel;
    private int currentScore;
    private int lives;
    private boolean gameStarted;

    private GestureDetector gestureDetector;

    private static final int MAX_TIMER_SECONDS = 4;

    private enum GestureType {
        PRESS,
        SWIPE,
        SHAKE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leyenda);

        levelTextView = findViewById(R.id.levelTextView_1);
        scoreTextView = findViewById(R.id.scoreTextView_1);
        heartImageView = findViewById(R.id.heartImageView);
        instructionTextView = findViewById(R.id.instructionTextView);
        startButton = findViewById(R.id.startButton_2);

        mediaPlayerBackground = MediaPlayer.create(this, R.raw.fondo2_);
        mediaPlayerPress = MediaPlayer.create(this, R.raw.apretar);
        mediaPlayerSwipe = MediaPlayer.create(this, R.raw.deslizar);
        mediaPlayerShake = MediaPlayer.create(this, R.raw.agita);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        shakeDetected = false;
        shakeInProgress = false;
        shakeActionCompleted = false;

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

        gestureDetector = new GestureDetector(this, new GestureListener());
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

        playBackgroundMusic();

        startNewRound();
    }

    private void startNewRound() {
        String instruction = instructions.get(random.nextInt(instructions.size()));
        instructionTextView.setText(instruction);
        playInstructionSound(getInstructionSound(instruction));

        updateLevelTextView();
        updateScoreTextView();

        startTimer();
    }

    private void checkAction(boolean actionCompleted) {
        if (!shakeInProgress) {
            boolean correctAction = actionCompleted && isCorrectAction();

            if (correctAction) {
                currentScore++;
                updateScoreTextView();
                currentLevel++;
                updateLevelTextView();
                startNewRound();
            } else {
                lives--;
                updateLivesImageView();
                if (lives <= 0) {
                    endGame();
                }
            }
        }
    }

    private boolean isCorrectAction() {
        String currentInstruction = instructionTextView.getText().toString();
        if (currentInstruction.equals(getString(R.string.instruction_shake))) {
            return shakeActionCompleted;
        } else if (currentInstruction.equals(getString(R.string.instruction_press))) {
            return gestureDetector.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0, 0, 0));
        } else if (currentInstruction.equals(getString(R.string.instruction_swipe))) {
            return gestureDetector.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0))
                    && gestureDetector.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 100, 0, 0));
        } else {
            return false;
        }
    }

    private String getInstructionSound(String instruction) {
        if (instruction.equals(getString(R.string.instruction_press))) {
            return "press";
        } else if (instruction.equals(getString(R.string.instruction_swipe))) {
            return "swipe";
        } else if (instruction.equals(getString(R.string.instruction_shake))) {
            return "shake";
        } else {
            return "";
        }
    }

    private void playInstructionSound(String sound) {
        if (sound.equals("press")) {
            mediaPlayerPress.start();
        } else if (sound.equals("swipe")) {
            mediaPlayerSwipe.start();
        } else if (sound.equals("shake")) {
            mediaPlayerShake.start();
        }
    }

    private void playBackgroundMusic() {
        mediaPlayerBackground.setLooping(true);
        mediaPlayerBackground.start();
    }

    private void stopBackgroundMusic() {
        mediaPlayerBackground.stop();
        mediaPlayerBackground.release();
    }

    private void startTimer() {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAction(false);
            }
        }, MAX_TIMER_SECONDS * 1000);
    }

    private void stopTimer() {
        // Implementa tu lógica para detener el temporizador aquí
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

    private void endGame() {
        gameStarted = false;
        startButton.setEnabled(true);
        startButton.setText(getString(R.string.start));

        stopTimer();
        stopBackgroundMusic();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (gameStarted && !mediaPlayerBackground.isPlaying()) {
            mediaPlayerBackground.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        mediaPlayerBackground.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBackgroundMusic();
        mediaPlayerPress.release();
        mediaPlayerSwipe.release();
        mediaPlayerShake.release();
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];

            if (x < -5 && !shakeDetected) {
                shakeDetected = true;
            } else if (x > 5 && shakeDetected) {
                shakeDetected = false;
                shakeInProgress = true;
                shakeActionCompleted = true;
                checkAction(true);
                startTimer();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No se utiliza en este código
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }
    }
}