package org.example.triharf.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class SoundManager {

    private static MediaPlayer victoryPlayer;
    private static MediaPlayer defeatPlayer;

    public SoundManager() {
        try {
            // Charger les fichiers audio
            String victoryPath = getClass().getResource("/org/example/triharf/sounds/success - Sound Effect.wav").toExternalForm();
            String defeatPath = getClass().getResource("/org/example/triharf/sounds/Lose.wav").toExternalForm();

            Media victoryMedia = new Media(victoryPath);
            Media defeatMedia = new Media(defeatPath);

            victoryPlayer = new MediaPlayer(victoryMedia);
            defeatPlayer = new MediaPlayer(defeatMedia);

            // Volume par défaut (0.0 à 1.0)
            victoryPlayer.setVolume(0.7);
            defeatPlayer.setVolume(0.7);

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des sons: " + e.getMessage());
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