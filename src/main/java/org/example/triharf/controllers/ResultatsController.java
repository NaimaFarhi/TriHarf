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
import javafx.collections.FXCollections;
import org.example.triharf.HelloApplication;
import org.example.triharf.models.ResultatPartie;

import java.io.IOException;
import java.util.List;

/**
 * ResultatsController.java
 * Affiche les résultats finaux d'une partie
 * Adapté au FXML Resultats.fxml existant
 */
public class ResultatsController {

    // ===== UI COMPONENTS (du FXML existant) =====
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

    /* =======================
       INITIALIZATION
       ======================= */

    @FXML
    public void initialize() {
        System.out.println("✅ ResultatsController initialisé");
    }

    /* =======================
       RECEPTION DES DONNÉES
       ======================= */

    /**
     * Reçoit les données de JeuSoloController
     * POINT CLEF : Appelée depuis JeuSoloController.navigateToResults()
     */
    public void displayResults(List<ResultatPartie> resultats, int scoreTotal, long dureePartie, String joueur, Character lettre) {
        if (resultats == null || resultats.isEmpty()) {
            System.err.println("❌ ERREUR : Pas de résultats !");
            showAlert("Erreur", "Résultats non disponibles");
            return;
        }

        this.resultats = resultats;
        this.scoreTotal = scoreTotal;
        this.dureePartie = dureePartie;
        this.joueur = joueur;
        this.lettre = lettre;

        System.out.println("✅ Résultats reçus:");
        System.out.println("   Joueur: " + joueur);
        System.out.println("   Score total: " + scoreTotal);
        System.out.println("   Durée: " + dureePartie + "s");
        System.out.println("   Résultats: " + resultats.size());

        afficherResultats();
    }

    /* =======================
       AFFICHAGE DES RÉSULTATS
       ======================= */

    private void afficherResultats() {
        if (lblScore == null || vboxDetails == null) {
            System.err.println("❌ ERREUR: Les labels du FXML ne sont pas bindés !");
            System.err.println("   Vérifiez que Resultats.fxml a : lblScore, vboxDetails");
            showAlert("Erreur FXML", "Les composants du FXML ne sont pas bindés correctement");
            return;
        }

        // ============================================
        // SCORE TOTAL
        // ============================================
        lblScore.setText(String.valueOf(scoreTotal));
        System.out.println("✅ Score affiché: " + scoreTotal);

        // ============================================
        // MESSAGE PERSONNALISÉ
        // ============================================
        afficherMessagePersonnalise();

        // ============================================
        // DÉTAILS DYNAMIQUES
        // ============================================
        afficherDetailsRecapitulatif();
    }

    /**
     * Affiche un message personnalisé selon le score
     */
    private void afficherMessagePersonnalise() {
        long nbValides = resultats.stream().filter(ResultatPartie::isValide).count();

        if (nbValides == 0) {
            lblMessage.setText("Oups ! 😅");
            lblEncouragement.setText("Aucun mot validé. C'est normal, cela arrive ! Réessaie et tu feras mieux.");
        } else if (nbValides == 1) {
            lblMessage.setText("Bien commencé ! 👍");
            lblEncouragement.setText("Tu as trouvé 1 mot valide. Continue comme ça !");
        } else if (scoreTotal < 50) {
            lblMessage.setText("Pas mal ! 🎯");
            lblEncouragement.setText("Tu progresses. Chaque partie te rend meilleur(e) !");
        } else if (scoreTotal < 100) {
            lblMessage.setText("Excellent ! 🌟");
            lblEncouragement.setText("Tu es sur la bonne voie. Bravo !");
        } else {
            lblMessage.setText("INCROYABLE ! 🚀");
            lblEncouragement.setText("Quel score impressionnant ! Tu domines ce jeu !");
        }
    }

    /**
     * Affiche les détails du récapitulatif dans vboxDetails
     */
    private void afficherDetailsRecapitulatif() {
        if (vboxDetails == null) return;

        vboxDetails.getChildren().clear();

        // Lettre jouée
        Label lblLettreDetail = new Label("Lettre : " + lettre);
        lblLettreDetail.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        vboxDetails.getChildren().add(lblLettreDetail);

        // Durée
        Label lblDureeDetail = new Label("Durée : " + dureePartie + " secondes");
        lblDureeDetail.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        vboxDetails.getChildren().add(lblDureeDetail);

        // Nombre de mots
        long nbValides = resultats.stream().filter(ResultatPartie::isValide).count();
        long nbRejetes = resultats.size() - nbValides;

        Label lblMotsValides = new Label("✓ Mots valides : " + nbValides + "/" + resultats.size());
        lblMotsValides.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");
        vboxDetails.getChildren().add(lblMotsValides);

        if (nbRejetes > 0) {
            Label lblMotsRejetes = new Label("✗ Mots rejetés : " + nbRejetes);
            lblMotsRejetes.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
            vboxDetails.getChildren().add(lblMotsRejetes);
        }

        // Catégories et réponses
        Label titleCategories = new Label("Détail des réponses :");
        titleCategories.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #34495e; -fx-padding: 15 0 10 0;");
        vboxDetails.getChildren().add(titleCategories);

        // Tableau simple des résultats
        for (ResultatPartie resultat : resultats) {
            HBox ligneResultat = new HBox(10);
            ligneResultat.setPadding(new Insets(8, 0, 8, 0));
            ligneResultat.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");

            // Statut
            String statut = resultat.isValide() ? "✓" : "✗";
            String couleur = resultat.isValide() ? "#27ae60" : "#e74c3c";
            Label lblStatut = new Label(statut);
            lblStatut.setStyle("-fx-font-weight: bold; -fx-text-fill: " + couleur + "; -fx-font-size: 14px;");
            lblStatut.setPrefWidth(20);

            // Catégorie
            Label lblCategorie = new Label(resultat.getCategorie());
            lblCategorie.setPrefWidth(100);
            lblCategorie.setStyle("-fx-font-size: 11px;");

            // Mot
            Label lblMot = new Label(resultat.getMot().isEmpty() ? "-" : resultat.getMot());
            lblMot.setPrefWidth(150);
            lblMot.setStyle("-fx-font-size: 11px; -fx-text-fill: #34495e;");

            // Points
            Label lblPts = new Label(resultat.getPoints() + " pts");
            lblPts.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

            ligneResultat.getChildren().addAll(lblStatut, lblCategorie, lblMot, lblPts);
            vboxDetails.getChildren().add(ligneResultat);
        }
    }

    /* =======================
       ACTIONS BUTTONS
       ======================= */

    @FXML
    public void handleRetourMenu(ActionEvent event) {
        navigateTo("/fxml/MenuPrincipal.fxml", "Menu Principal");
    }

    @FXML
    public void handleMenu(ActionEvent event) {
        navigateTo("/fxml/MenuPrincipal.fxml", "Menu Principal");
    }

    @FXML
    public void handleReessayer(ActionEvent event) {
        navigateTo("/fxml/MenuPrincipal.fxml", "Menu Principal");
    }

    @FXML
    public void handleRevanche(ActionEvent event) {
        navigateTo("/fxml/MenuPrincipal.fxml", "Menu Principal");
    }

    /* =======================
       NAVIGATION
       ======================= */

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource(fxmlPath)
            );
            Parent root = loader.load();

            // Obtenir la Stage depuis n'importe quel composant visible
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

    /* =======================
       UTILITAIRES
       ======================= */

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}