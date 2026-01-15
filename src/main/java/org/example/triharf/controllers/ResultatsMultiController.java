package org.example.triharf.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;
import org.example.triharf.models.Joueur;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for multiplayer results/ranking page
 */
public class ResultatsMultiController {

    @FXML
    private Label lblLettre;
    @FXML
    private HBox hboxPodium;
    @FXML
    private VBox vboxRanking;
    @FXML
    private VBox vboxYourResult;
    @FXML
    private Label lblYourRank;
    @FXML
    private Label lblYourScore;
    @FXML
    private Button btnRejouer;
    @FXML
    private Button btnRevanche;
    @FXML
    private Button btnMenu;

    // Alternative FXML elements
    @FXML
    private HBox podiumContainer;
    @FXML
    private Label lblName1, lblScore1;
    @FXML
    private Label lblName2, lblScore2;
    @FXML
    private Label lblName3, lblScore3;
    @FXML
    private HBox listContainer;

    private Map<String, Integer> scores;
    private String currentPlayer;
    private Character lettre;
    private List<Map.Entry<String, Integer>> rankedPlayers;
    private List<Joueur> classementJoueurs;

    @FXML
    public void initialize() {
        System.out.println("✅ ResultatsMultiController initialisé");
    }

    /**
     * Display the ranking of all players (Map-based version)
     */
    public void displayRanking(Map<String, Integer> playerScores, String currentPlayer, Character lettre) {
        this.scores = playerScores;
        this.currentPlayer = currentPlayer;
        this.lettre = lettre;

        if (scores == null || scores.isEmpty()) {
            System.err.println("❌ Pas de scores à afficher");
            return;
        }

        // Sort players by score (descending)
        rankedPlayers = new ArrayList<>(scores.entrySet());
        rankedPlayers.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        System.out.println("✅ Classement calculé: " + rankedPlayers.size() + " joueurs");

        // Update letter display
        if (lblLettre != null) {
            lblLettre.setText("Lettre: " + lettre);
        }

        // Build podium (top 3)
        buildPodium();

        // Build full ranking list
        buildRankingList();

        // Highlight current player's result
        highlightCurrentPlayer();
    }

    /**
     * Set classement using Joueur objects
     */
    /**
     * Wrapper for displayRanking to match method call from JeuChaosController
     */
    public void setScores(Map<String, Integer> playerScores, String currentPlayer) {
        displayRanking(playerScores, currentPlayer, '?'); // Pass a default or null for letter currently
    }

    public void setClassement(List<Joueur> joueurs) {
        // Sort players by score descending
        this.classementJoueurs = joueurs.stream()
                .sorted((j1, j2) -> Integer.compare(j2.getScoreTotal(), j1.getScoreTotal()))
                .collect(Collectors.toList());

        afficherPodium();
        afficherListeRestante();
    }

    private void buildPodium() {
        if (hboxPodium == null)
            return;
        hboxPodium.getChildren().clear();

        String[] medals = { "🥇", "🥈", "🥉" };
        String[] colors = { "#f39c12", "#95a5a6", "#cd6133" };
        int[] heights = { 140, 110, 90 };

        // For podium display: 2nd, 1st, 3rd
        int[] podiumOrder = { 1, 0, 2 };

        for (int pos : podiumOrder) {
            if (pos < rankedPlayers.size()) {
                Map.Entry<String, Integer> entry = rankedPlayers.get(pos);
                VBox podiumBox = createPodiumBox(entry.getKey(), entry.getValue(), medals[pos], colors[pos],
                        heights[pos]);
                hboxPodium.getChildren().add(podiumBox);
            }
        }
    }

