package org.example.triharf.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
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
    @FXML private Label lblTitre;
    @FXML private Label lblEmoji;

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
     * Affiche les détails du récapitulatif
     */
    private void afficherDetailsRecapitulatif() {
        if (vboxDetails == null) return;

        vboxDetails.getChildren().clear();

        long nbValides = resultats.stream().filter(ResultatPartie::isValide).count();
        long nbRejetes = resultats.size() - nbValides;

        // Lettre
        Label lblLettreDetail = new Label("Lettre : " + lettre);
        lblLettreDetail.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        vboxDetails.getChildren().add(lblLettreDetail);

        // Durée
        Label lblDureeDetail = new Label("Durée : " + dureePartie + " secondes");
        lblDureeDetail.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        vboxDetails.getChildren().add(lblDureeDetail);

        // Résumé
        Label lblMotsValides = new Label("✓ Mots valides : " + nbValides + "/" + resultats.size());
        lblMotsValides.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");
        vboxDetails.getChildren().add(lblMotsValides);

        if (nbRejetes > 0) {
            Label lblMotsRejetes = new Label("✗ Mots rejetés : " + nbRejetes);
            lblMotsRejetes.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
            vboxDetails.getChildren().add(lblMotsRejetes);
        }

        // Détail des réponses
        Label titleCategories = new Label("Détail des réponses :");
        titleCategories.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #34495e; -fx-padding: 15 0 10 0;");
        vboxDetails.getChildren().add(titleCategories);

        for (ResultatPartie resultat : resultats) {
            HBox ligneResultat = new HBox(10);
            ligneResultat.setPadding(new Insets(8, 0, 8, 0));
            ligneResultat.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");

            String statut = resultat.isValide() ? "✓" : "✗";
            String couleur = resultat.isValide() ? "#27ae60" : "#e74c3c";
            Label lblStatut = new Label(statut);
            lblStatut.setStyle("-fx-font-weight: bold; -fx-text-fill: " + couleur + "; -fx-font-size: 14px;");
            lblStatut.setPrefWidth(20);

            Label lblCategorie = new Label(resultat.getCategorie());
            lblCategorie.setPrefWidth(100);
            lblCategorie.setStyle("-fx-font-size: 11px;");

            Label lblMot = new Label(resultat.getMot().isEmpty() ? "-" : resultat.getMot());
            lblMot.setPrefWidth(150);
            lblMot.setStyle("-fx-font-size: 11px; -fx-text-fill: #34495e;");

            Label lblPts = new Label(resultat.getPoints() + " pts");
            lblPts.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

            ligneResultat.getChildren().addAll(lblStatut, lblCategorie, lblMot, lblPts);
            vboxDetails.getChildren().add(ligneResultat);
        }
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