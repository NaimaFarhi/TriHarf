package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;
import org.example.triharf.network.GameClient;
import org.example.triharf.network.GameServer;
import org.example.triharf.network.NetworkMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private Button btnCommencer; // Host only button

    private String gameMode = "MULTI";
    private GameClient gameClient;
    private GameServer gameServer;
    private String roomId;
    private int maxPlayers = 4;

    private boolean isHost = false;
    private List<String> categories = new ArrayList<>();
    private String currentLetter = null;
    private List<String> playerPseudos = new ArrayList<>();
    private int totalRounds = 3;
    private int roundDuration = 120;

    @FXML
    public void initialize() {
        System.out.println("✅ ListeAttenteController initialisé");
        // Hide start button initially
        if (btnCommencer != null)
            btnCommencer.setVisible(false);
    }

    public void setNetwork(GameClient client, GameServer server, String roomId) {
        this.gameClient = client;
        this.gameServer = server;
        this.roomId = roomId;
        this.isHost = (server != null); // Host has server reference

        if (lblGameCode != null) {
            lblGameCode.setText(roomId);
        }

        if (lblMaxPlayers != null) {
            lblMaxPlayers.setText(String.valueOf(maxPlayers));
        }

        if (gameClient != null) {
            gameClient.setMessageHandler(this::handleIncomingMessage);
        }

        // Initialize player count (host is already connected)
        if (lblPlayerCount != null) {
            lblPlayerCount.setText("1");
        }

        // Show start button only for host
        if (btnCommencer != null && isHost) {
            btnCommencer.setVisible(true);
        }
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
        if (lblMaxPlayers != null) {
            lblMaxPlayers.setText(String.valueOf(maxPlayers));
        }
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public void setGameMode(String mode) {
        this.gameMode = mode;
        System.out.println("Mode de jeu (Attente) : " + mode);
    }

    public void setRoundConfig(int totalRounds, int roundDuration) {
        this.totalRounds = totalRounds;
        this.roundDuration = roundDuration;
        System.out.println("✅ Configuration manches: " + totalRounds + " manches, " + roundDuration + "s chacune");
    }

    private void handleIncomingMessage(NetworkMessage message) {
        javafx.application.Platform.runLater(() -> {
            switch (message.getType()) {
                case PLAYER_JOINED -> {
                    @SuppressWarnings("unchecked")
                    List<String> players = (List<String>) message.getData();
                    updatePlayerList(players);
                }
                case ROOM_INFO -> {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> info = (java.util.Map<String, Object>) message.getData();
                    if (info.containsKey("maxPlayers")) {
                        Object max = info.get("maxPlayers");
                        if (max instanceof Number) {
                            setMaxPlayers(((Number) max).intValue());
                            System.out.println("ℹ️ Max players updated to: " + max);
                        }
                    }
                }
                case GAME_START -> {
                    // Log raw GAME_START data for debugging
                    System.out.println("🔔 GAME_START reçu (ListeAttenteController)");

                    // Extract categories, letter, and players from GAME_START message
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) message.getData();
                    if (data != null) {
                        System.out.println("📦 GAME_START data keys: " + data.keySet());

                        @SuppressWarnings("unchecked")
                        List<String> cats = (List<String>) data.get("categories");
                        if (cats != null && !cats.isEmpty()) {
                            this.categories = new ArrayList<>(cats);
                            System.out.println("✅ Catégories reçues du serveur: " + cats.size());
                        }
                        // Extract letter
                        String letter = (String) data.get("letter");
                        if (letter != null && !letter.isEmpty()) {
                            this.currentLetter = letter;
                            System.out.println("✅ Lettre reçue du serveur: " + letter);
                        }
                        // Extract players list
                        @SuppressWarnings("unchecked")
                        List<String> players = (List<String>) data.get("players");
                        if (players != null && !players.isEmpty()) {
                            this.playerPseudos = new ArrayList<>(players);
                            System.out.println("✅ Joueurs reçus du serveur: " + players);
                        }
                        // Extract round configuration
                        Object durationObj = data.get("duration");
                        Object roundsObj = data.get("totalRounds");
                        System.out.println("📊 duration raw value: " + durationObj + " (type: "
                                + (durationObj != null ? durationObj.getClass().getName() : "null") + ")");
                        System.out.println("📊 totalRounds raw value: " + roundsObj + " (type: "
                                + (roundsObj != null ? roundsObj.getClass().getName() : "null") + ")");

                        if (durationObj instanceof Number) {
                            this.roundDuration = ((Number) durationObj).intValue();
                            System.out.println("✅ Durée manche reçue: " + roundDuration + "s");
                        } else {
                            System.out.println("⚠️ duration n'est pas un Number, utilisant défaut: " + roundDuration);
                        }
                        if (roundsObj instanceof Number) {
                            this.totalRounds = ((Number) roundsObj).intValue();
                            System.out.println("✅ Nombre de manches reçu: " + totalRounds);
                        } else {
                            System.out.println("⚠️ totalRounds n'est pas un Number, utilisant défaut: " + totalRounds);
                        }

                        String mode = (String) data.get("gameMode");
                        if (mode != null) {
                            this.gameMode = mode;
                            System.out.println("✅ Mode de jeu reçu du server: " + mode);
                        } else {
                            System.out.println("⚠️ Mode de jeu NON reçu, conservation de: " + this.gameMode);
                        }
                    }
                    startGame();
                }
                // Ignore other message types in this context
                default -> {
                }
            }
        });

    }

    private void updatePlayerList(List<String> playersStatus) {
        if (vboxPlayers != null) {
            vboxPlayers.getChildren().clear();
            for (String ps : playersStatus) {
                String[] parts = ps.split(":");
                String name = parts[0];
                String status = parts.length > 1 ? parts[1] : "ATTENTE";
                String emoji = "PREST".equals(status) ? "✅" : "⏳";
                Label lbl = new Label(emoji + " " + name);
                lbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
                vboxPlayers.getChildren().add(lbl);
            }
        }

        if (lblPlayerCount != null) {
            lblPlayerCount.setText(String.valueOf(playersStatus.size()));
        }

        // Enable start button when minimum players reached (host only)
        if (btnCommencer != null && isHost) {
            int minPlayers = gameMode.equals("BATAILLE_ROYALE") ? 2 : 2;
            btnCommencer.setDisable(playersStatus.size() < minPlayers);
        }
    }

    @FXML
    private void handleCommencer() {
        if (!isHost)
            return;

        // Broadcast game start
        gameServer.startGame(roomId);
    }

    private void startGame() {
        if ("CHAOS".equals(gameMode) || "MODE_CHAOS".equals(gameMode)) {
            navigateToChaosGame("/fxml/partie_chaos.fxml", "Partie - CHAOS");
        } else {
            navigateToGame("/fxml/partie_multi.fxml", "Partie - " + gameMode);
        }
    }

    private void navigateToChaosGame(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof JeuChaosController) {
                JeuChaosController cc = (JeuChaosController) controller;
                cc.setNetwork(gameClient, roomId);
                cc.setIsHost(isHost);
                cc.setCategories(categories);
                if (currentLetter != null)
                    cc.setLettre(currentLetter);
                if (playerPseudos != null)
                    cc.setPlayerList(playerPseudos);
                cc.setGameDuration(roundDuration);

                System.out.println("🔥 Navigation vers Chaos Mode avec " + categories.size() + " catégories");
                cc.demarrerPartie();
            }

            Stage stage = (Stage) btnQuitter.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ... handleQuitter, handleCopyCode ...

    @FXML
    private void handleQuitter() {
        // If host leaves, server stops and everyone gets kicked
        if (isHost && gameServer != null) {
            try {
                gameServer.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (gameClient != null) {
            gameClient.sendMessage(new NetworkMessage(
                    NetworkMessage.Type.DISCONNECT,
                    ParametresGenerauxController.pseudoGlobal,
                    roomId));
        }

        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
    }

    @FXML
    private void handleCopyCode() {
        if (lblGameCode != null && lblGameCode.getText() != null) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(lblGameCode.getText());
            clipboard.setContent(content);
        }
    }

    private void navigateToGame(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof JeuMultiController) {
                JeuMultiController mc = (JeuMultiController) controller;
                mc.setNetwork(gameClient, roomId);
                mc.setIsHost(isHost);
                mc.setCategories(categories);
                mc.setRoundConfig(totalRounds, roundDuration);

                // Set Game Mode
                mc.setGameMode(gameMode);

                if (currentLetter != null) {
                    mc.setLettre(currentLetter);
                }
                if (playerPseudos != null && !playerPseudos.isEmpty()) {
                    mc.setPlayerList(playerPseudos);
                }
                System.out.println("📋 Catégories passées à JeuMultiController: " + categories);
                System.out.println("📋 Lettre passée à JeuMultiController: " + currentLetter);
                System.out.println("📋 Joueurs passés à JeuMultiController: " + playerPseudos);
                System.out.println("📋 Configuration manches: " + totalRounds + "/" + roundDuration + "s");
                // Start the game after setting up categories
                mc.demarrerPartie();
            }

            Stage stage = (Stage) btnQuitter.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnQuitter.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}