    private VBox createPodiumBox(String playerName, int score, String medal, String color, int height) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.BOTTOM_CENTER);
        box.setPrefWidth(150);
        box.setMinHeight(height + 60);

        // Player name
        Label nameLabel = new Label(playerName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Medal
        Label medalLabel = new Label(medal);
        medalLabel.setStyle("-fx-font-size: 32px;");

        // Score
        Label scoreLabel = new Label(score + " pts");
        scoreLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Podium stand
        VBox stand = new VBox();
        stand.setPrefHeight(height);
        stand.setPrefWidth(120);
        stand.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10 10 0 0;");
        stand.setAlignment(Pos.CENTER);

        // Rank number on stand
        Label rankLabel = new Label(String.valueOf(rankedPlayers.indexOf(Map.entry(playerName, score)) + 1));
        rankLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        stand.getChildren().add(rankLabel);

        box.getChildren().addAll(nameLabel, medalLabel, scoreLabel, stand);
        return box;
    }

    private void afficherPodium() {
        if (classementJoueurs == null || classementJoueurs.isEmpty())
            return;

        // 1st Place
        if (classementJoueurs.size() >= 1) {
            Joueur j1 = classementJoueurs.get(0);
            if (lblName1 != null)
                lblName1.setText(j1.getPseudo());
            if (lblScore1 != null)
                lblScore1.setText(String.valueOf(j1.getScoreTotal()));
        }

        // 2nd Place
        if (classementJoueurs.size() >= 2) {
            Joueur j2 = classementJoueurs.get(1);
            if (lblName2 != null)
                lblName2.setText(j2.getPseudo());
            if (lblScore2 != null)
                lblScore2.setText(String.valueOf(j2.getScoreTotal()));
        } else {
            if (lblName2 != null)
                lblName2.setText("-");
            if (lblScore2 != null)
                lblScore2.setText("");
        }

        // 3rd Place
        if (classementJoueurs.size() >= 3) {
            Joueur j3 = classementJoueurs.get(2);
            if (lblName3 != null)
                lblName3.setText(j3.getPseudo());
            if (lblScore3 != null)
                lblScore3.setText(String.valueOf(j3.getScoreTotal()));
        } else {
            if (lblName3 != null)
                lblName3.setText("-");
            if (lblScore3 != null)
                lblScore3.setText("");
        }
    }

    private void buildRankingList() {
        if (vboxRanking == null)
            return;
        vboxRanking.getChildren().clear();

        int rank = 1;
        for (Map.Entry<String, Integer> entry : rankedPlayers) {
            HBox row = createRankingRow(rank, entry.getKey(), entry.getValue());
            vboxRanking.getChildren().add(row);
            rank++;
        }
    }

    private HBox createRankingRow(int rank, String playerName, int score) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 20, 10, 20));

        boolean isCurrentPlayer = playerName.equals(currentPlayer);
        String bgColor = isCurrentPlayer ? "-fx-background-color: rgba(155, 89, 182, 0.3); -fx-border-color: #9b59b6;"
                : "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: #444;";
        row.setStyle(bgColor + " -fx-border-radius: 8; -fx-background-radius: 8;");

        // Rank
        String rankEmoji = switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "#" + rank;
        };
        Label rankLabel = new Label(rankEmoji);
        rankLabel.setStyle("-fx-font-size: 18px; -fx-min-width: 50;");

        // Player name
        Label nameLabel = new Label(playerName + (isCurrentPlayer ? " (vous)" : ""));
        nameLabel.setStyle("-fx-text-fill: " + (isCurrentPlayer ? "#9b59b6" : "white")
                + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        nameLabel.setPrefWidth(200);

        // Score
        Label scoreLabel = new Label(score + " points");
        scoreLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 16px; -fx-font-weight: bold;");

        row.getChildren().addAll(rankLabel, nameLabel, scoreLabel);
        return row;
    }

    private void afficherListeRestante() {
        if (listContainer == null)
            return;
        listContainer.getChildren().clear();

        // Players from rank 4 onwards
        if (classementJoueurs != null && classementJoueurs.size() > 3) {
            for (int i = 3; i < classementJoueurs.size(); i++) {
                Joueur j = classementJoueurs.get(i);
                VBox card = createPlayerCard(i + 1, j);
                listContainer.getChildren().add(card);
            }
        }
    }

    private VBox createPlayerCard(int rank, Joueur joueur) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.08); " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: rgba(255, 255, 255, 0.2); " +
                        "-fx-border-radius: 12; " +
                        "-fx-padding: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        card.setPrefWidth(120);
        card.setMinWidth(120);

        Label lblRank = new Label("#" + rank);
        lblRank.setStyle("-fx-text-fill: #aaa; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label lblName = new Label(joueur.getPseudo());
        lblName.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        lblName.setWrapText(true);
        lblName.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label lblScore = new Label(String.valueOf(joueur.getScoreTotal()) + " pts");
        lblScore.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 14px; -fx-font-weight: bold;");

        card.getChildren().addAll(lblRank, lblName, lblScore);
        return card;
    }

    private void highlightCurrentPlayer() {
        if (lblYourRank == null || lblYourScore == null || rankedPlayers == null)
            return;

        int rank = 1;
        int score = 0;
        for (Map.Entry<String, Integer> entry : rankedPlayers) {
            if (entry.getKey().equals(currentPlayer)) {
                score = entry.getValue();
                break;
            }
            rank++;
        }

        // Display rank with medal if top 3
        String rankText = switch (rank) {
            case 1 -> "🥇 #1";
            case 2 -> "🥈 #2";
            case 3 -> "🥉 #3";
            default -> "#" + rank;
        };

        lblYourRank.setText(rankText);
        lblYourScore.setText(score + " points");

        // Update style based on rank
        if (rank == 1 && vboxYourResult != null) {
            vboxYourResult.setStyle(
                    "-fx-padding: 20; -fx-background-color: rgba(243, 156, 18, 0.2); -fx-border-color: #f39c12; -fx-border-radius: 10; -fx-background-radius: 10;");
        }
    }

    private org.example.triharf.network.GameClient gameClient;

    public void setGameClient(org.example.triharf.network.GameClient gameClient) {
        this.gameClient = gameClient;
    }

    private void disconnect() {
        if (gameClient != null && gameClient.isConnected()) {
            try {
                System.out.println("🔌 Disconnecting previous game session...");
                gameClient.disconnect();
            } catch (IOException e) {
                System.err.println("Error disconnecting: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRejouer() {
        disconnect();
        navigateTo("/fxml/param_partie_multi.fxml", "Configuration Multijoueur");
    }

    @FXML
    public void handleRevanche(ActionEvent event) {
        disconnect();
        navigateTo("/fxml/param_partie_multi.fxml", "Configuration Multijoueur");
    }

    @FXML
    private void handleMenu() {
        disconnect();
        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
    }

    @FXML
    public void handleMenu(ActionEvent event) {
        disconnect();
        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnMenu.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}