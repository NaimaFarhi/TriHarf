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
    private Button btnPret;

    @FXML
    public void initialize() {
        System.out.println("✅ ListeAttenteController initialisé");
    }

    private String gameMode = "MULTI";
    private GameClient gameClient;
    private GameServer gameServer;
    private String roomId;
    private boolean isReady = false;

    public void setNetwork(GameClient client, GameServer server, String roomId) {
        this.gameClient = client;
        this.gameServer = server;
        this.roomId = roomId;

        if (lblGameCode != null) {
            lblGameCode.setText(roomId);
        }

        if (gameClient != null) {
            gameClient.setMessageHandler(this::handleIncomingMessage);
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
        if (gameClient != null) {
            isReady = !isReady; // Toggle
            gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.PLAYER_READY, ParametresGenerauxController.pseudoGlobal, isReady));
            
            if (btnPret != null) {
                btnPret.setText(isReady ? "PAS PRÊT" : "JE SUIS PRÊT");
            }

            // Si c'est l'hôte et qu'il est prêt, il peut décider de lancer (pour l'instant automatique s'il est prêt)
            // Mais l'utilisateur a dit "kan je clique je suis pret tu dois afficher...", il n'a pas dit de lancer tout de suite.
            // Cependant, il faut bien lancer la partie à un moment.
            // On va laisser l'hôte lancer s'il est prêt.
            if (gameServer != null && isReady) {
                 gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.GAME_START, ParametresGenerauxController.pseudoGlobal, roomId));
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
                ((JeuMultiController) controller).setNetwork(gameClient, roomId);
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
