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
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView levelTextView;
    private TextView scoreTextView;
    private ImageView heart1ImageView;
    private ImageView heart2ImageView;
    private ImageView heart3ImageView;
    private TextView instructionTextView;
    private Button startButton;

    private MediaPlayer mediaPlayerBackground;
    private MediaPlayer mediaPlayerPress;
    private MediaPlayer mediaPlayerSwipe;
    private MediaPlayer mediaPlayerShake;
    private MediaPlayer mediaPlayerDefeat;
    private MediaPlayer mediaPlayerGame;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean shakeDetected;
    private boolean shakeInProgress;
    private boolean shakeActionCompleted;

    private int currentLevel;
    private int currentScore;
    private int lives;
    private boolean gameStarted;
    private boolean gameOver;

    private Random random;
    private List<String> instructions;
    private int currentInstructionIndex;
    private SharedPreferences sharedPreferences;

    private GestureDetector gestureDetector;

    private static final int MAX_TIMER_SECONDS = 10;
    private static final int MIN_TIMER_SECONDS = 4;
    private static final int SETTINGS_REQUEST_CODE = 1;

    private int currentTimerSeconds;
    private GestureType gestureType;

    private enum GestureType {
        PRESS,
        SWIPE,
        SHAKE,
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        levelTextView = findViewById(R.id.levelTextView);
        scoreTextView = findViewById(R.id.scoreTextView);

        heart1ImageView = findViewById(R.id.heart1ImageView);
        heart2ImageView = findViewById(R.id.heart2ImageView);
        heart3ImageView = findViewById(R.id.heart3ImageView);
        instructionTextView = findViewById(R.id.instructionTextView);
        startButton = findViewById(R.id.startButton);

        mediaPlayerBackground = MediaPlayer.create(this, R.raw.fondo);
        mediaPlayerPress = MediaPlayer.create(this, R.raw.apretar);
        mediaPlayerSwipe = MediaPlayer.create(this, R.raw.deslizar);
        mediaPlayerShake = MediaPlayer.create(this, R.raw.agita);
        mediaPlayerDefeat = MediaPlayer.create(this, R.raw.lose);
        mediaPlayerGame = MediaPlayer.create(this, R.raw.defeat);

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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String gameMode = sharedPreferences.getString("game_mode", "normal");
        Button btnLeyenda = findViewById(R.id.btn_leyenda);
        Button btnPreferencias = findViewById(R.id.Preferences);

        if (gameMode.equals("normal")) {
            btnLeyenda.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);
        } else {
            btnLeyenda.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.GONE);
        }



        btnLeyenda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, leyenda.class);
                startActivity(intent);
            }
        });

        btnPreferencias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

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
        lives = 3;
        gameStarted = true;
        gameOver = false;
        Button btnPreferencias = findViewById(R.id.Preferences);
        btnPreferencias.setVisibility(View.GONE);

        updateLevelTextView();
        updateScoreTextView();
        updateLivesImageViews();

        startButton.setEnabled(false);

        playBackgroundMusic();

        startNewRound();
    }

    private void startNewRound() {
        if (!gameOver) {
            currentInstructionIndex = random.nextInt(instructions.size());
            String instruction = instructions.get(currentInstructionIndex);
            String instructionSound = getInstructionSound(instruction);

            instructionTextView.setText(instruction);
            playInstructionSound(instructionSound);

            // Actualizar el nivel y la puntuación en la interfaz de usuario
            updateLevelTextView();
            updateScoreTextView();

            // Configurar el temporizador
            currentTimerSeconds = MAX_TIMER_SECONDS - (currentLevel - 1);
            startTimer();
        }
    }

    private void checkAction(boolean actionCompleted) {
        if (!gameOver && !shakeInProgress) {
            boolean correctAction = actionCompleted && isCorrectAction();

            if (correctAction) {
                currentScore++;
                updateScoreTextView();
            } else {
                lives--;
                updateLivesImageViews();

                if (lives <= 0) {
                    endGame();
                    return;
                }
            }

            // Actualizar el nivel en la interfaz de usuario
            currentLevel++;
            updateLevelTextView();

            // Comenzar un nuevo nivel
            startNewRound();
        }
    }

    private boolean isCorrectAction() {
        String currentInstruction = instructions.get(currentInstructionIndex);
        if (currentInstruction.equals(getString(R.string.instruction_shake))) {
            return shakeActionCompleted;
        } else {
            return currentInstruction.equals(getString(R.string.instruction_press)) && gestureType == GestureType.PRESS ||
                    currentInstruction.equals(getString(R.string.instruction_swipe)) && gestureType == GestureType.SWIPE;
        }
    }

    private void endGame() {
        gameStarted = false;
        gameOver = true;
        Button btnPreferencias = findViewById(R.id.Preferences);
        btnPreferencias.setVisibility(View.VISIBLE);

        shakeDetected = false;
        shakeInProgress = false;

        mediaPlayerBackground.stop();
        mediaPlayerDefeat.start();

        instructionTextView.setText(getString(R.string.game_over));

        startButton.setEnabled(true);
        startButton.setText(getString(R.string.start));

        resetLivesImageViews();
        stopTimer();
        mediaPlayerBackground.stop();

        // Obtener el puntaje más alto guardado en las preferencias
        int highestScore = sharedPreferences.getInt("high_score_carrera", 0);

        // Comparar el puntaje actual con el puntaje más alto
        if (currentScore > highestScore) {
            // Actualizar el puntaje más alto en las preferencias
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("high_score_carrera", currentScore);
            editor.apply();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            // Actualizar la vista de MainActivity aquí
        }
    }


    private void updateLevelTextView() {
        levelTextView.setText(getString(R.string.level, currentLevel));
    }

    private void updateScoreTextView() {
        scoreTextView.setText(getString(R.string.score, currentScore));
    }

    private void updateLivesImageViews() {
        if (lives == 3) {
            heart1ImageView.setImageResource(R.drawable.heart_full);
            heart2ImageView.setImageResource(R.drawable.heart_full);
            heart3ImageView.setImageResource(R.drawable.heart_full);
        } else if (lives == 2) {
            heart1ImageView.setImageResource(R.drawable.heart_empty);
            heart2ImageView.setImageResource(R.drawable.heart_full);
            heart3ImageView.setImageResource(R.drawable.heart_full);
        } else if (lives == 1) {
            heart1ImageView.setImageResource(R.drawable.heart_empty);
            heart2ImageView.setImageResource(R.drawable.heart_empty);
            heart3ImageView.setImageResource(R.drawable.heart_full);
        } else {
            heart1ImageView.setImageResource(R.drawable.heart_empty);
            heart2ImageView.setImageResource(R.drawable.heart_empty);
            heart3ImageView.setImageResource(R.drawable.heart_empty);
        }
    }

    private void resetLivesImageViews() {
        heart1ImageView.setImageResource(R.drawable.heart_full);
        heart2ImageView.setImageResource(R.drawable.heart_full);
        heart3ImageView.setImageResource(R.drawable.heart_full);
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

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (gameStarted && !mediaPlayerBackground.isPlaying()) {
            mediaPlayerBackground.start();
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String gameMode = sharedPreferences.getString("game_mode", "normal");
        Button btnLeyenda = findViewById(R.id.btn_leyenda);
        Button btnPreferencias = findViewById(R.id.Preferences);

        if (gameMode.equals("normal")) {
            btnLeyenda.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);
        } else {
            btnLeyenda.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.GONE);
        }



    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        mediaPlayerBackground.pause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
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

    private void startTimer() {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (shakeInProgress) {
                    shakeInProgress = false;
                    shakeActionCompleted = false;
                    checkAction(false);
                }
            }
        }, currentTimerSeconds * 1000);
    }

    private void stopTimer() {
        // Cancelar el temporizador actual
        // ...
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            gestureType = GestureType.PRESS;
            checkAction(true);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            gestureType = GestureType.SWIPE;
            checkAction(true);
            return true;
        }
    }


}
