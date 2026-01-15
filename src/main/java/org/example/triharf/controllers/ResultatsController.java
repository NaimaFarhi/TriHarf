package org.example.triharf.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;
import org.example.triharf.models.ResultatPartie;

import java.io.IOException;
import java.util.List;

/**
 * ResultatsController.java
 * Affiche les résultats finaux d'une partie
 * Reçoit les données de JeuSoloController
 */
public class ResultatsController {

    // ===== UI COMPONENTS =====
    @FXML
    private Label lblScore;

    @FXML
    private Label lblMessage;

    @FXML
    private Label lblEncouragement;

    @FXML
    private VBox vboxDetails;

    @FXML
    private Button btnReessayer;

    @FXML
    private Button btnMenu;

    // ===== STATE =====
    private List<ResultatPartie> resultats;
    private int scoreTotal;
    private long dureePartie;
    private String joueur;
    private Character lettre;

    @FXML
    public void initialize() {
        System.out.println("✅ ResultatsController initialisé");
    }

    /**
     * Reçoit les données de JeuSoloController et les affiche
     */
    public void displayResults(List<ResultatPartie> resultats, int scoreTotal, long dureePartie, String joueur,
            Character lettre) {

        if (resultats == null || resultats.isEmpty()) {
            System.err.println("❌ Résultats vides!");
            showAlert("Erreur", "Pas de résultats à afficher");
            return;
        }

        this.resultats = resultats;
        this.scoreTotal = scoreTotal;
        this.dureePartie = dureePartie;
        this.joueur = joueur;
        this.lettre = lettre;

        System.out.println("✅ Résultats reçus et affichés");
        afficherResultats();
    }

    private void afficherResultats() {
        if (lblScore == null || vboxDetails == null) {
            System.err.println("❌ ERREUR: Composants FXML non bindés");
            return;
        }

        // Afficher le score
        lblScore.setText(String.valueOf(scoreTotal));

        // Afficher le message personnalisé
        afficherMessage();

        // Afficher les détails
        afficherDetailsRecapitulatif();
    }

    /**
     * Message personnalisé selon le score
     */
    @FXML
    private Label lblTitre;
    @FXML
    private Label lblEmoji;

    private org.example.triharf.utils.SoundManager soundManager = new org.example.triharf.utils.SoundManager();

    /**
     * Message personnalisé selon le score
     */
    private void afficherMessage() {
        long nbValides = resultats.stream().filter(ResultatPartie::isValide).count();

        if (nbValides == 0) {
            lblTitre.setText("DOMMAGE");
            lblEmoji.setText("😢");
            lblMessage.setText("Oups ! 😅");
            lblEncouragement.setText("Aucun mot validé. C'est normal, cela arrive ! Réessaie et tu feras mieux.");
            org.example.triharf.utils.SoundManager.playDefeat();
        } else if (scoreTotal < 50) {
            lblTitre.setText("TERMINÉ");
            lblEmoji.setText("📝");
            lblMessage.setText("Pas mal ! 🎯");
            lblEncouragement.setText("Tu progresses. Chaque partie te rend meilleur(e) !");
            org.example.triharf.utils.SoundManager.playVictory();
        } else if (scoreTotal < 100) {
            lblTitre.setText("BIEN JOUÉ");
            lblEmoji.setText("👏");
            lblMessage.setText("Excellent ! 🌟");
            lblEncouragement.setText("Tu es sur la bonne voie. Bravo !");
            org.example.triharf.utils.SoundManager.playVictory();
        } else {
            lblTitre.setText("VICTOIRE");
            lblEmoji.setText("🏆");
            lblMessage.setText("INCROYABLE ! 🚀");
            lblEncouragement.setText("Quel score impressionnant ! Tu domines ce jeu !");
            org.example.triharf.utils.SoundManager.playVictory();
        }
    }

