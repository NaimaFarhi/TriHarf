package org.example.triharf.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.geometry.Insets;

import org.example.triharf.HelloApplication;
import org.example.triharf.enums.Langue;
import org.example.triharf.models.Categorie;
import org.example.triharf.models.ResultatPartie;
import org.example.triharf.services.GameSession;
import org.example.triharf.dao.CategorieDAO;

import java.io.IOException;
import java.util.*;

/**
 * JeuSoloController
 * UI SEULEMENT
 * logique métier est dans GameSession
 */
public class JeuSoloController {

    // ================= UI =================
    @FXML
    private Label lblTimer;
    @FXML
    private Label lblLettre;
    @FXML
    private FlowPane containerCategories;
    @FXML
    private Button btnTerminer;
    @FXML
    private Button btnRetour;

    // ================= STATE =================
    private GameSession gameSession;
    private Langue langue;
    private String joueur;

    private List<Categorie> categories = new ArrayList<>();
    private final Map<Categorie, TextField> champsParCategorie = new HashMap<>();

    private final CategorieDAO categorieDAO = new CategorieDAO();

    // ================= INJECTION =================

    public void setLangue(Langue langue) {
        this.langue = langue;
    }

    public void setJoueur(String joueur) {
        this.joueur = joueur;
    }

    public void setCategories(List<String> nomsCategories) {
        categories.clear();
        for (String nom : nomsCategories) {
            Categorie c = categorieDAO.findByNom(nom);
            if (c != null) {
                categories.add(c);
            }
        }
        System.out.println("✅ Catégories chargées: " + categories.size());
    }

    // ================= INIT =================

    @FXML
    public void initialize() {

        btnRetour.setOnAction(e -> retourMenu());
        btnTerminer.setOnAction(this::handleTerminer);

        System.out.println("✅ JeuSoloController initialisé");
    }

    // ================= DÉMARRAGE =================

    public void demarrerPartie() {

        if (categories.isEmpty()) {
            showAlert("Erreur", "Aucune catégorie sélectionnée");
            return;
        }

        // Fallback: Si pas injectés, on prend les globaux
        if (joueur == null)
            joueur = ParametresGenerauxController.pseudoGlobal;
        if (langue == null)
            langue = ParametresGenerauxController.langueGlobale;

        gameSession = new GameSession(joueur, langue);

        // Calcul durée selon difficulté (0=Facile=180s, 1=Moyen=120s, 2=Difficile=60s)
        int dureeSecondes = switch (difficulte) {
            case 0 -> 180; // 3 min
            case 1 -> 120; // 2 min
            case 2 -> 60; // 1 min
            default -> 120;
        };

        gameSession.demarrerPartie(dureeSecondes);

        // Lettre
        lblLettre.setText(gameSession.getLettre().toString());

        // Timer
        gameSession.getGameEngine().setOnTimerUpdate(() -> lblTimer.setText(gameSession.getGameEngine().formatTime()));

        gameSession.getGameEngine().setOnGameEnd(this::terminerPartie);

        creerChamps();
    }

    // ================= UI DYNAMIQUE =================

    private void creerChamps() {
        containerCategories.getChildren().clear();
        champsParCategorie.clear();

        // Array of accent colors for variety
        String[] accentColors = {
                "#9b59b6", "#3498db", "#e74c3c", "#2ecc71",
                "#f39c12", "#1abc9c", "#e91e63", "#00bcd4"
        };

        int colorIndex = 0;
        for (Categorie categorie : categories) {
            String accentColor = accentColors[colorIndex % accentColors.length];
            colorIndex++;

            VBox box = new VBox(12);
            box.setPadding(new Insets(18));
            box.setPrefWidth(200);
            box.setMinWidth(180);
            box.setStyle(String.format(
                    "-fx-background-color: rgba(40, 40, 60, 0.8); " +
                            "-fx-background-radius: 15; " +
                            "-fx-border-color: %s55; " +
                            "-fx-border-radius: 15; " +
                            "-fx-border-width: 2; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 10, 0, 0, 3);",
                    accentColor));

            // Category label with icon
            Label label = new Label("📁 " + categorie.getNom());
            label.setStyle(String.format(
                    "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: %s;",
                    accentColor));

            // Text field with dark styling
            TextField tf = new TextField();
            tf.setPromptText("Entrez un mot...");
            tf.setStyle(
                    "-fx-background-color: rgba(0, 0, 0, 0.3); " +
                            "-fx-text-fill: white; " +
                            "-fx-prompt-text-fill: #777; " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: rgba(255, 255, 255, 0.15); " +
                            "-fx-border-radius: 8; " +
                            "-fx-padding: 10; " +
                            "-fx-font-size: 14px;");

            // Add focus effect
            tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    tf.setStyle(
                            "-fx-background-color: rgba(0, 0, 0, 0.4); " +
                                    "-fx-text-fill: white; " +
                                    "-fx-prompt-text-fill: #888; " +
                                    "-fx-background-radius: 8; " +
                                    "-fx-border-color: " + accentColor + "; " +
                                    "-fx-border-radius: 8; " +
                                    "-fx-padding: 10; " +
                                    "-fx-font-size: 14px;");
                } else {
                    tf.setStyle(
                            "-fx-background-color: rgba(0, 0, 0, 0.3); " +
                                    "-fx-text-fill: white; " +
                                    "-fx-prompt-text-fill: #777; " +
                                    "-fx-background-radius: 8; " +
                                    "-fx-border-color: rgba(255, 255, 255, 0.15); " +
                                    "-fx-border-radius: 8; " +
                                    "-fx-padding: 10; " +
                                    "-fx-font-size: 14px;");
                }
            });

            champsParCategorie.put(categorie, tf);

            box.getChildren().addAll(label, tf);
            containerCategories.getChildren().add(box);
        }
    }

    // ================= FIN DE PARTIE =================

    @FXML
    private void handleTerminer(ActionEvent event) {
        terminerPartie();
    }

    private boolean partieTerminee = false;

    private synchronized void terminerPartie() {
        if (partieTerminee)
            return;
        partieTerminee = true;

        // Récupération réponses UI → GameSession
        for (Map.Entry<Categorie, TextField> entry : champsParCategorie.entrySet()) {
            gameSession.setReponse(entry.getKey(), entry.getValue().getText());
        }

        gameSession.terminerPartie();

        List<ResultatPartie> resultats = gameSession.getResultsManager().getResultats();

        int score = gameSession.getResultsManager().getScoreTotal();
        long duree = gameSession.getResultsManager().getDureePartie();

        navigateToResults(resultats, score, duree);
    }

    // ================= NAVIGATION =================

    private void navigateToResults(
            List<ResultatPartie> resultats,
            int score,
            long duree) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/Resultats.fxml"));
            Parent root = loader.load();

            ResultatsController controller = loader.getController();
            controller.displayResults(
                    resultats,
                    score,
                    duree,
                    joueur,
                    gameSession.getLettre());

            Stage stage = (Stage) btnTerminer.getScene().getWindow();
            if (stage == null && btnRetour.getScene() != null) {
                stage = (Stage) btnRetour.getScene().getWindow();
            }
            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Navigation résultats impossible");
        }
    }

    private int difficulte = 1;

    public void setDifficulte(int difficulte) {
        this.difficulte = difficulte;
        System.out.println("🎯 Difficulté reçue: " + difficulte);
    }

    private void retourMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/main_menu.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= UTILS =================

    private void showAlert(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
