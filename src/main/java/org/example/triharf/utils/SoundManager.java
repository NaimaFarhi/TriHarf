package org.example.triharf.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class SoundManager {

    private static MediaPlayer victoryPlayer;
    private static MediaPlayer defeatPlayer;

    public SoundManager() {
        try {
            // Charger les fichiers audio (Correction des chemins)
            var victoryRes = getClass().getResource("/sounds/success - Sound Effect.wav");
            var defeatRes = getClass().getResource("/sounds/Lose.wav");

            if (victoryRes != null) {
                Media victoryMedia = new Media(victoryRes.toExternalForm());
                victoryPlayer = new MediaPlayer(victoryMedia);
                victoryPlayer.setVolume(0.7);
            }
            
            if (defeatRes != null) {
                Media defeatMedia = new Media(defeatRes.toExternalForm());
                defeatPlayer = new MediaPlayer(defeatMedia);
                defeatPlayer.setVolume(0.7);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des sons: " + e.getMessage());
        }
    }

    /**
     * Jouer le son de victoire
     */
    public static void playVictory() {
        if (victoryPlayer != null) {
            victoryPlayer.stop();
            victoryPlayer.play();
        }
    }

    /**
     * Jouer le son de défaite
     */
    public static void playDefeat() {
        if (defeatPlayer != null) {
            defeatPlayer.stop();
            defeatPlayer.play();
        }
    }

    /**
     * Arrêter tous les sons
     */
    public static void stopAll() {
        if (victoryPlayer != null) victoryPlayer.stop();
        if (defeatPlayer != null) defeatPlayer.stop();
    }

    /**
     * Régler le volume (0.0 à 1.0)
     */
    public static void setVolume(double volume) {
        if (victoryPlayer != null) victoryPlayer.setVolume(volume);
        if (defeatPlayer != null) defeatPlayer.setVolume(volume);
    }
}