package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;

import java.io.IOException;

/**
 * Contrôleur pour l'écran des résultats
 */
public class ResultatsController {

    @FXML
    private Label labelScore;

    @FXML
    private Button btnRejouer;

    @FXML
    private Button btnMenu;

    @FXML
    private Button btnTurbo;

    private int scoreTotal = 0;

    @FXML
    public void initialize() {
        if (btnRejouer != null) btnRejouer.setOnAction(e -> rejouer());
        if (btnMenu != null) btnMenu.setOnAction(e -> retourMenu());
        if (btnTurbo != null) btnTurbo.setOnAction(e -> activerTurbo());

        // Afficher le score
        afficherScore();
    }

    /**
     * Affiche le score total
     */
    private void afficherScore() {
        if (labelScore != null) {
            labelScore.setText("SCORE TOTAL : " + scoreTotal + " pts");
        }
    }

    /**
     * Réjoue une partie
     */
    private void rejouer() {
        System.out.println("Relancer une nouvelle partie...");
        navigateTo("fxml/param_partie_solo.fxml", "Paramètres - Mode Solo");
    }

    /**
     * Retour au menu principal
     */
    private void retourMenu() {
        System.out.println("Retour au menu principal...");
        navigateTo("fxml/main_menu.fxml", "Menu Principal");
    }

    /**
     * Active le mode Turbo
     */
    private void activerTurbo() {
        System.out.println("Mode Turbo activé !");
        // À implémenter : logique du mode turbo
    }

    /**
     * Définir le score depuis le contrôleur précédent
     */
    public void setScore(int score) {
        this.scoreTotal = score;
        afficherScore();
    }

    /**
     * Navigation vers une autre vue
     */
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) btnMenu.getScene().getWindow();
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