    /**
     * Affiche les détails du récapitulatif avec un design amélioré
     */
    private void afficherDetailsRecapitulatif() {
        if (vboxDetails == null)
            return;

        vboxDetails.getChildren().clear();
        vboxDetails.setSpacing(15);

        long nbValides = resultats.stream().filter(ResultatPartie::isValide).count();
        long nbRejetes = resultats.size() - nbValides;

        // ===== STATS GRID (Letter, Duration, Valid, Rejected) =====
        HBox statsGrid = new HBox(15);
        statsGrid.setAlignment(javafx.geometry.Pos.CENTER);
        statsGrid.setStyle("-fx-padding: 10 0 20 0;");

        // Stat Card: Letter
        VBox letterCard = createStatCard("🔤", "LETTRE", String.valueOf(lettre), "#9b59b6");

        // Stat Card: Duration
        String durationText = dureePartie >= 60
                ? String.format("%dm %ds", dureePartie / 60, dureePartie % 60)
                : dureePartie + "s";
        VBox durationCard = createStatCard("⏱️", "DURÉE", durationText, "#3498db");

        // Stat Card: Valid Words
        VBox validCard = createStatCard("✓", "VALIDES", nbValides + "/" + resultats.size(), "#27ae60");

        // Stat Card: Rejected Words (only if there are rejections)
        if (nbRejetes > 0) {
            VBox rejectedCard = createStatCard("✗", "REJETÉS", String.valueOf(nbRejetes), "#e74c3c");
            statsGrid.getChildren().addAll(letterCard, durationCard, validCard, rejectedCard);
        } else {
            statsGrid.getChildren().addAll(letterCard, durationCard, validCard);
        }

        vboxDetails.getChildren().add(statsGrid);

        // ===== DIVIDER =====
        HBox divider = new HBox();
        divider.setAlignment(javafx.geometry.Pos.CENTER);
        Region dividerLine = new Region();
        dividerLine.setStyle(
                "-fx-background-color: linear-gradient(to right, transparent, rgba(155, 89, 182, 0.4), transparent); -fx-pref-height: 1; -fx-pref-width: 300;");
        divider.getChildren().add(dividerLine);
        vboxDetails.getChildren().add(divider);

        // ===== RESPONSES SECTION TITLE =====
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(javafx.geometry.Pos.CENTER);
        titleBox.setStyle("-fx-padding: 15 0 10 0;");
        Label titleIcon = new Label("📋");
        titleIcon.setStyle("-fx-font-size: 16px;");
        Label titleCategories = new Label("DÉTAIL DES RÉPONSES");
        titleCategories.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #a0a0b0; -fx-letter-spacing: 1px;");
        titleBox.getChildren().addAll(titleIcon, titleCategories);
        vboxDetails.getChildren().add(titleBox);

        // ===== RESULTS LIST =====
        VBox resultsList = new VBox(8);
        resultsList.setStyle("-fx-padding: 5 0;");

        for (ResultatPartie resultat : resultats) {
            HBox resultItem = createResultItem(resultat);
            resultsList.getChildren().add(resultItem);
        }

        vboxDetails.getChildren().add(resultsList);
    }

    /**
     * Creates a styled stat card with icon, label, and value
     */
    private VBox createStatCard(String icon, String label, String value, String accentColor) {
        VBox card = new VBox(5);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setStyle(String.format(
                "-fx-background-color: rgba(255, 255, 255, 0.05); " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-radius: 12; " +
                        "-fx-border-width: 2; " +
                        "-fx-padding: 15 25; " +
                        "-fx-min-width: 100;",
                accentColor + "66" // Add transparency to border
        ));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle(String.format(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: %s;",
                accentColor));

        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 10px; -fx-text-fill: #888; -fx-font-weight: bold;");

        card.getChildren().addAll(iconLabel, valueLabel, labelText);
        return card;
    }

    /**
     * Creates a styled result item row
     */
    private HBox createResultItem(ResultatPartie resultat) {
        HBox item = new HBox(15);
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 15, 12, 15));

        boolean isValid = resultat.isValide();
        String bgColor = isValid ? "rgba(39, 174, 96, 0.1)" : "rgba(231, 76, 60, 0.1)";
        String borderColor = isValid ? "rgba(39, 174, 96, 0.3)" : "rgba(231, 76, 60, 0.3)";
        String statusColor = isValid ? "#27ae60" : "#e74c3c";

        item.setStyle(String.format(
                "-fx-background-color: %s; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-width: 1;",
                bgColor, borderColor));

        // Status indicator (circle with checkmark or X)
        Label statusIndicator = new Label(isValid ? "✓" : "✗");
        statusIndicator.setStyle(String.format(
                "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: %s; " +
                        "-fx-background-color: %s; " +
                        "-fx-background-radius: 15; " +
                        "-fx-min-width: 30; " +
                        "-fx-min-height: 30; " +
                        "-fx-alignment: center;",
                statusColor, statusColor + "22"));
        statusIndicator.setAlignment(javafx.geometry.Pos.CENTER);
        statusIndicator.setMinWidth(30);
        statusIndicator.setMinHeight(30);

        // Category name
        Label categoryLabel = new Label(resultat.getCategorie());
        categoryLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        categoryLabel.setPrefWidth(120);

        // Word/Answer
        String motText = resultat.getMot().isEmpty() ? "—" : resultat.getMot();
        Label motLabel = new Label(motText);
        motLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ccc;");
        HBox.setHgrow(motLabel, javafx.scene.layout.Priority.ALWAYS);

        // Points badge
        int points = resultat.getPoints();
        Label pointsBadge = new Label("+" + points);
        String pointsColor = points > 0 ? "#ffd700" : "#888";
        pointsBadge.setStyle(String.format(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: %s; " +
                        "-fx-background-color: rgba(255, 215, 0, 0.15); " +
                        "-fx-background-radius: 12; " +
                        "-fx-padding: 4 12;",
                pointsColor));

        item.getChildren().addAll(statusIndicator, categoryLabel, motLabel, pointsBadge);
        return item;
    }

    @FXML
    public void handleRetourMenu(ActionEvent event) {
        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
    }

    @FXML
    public void handleMenu(ActionEvent event) {
        navigateTo("/fxml/main_menu.fxml", "TriHarf - Menu Principal");
    }

    @FXML
    public void handleReessayer(ActionEvent event) {
        navigateTo("/fxml/param_partie_solo.fxml", "Configuration de la partie");
    }

    @FXML
    public void handleRevanche(ActionEvent event) {
        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = null;
            if (btnMenu != null && btnMenu.getScene() != null) {
                stage = (Stage) btnMenu.getScene().getWindow();
            } else if (btnReessayer != null && btnReessayer.getScene() != null) {
                stage = (Stage) btnReessayer.getScene().getWindow();
            } else {
                System.err.println("❌ Impossible de trouver la Stage !");
                return;
            }

            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}