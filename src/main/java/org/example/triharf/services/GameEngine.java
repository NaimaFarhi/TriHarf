package org.example.triharf.services;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.Random;

public class GameEngine {
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random random = new Random();

    private Character currentLetter;
    private int remainingTime;
    private Timeline timer;
    private GameState state;
    private Runnable onTimerUpdate;
    private Runnable onGameEnd;

    public enum GameState {
        NOT_STARTED,
        IN_PROGRESS,
        PAUSED,
        FINISHED
    }

    public GameEngine() {
        this.state = GameState.NOT_STARTED;
    }

    public Character generateRandomLetter() {
        currentLetter = LETTERS.charAt(random.nextInt(LETTERS.length()));
        return currentLetter;
    }

    public void startTimer(int durationSeconds) {
        this.remainingTime = durationSeconds;
        this.state = GameState.IN_PROGRESS;

        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingTime--;
            if (onTimerUpdate != null) {
                onTimerUpdate.run();
            }
            if (remainingTime <= 0) {
                endGame();
            }
        }));
        timer.setCycleCount(durationSeconds);
        timer.play();
    }

    public void pauseTimer() {
        if (timer != null && state == GameState.IN_PROGRESS) {
            timer.pause();
            state = GameState.PAUSED;
        }
    }

    public void resumeTimer() {
        if (timer != null && state == GameState.PAUSED) {
            timer.play();
            state = GameState.IN_PROGRESS;
        }
    }

    public void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
        endGame();
    }

    private void endGame() {
        state = GameState.FINISHED;
        if (timer != null) {
            timer.stop();
        }
        if (onGameEnd != null) {
            onGameEnd.run();
        }
    }

    // Getters & Setters
    public Character getCurrentLetter() { return currentLetter; }
    public int getRemainingTime() { return remainingTime; }
    public GameState getState() { return state; }

    public void setOnTimerUpdate(Runnable callback) { this.onTimerUpdate = callback; }
    public void setOnGameEnd(Runnable callback) { this.onGameEnd = callback; }

    public String formatTime() {
        int minutes = remainingTime / 60;
        int seconds = remainingTime % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}