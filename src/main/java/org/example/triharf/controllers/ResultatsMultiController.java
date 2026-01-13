package org.example.triharf.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;
import org.example.triharf.models.Joueur;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ResultatsMultiController {

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
    @FXML
    private Button btnRevanche;
    @FXML
    private Button btnMenu;

    private List<Joueur> classmentJoueurs;

    @FXML
    public void initialize() {
        // Initial setup if needed
    }

    public void setClassement(List<Joueur> joueurs) {
        // Sort players by score descending
        this.classmentJoueurs = joueurs.stream()
                .sorted((j1, j2) -> Integer.compare(j2.getScoreTotal(), j1.getScoreTotal()))
                .collect(Collectors.toList());

        afficherPodium();
        afficherListeRestante();
    }

    private void afficherPodium() {
        // Clear default generic text if list is empty or small
        if (classmentJoueurs.isEmpty())
            return;

        // 1st Place
        if (classmentJoueurs.size() >= 1) {
            Joueur j1 = classmentJoueurs.get(0);
            lblName1.setText(j1.getPseudo());
            lblScore1.setText(String.valueOf(j1.getScoreTotal()));
        }

        // 2nd Place
        if (classmentJoueurs.size() >= 2) {
            Joueur j2 = classmentJoueurs.get(1);
            lblName2.setText(j2.getPseudo());
            lblScore2.setText(String.valueOf(j2.getScoreTotal()));
        } else {
            lblName2.setText("-");
            lblScore2.setText("");
        }

        // 3rd Place
        if (classmentJoueurs.size() >= 3) {
            Joueur j3 = classmentJoueurs.get(2);
            lblName3.setText(j3.getPseudo());
            lblScore3.setText(String.valueOf(j3.getScoreTotal()));
        } else {
            lblName3.setText("-");
            lblScore3.setText("");
        }
    }

    private void afficherListeRestante() {
        listContainer.getChildren().clear();

        // Players from rank 4 onwards
        if (classmentJoueurs.size() > 3) {
            for (int i = 3; i < classmentJoueurs.size(); i++) {
                Joueur j = classmentJoueurs.get(i);
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

    @FXML
    public void handleRevanche(ActionEvent event) {
        // Logic to restart multiplayer game setup
        navigateTo("/fxml/param_partie_multi.fxml");
    }

    @FXML
    public void handleMenu(ActionEvent event) {
        navigateTo("/fxml/main_menu.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnMenu.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
