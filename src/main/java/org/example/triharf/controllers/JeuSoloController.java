package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur pour le jeu solo (partie.fxml)
 */
public class JeuSoloController {

    @FXML
    private Label labelTimer;

    @FXML
    private Label labelScore;

    @FXML
    private TextField tfPays;

    @FXML
    private TextField tfAnimal;

    @FXML
    private TextField tfFruit;

    @FXML
    private TextField tfMetier;

    @FXML
    private TextField tfFilm;

    @FXML
    private TextField tfPrenom;

    @FXML
    private TextField tfVille;

    @FXML
    private Button btnTerminer;

    private int timeRemaining = 180; // 3 minutes en secondes
    private int score = 0;
    private Timeline timeline;
    private Map<String, String> reponsesJoueur = new HashMap<>();

    @FXML
    public void initialize() {
        btnTerminer.setOnAction(e -> terminerPartie());

        // Démarrer le timer
        demarrerTimer();
    }

    /**
     * Démarre le timer de la partie
     */
    private void demarrerTimer() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            timeRemaining--;
            mettreAJourAffichageTimer();

            if (timeRemaining <= 0) {
                timeline.stop();
                terminerPartie();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Met à jour l'affichage du timer
     */
    private void mettreAJourAffichageTimer() {
        int minutes = timeRemaining / 60;
        int secondes = timeRemaining % 60;
        labelTimer.setText(String.format("%02d:%02d", minutes, secondes));
    }

    /**
     * Récupère les réponses du joueur
     */
    private void recupérerReponses() {
        reponsesJoueur.clear();
        if (tfPays != null) reponsesJoueur.put("Pays", tfPays.getText().trim());
        if (tfAnimal != null) reponsesJoueur.put("Animal", tfAnimal.getText().trim());
        if (tfFruit != null) reponsesJoueur.put("Fruit", tfFruit.getText().trim());
        if (tfMetier != null) reponsesJoueur.put("Métier", tfMetier.getText().trim());
        if (tfFilm != null) reponsesJoueur.put("Film", tfFilm.getText().trim());
        if (tfPrenom != null) reponsesJoueur.put("Prénom", tfPrenom.getText().trim());
        if (tfVille != null) reponsesJoueur.put("Ville", tfVille.getText().trim());
    }

    /**
     * Calcule le score (simplifié pour l'instant)
     */
    private void calculerScore() {
        score = 0;
        for (String reponse : reponsesJoueur.values()) {
            if (!reponse.isEmpty()) {
                score += 50; // 50 points par réponse valide
            }
        }
        if (labelScore != null) {
            labelScore.setText("Score: " + score + " pts");
        }
    }

    /**
     * Termine la partie
     */
    private void terminerPartie() {
        if (timeline != null) {
            timeline.stop();
        }

        recupérerReponses();
        calculerScore();

        System.out.println("Partie terminée !");
        System.out.println("Réponses : " + reponsesJoueur);
        System.out.println("Score final : " + score);

        // Naviguer vers l'écran de résultats
        navigateTo("fxml/resultats-view.fxml", "Résultats");
    }

    /**
     * Navigation vers une autre vue
     */
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) btnTerminer.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de " + fxmlPath);
            e.printStackTrace();
        }
    }
}