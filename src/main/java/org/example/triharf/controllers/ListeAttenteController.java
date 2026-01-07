package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;
import org.example.triharf.network.GameClient;
import org.example.triharf.network.GameServer;
import org.example.triharf.network.NetworkMessage;

import java.io.IOException;
import java.util.List;

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
    private TextField tfNgrokUrl;

    @FXML
    private Button btnCopyNgrok;

    @FXML
    private Button btnPret;

    @FXML
    public void initialize() {
        System.out.println("✅ ListeAttenteController initialisé");
        
        if (tfNgrokUrl != null && lblGameCode != null) {
            tfNgrokUrl.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isEmpty()) {
                    lblGameCode.setText(newVal);
                } else {
                    if (networkService != null) {
                        lblGameCode.setText(networkService.getRoomId());
                    }
                }
            });
        }
    }

    private String gameMode = "MULTI";
    private boolean isReady = false;
    private org.example.triharf.services.NetworkService networkService;
    private List<String> categories;

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public void setNgrokUrl(String url) {
        if (tfNgrokUrl != null) {
            tfNgrokUrl.setText(url);
        }
    }

    public void setNetwork(org.example.triharf.services.NetworkService networkService) {
        this.networkService = networkService;
        
        if (networkService.getRoomId() != null && lblGameCode != null) {
            lblGameCode.setText(networkService.getRoomId());
        }

        if (networkService.getGameClient() != null) {
            networkService.getGameClient().setMessageHandler(this::handleIncomingMessage);
        }
    }

    private void handleIncomingMessage(NetworkMessage message) {
        javafx.application.Platform.runLater(() -> {
            switch (message.getType()) {
                case PLAYER_JOINED -> {
                    List<String> players = (List<String>) message.getData();
                    updatePlayerList(players);
                }
                case GAME_START -> {
                    startGame();
                }
            }
        });
    }

    private void startGame() {
        String targetFxml = "/fxml/partie_multi.fxml";
        String title = "Mode Multijoueur";

        if ("BATTLE".equals(gameMode)) {
            targetFxml = "/fxml/partie_battle.fxml";
            title = "Battle Royale";
        }

        navigateToMulti(targetFxml, title);
    }

    private void updatePlayerList(List<String> playersStatus) {
        if (vboxPlayers != null) {
            vboxPlayers.getChildren().clear();
            for (String ps : playersStatus) {
                String[] parts = ps.split(":");
                String id = parts[0];
                String status = parts.length > 1 ? parts[1] : "ATTENTE";
                String emoji = "PREST".equals(status) ? "✅" : "⏳";
                vboxPlayers.getChildren().add(new Label(emoji + " " + id + " (" + status + ")"));
            }
        }
        if (lblPlayerCount != null) {
            lblPlayerCount.setText(String.valueOf(playersStatus.size()));
        }
    }

    public void setGameMode(String mode) {
        this.gameMode = mode;
        System.out.println("Mode de jeu (Attente) : " + mode);
    }

    @FXML
    private void handlePret() {
        if (networkService != null && networkService.getGameClient() != null) {
            isReady = !isReady; // Toggle
            networkService.getGameClient().sendMessage(new NetworkMessage(NetworkMessage.Type.PLAYER_READY, ParametresGenerauxController.pseudoGlobal, isReady));
            
            if (btnPret != null) {
                btnPret.setText(isReady ? "PAS PRÊT" : "JE SUIS PRÊT");
            }

            // Si c'est l'hôte et qu'il est prêt, il peut décider de lancer (pour l'instant automatique s'il est prêt)
            // On vérifie si on a un serveur via le service
            if (networkService.getGameServer() != null && isReady) {
                 networkService.getGameClient().sendMessage(new NetworkMessage(NetworkMessage.Type.GAME_START, ParametresGenerauxController.pseudoGlobal, networkService.getRoomId()));
            }
        }
    }

    private void navigateToMulti(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();

            // Pass network to JeuMultiController
            Object controller = loader.getController();
            if (controller instanceof JeuMultiController) {
                ((JeuMultiController) controller).setNetwork(networkService);
                if (categories != null) {
                    ((JeuMultiController) controller).setCategories(categories);
                }
            }

            Stage stage = (Stage) btnPret.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);

        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
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

    @FXML
    private void handleCopyNgrok() {
        if (tfNgrokUrl != null && tfNgrokUrl.getText() != null && !tfNgrokUrl.getText().isEmpty()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(tfNgrokUrl.getText());
            clipboard.setContent(content);
            System.out.println("📋 URL Ngrok copiée: " + tfNgrokUrl.getText());
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
