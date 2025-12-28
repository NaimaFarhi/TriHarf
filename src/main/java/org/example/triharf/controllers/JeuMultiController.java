package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TableView;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;

import java.io.IOException;

/**
 * Contrôleur pour le jeu multijoueur (partie_multi.fxml)
 */
public class JeuMultiController {

    @FXML
    private Button btnRetour; // Le bouton "←"

    @FXML
    private Label labelTimer;

    @FXML
    private Label labelNbJoueurs;

    @FXML
    private TableView<?> tableJoueurs;

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField tfMessageChat;

    @FXML
    private Button btnEnvoyer; // "📤"

    private int timeRemaining = 180; // 3 minutes
    private Timeline timeline;
    private int nbJoueurs = 4;

    @FXML
    public void initialize() {
        if (btnRetour != null) btnRetour.setOnAction(e -> retourMenu());
        if (btnEnvoyer != null) btnEnvoyer.setOnAction(e -> envoyerMessage());

        // Démarrer le timer
        demarrerTimer();

        // Afficher le nombre de joueurs
        if (labelNbJoueurs != null) {
            labelNbJoueurs.setText(nbJoueurs + " joueurs");
        }
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
        if (labelTimer != null) {
            labelTimer.setText(String.format("%02d:%02d", minutes, secondes));
        }
    }

    /**
     * Envoie un message dans le chat
     */
    private void envoyerMessage() {
        if (tfMessageChat == null || chatArea == null) return;

        String message = tfMessageChat.getText().trim();
        if (message.isEmpty()) return;

        // Ajouter le message au chat
        String contenuChat = chatArea.getText();
        chatArea.setText(contenuChat + "\nMoi: " + message);
        tfMessageChat.clear();

        System.out.println("Message envoyé: " + message);
    }

    /**
     * Termine la partie multijoueur
     */
    private void terminerPartie() {
        if (timeline != null) {
            timeline.stop();
        }

        System.out.println("Partie multijoueur terminée !");

        // Naviguer vers l'écran de résultats
        navigateTo("fxml/resultats-view.fxml", "Résultats");
    }

    /**
     * Retour au menu principal
     */
    private void retourMenu() {
        if (timeline != null) {
            timeline.stop();
        }
        navigateTo("fxml/main_menu.fxml", "Menu Principal");
    }

    /**
     * Navigation vers une autre vue
     */
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) btnRetour.getScene().getWindow();
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