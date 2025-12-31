package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;

import java.io.IOException;

/**
 * Contrôleur pour la salle d'attente multijoueur (liste_attente.fxml)
 */
public class ListeAttenteController {

    @FXML
    private Label lblPlayerCount;

    @FXML
    private Label lblMaxPlayers;

    @FXML
    private VBox vboxPlayers;

    @FXML
    private Label lblGameCode;

    @FXML
    private Button btnCopyCode;

    @FXML
    private Button btnQuitter;

    @FXML
    private Button btnPret;

    @FXML
    public void initialize() {
        System.out.println("✅ ListeAttenteController initialisé");
    }

    @FXML
    private void handlePret() {
        System.out.println("✅ Joueur prêt - Redirection vers partie_multi.fxml");
        navigateTo("/fxml/partie_multi.fxml", "Mode Multijoueur");
    }

    @FXML
    private void handleQuitter() {
        System.out.println("❌ Joueur quitte la salle d'attente");
        navigateTo("/fxml/main_menu.fxml", "TriHarf - Menu Principal");
    }

    @FXML
    private void handleCopyCode() {
        if (lblGameCode != null && lblGameCode.getText() != null) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(lblGameCode.getText());
            clipboard.setContent(content);
            System.out.println("📋 Code copié: " + lblGameCode.getText());
        }
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) btnPret.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);

        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
