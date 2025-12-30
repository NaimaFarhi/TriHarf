package org.example.triharf.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import org.example.triharf.HelloApplication;
import org.example.triharf.services.GameEngine;
import org.example.triharf.services.ResultsManager;
import org.example.triharf.services.ValidationService;
import org.example.triharf.models.Categorie;
import org.example.triharf.models.ResultatPartie;
import org.example.triharf.dao.CategorieDAO;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JeuSoloController.java
 * Lie GameEngine + ResultsManager + ValidationService
 * Flux : Démarrer → Jouer → Valider → Afficher résultats
 */
public class JeuSoloController {

    // ===== UI COMPONENTS =====
    @FXML
    private Label lblTimer;

    @FXML
    private Label lblLettre;

    @FXML
    private Label lblScore;

    @FXML
    private VBox containerCategories;

    @FXML
    private Button btnTerminer;

    // ===== SERVICES (Backend Layer - F) =====
    private GameEngine gameEngine;
    private ResultsManager resultsManager;
    private ValidationService validationService;

    // ===== STATE MANAGEMENT =====
    private Character lettreActuelle;
    private int scorePreview = 0;
    private final Map<String, TextField> textFieldsParCategorie = new HashMap<>();
    private final Map<Categorie, String> reponses = new HashMap<>();

    // ===== INJECTED DATA =====
    private List<String> categoriesNoms; // Reçoit les noms de categories
    private List<Categorie> categories; // Objets Categorie complets
    private int difficulte;
    private String joueur;
    private int gameDuration = 180; // 3 minutes

    // ===== DAO =====
    private CategorieDAO categorieDAO = new CategorieDAO();

    /* =======================
       INJECTION METHODS
       ======================= */

    public void setCategories(List<String> categoriesNoms) {
        this.categoriesNoms = categoriesNoms;
        // Convertir les noms en objets Categorie
        this.categories = new ArrayList<>();
        for (String nom : categoriesNoms) {
            Categorie cat = categorieDAO.findByNom(nom);
            if (cat != null) {
                this.categories.add(cat);
            }
        }
        System.out.println("Catégories converties: " + categories.size());
    }

    public void setDifficulte(int difficulte) {
        this.difficulte = difficulte;
    }

    public void setJoueur(String joueur) {
        this.joueur = joueur;
    }

    /* =======================
       INITIALIZATION
       ======================= */

    @FXML
    public void initialize() {
        this.gameEngine = new GameEngine();
        this.validationService = new ValidationService();
        this.resultsManager = new ResultsManager(gameDuration);
        System.out.println("✅ JeuSoloController initialisé");
    }

    /* =======================
       DÉMARRAGE DE LA PARTIE
       ======================= */

    public void demarrerPartie() {
        if (categories == null || categories.isEmpty()) {
            System.err.println("❌ ERREUR : Aucune catégorie reçue !");
            showAlert("Erreur", "Aucune catégorie sélectionnée !");
            return;
        }

        if (joueur == null || joueur.trim().isEmpty()) {
            joueur = "Joueur_Anonyme";
        }

        System.out.println("✅ Démarrage partie");
        System.out.println("   Joueur: " + joueur);
        System.out.println("   Catégories: " + categories.size());
        System.out.println("   Difficulté: " + difficulte);

        try {
            // ============================================
            // 1️⃣ GÉNÉRER LETTRE
            // ============================================
            lettreActuelle = gameEngine.generateRandomLetter();
            afficherLettre();

            // ============================================
            // 2️⃣ CRÉER UI DYNAMIQUE
            // ============================================
            creerChampsDynamiquement();

            // ============================================
            // 3️⃣ SETUP LISTENERS
            // ============================================
            ajouterListenersScore();
            mettreAJourScore();

            // ============================================
            // 4️⃣ DÉMARRER TIMER
            // ============================================
            gameEngine.setOnTimerUpdate(this::afficherTimer);
            gameEngine.setOnGameEnd(this::handleTerminerAuto);
            gameEngine.startTimer(gameDuration);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de démarrer la partie");
        }
    }

    /* =======================
       UI DYNAMIQUE
       ======================= */

    private void creerChampsDynamiquement() {
        containerCategories.getChildren().clear();
        textFieldsParCategorie.clear();
        reponses.clear();

        for (Categorie categorie : categories) {
            HBox ligne = new HBox(15);
            ligne.setPadding(new Insets(10));
            ligne.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5;");

            Label labelCategorie = new Label(categorie.getNom());
            labelCategorie.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
            labelCategorie.setMinWidth(120);

            TextField textField = new TextField();
            textField.setPromptText("Entrez une réponse...");
            textField.setPrefWidth(300);
            textField.setStyle("-fx-font-size: 12;");

            textFieldsParCategorie.put(categorie.getNom(), textField);
            reponses.put(categorie, "");

            ligne.getChildren().addAll(labelCategorie, textField);
            containerCategories.getChildren().add(ligne);
        }
    }

    private void ajouterListenersScore() {
        for (TextField tf : textFieldsParCategorie.values()) {
            tf.textProperty().addListener((obs, oldVal, newVal) -> mettreAJourScore());
        }
    }

    /* =======================
       LOGIQUE DU JEU
       ======================= */

    private void afficherLettre() {
        lblLettre.setText(lettreActuelle.toString());
        lblLettre.setStyle("-fx-font-size: 48; -fx-font-weight: bold; -fx-text-fill: #FF6B6B;");
    }

    private void afficherTimer() {
        lblTimer.setText(gameEngine.formatTime());
    }

    private void mettreAJourScore() {
        scorePreview = 0;
        for (TextField tf : textFieldsParCategorie.values()) {
            String reponse = tf.getText().trim();
            if (!reponse.isEmpty() && Character.toLowerCase(reponse.charAt(0)) == Character.toLowerCase(lettreActuelle)) {
                scorePreview += 10; // Score de preview
            }
        }
        lblScore.setText(scorePreview + " pts (aperçu)");
    }

    private void recupererReponses() {
        reponses.clear();
        int index = 0;
        for (Categorie categorie : categories) {
            TextField tf = textFieldsParCategorie.get(categorie.getNom());
            if (tf != null) {
                reponses.put(categorie, tf.getText().trim());
            }
            index++;
        }
    }

    /* =======================
       FIN DE PARTIE
       ======================= */

    @FXML
    public void handleTerminer(ActionEvent event) {
        terminerPartie();
    }

    public void handleTerminerAuto() {
        terminerPartie();
    }

    private void terminerPartie() {
        try {
            gameEngine.stopTimer();
            recupererReponses();

            System.out.println("🏁 Partie terminée");
            System.out.println("   Lettre: " + lettreActuelle);
            System.out.println("   Réponses: " + reponses.size());

            // ============================================
            // 1️⃣ VALIDER LES MOTS via ResultsManager
            // Ceci utilise ValidationService en interne
            // ============================================
            resultsManager.validerMots(reponses, lettreActuelle);

            // ============================================
            // 2️⃣ RÉCUPÉRER LES RÉSULTATS
            // ============================================
            List<ResultatPartie> resultats = resultsManager.getResultats();
            int scoreTotal = resultsManager.getScoreTotal();
            long dureePartie = resultsManager.getDureePartie();

            System.out.println("✅ Validation complète");
            System.out.println("   Score total: " + scoreTotal);
            System.out.println("   Durée: " + dureePartie + "s");
            System.out.println("   Résultats: " + resultats.size());

            // ============================================
            // 3️⃣ NAVIGUER VERS RÉSULTATS avec les données
            // ============================================
            navigateToResults(resultats, scoreTotal, dureePartie);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la fermeture: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la validation: " + e.getMessage());
        }
    }

    /* =======================
       NAVIGATION
       ======================= */

    private void navigateToResults(List<ResultatPartie> resultats, int scoreTotal, long dureePartie) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/Resultats.fxml")
            );
            Parent root = loader.load();

            // ⚠️ CRUCIAL : Passer les données au controller suivant
            ResultatsController resultatsController = loader.getController();
            resultatsController.displayResults(resultats, scoreTotal, dureePartie, joueur, lettreActuelle);

            // Obtenir la Stage de manière sécurisée
            Stage stage = null;
            if (btnTerminer != null && btnTerminer.getScene() != null) {
                stage = (Stage) btnTerminer.getScene().getWindow();
            } else {
                System.err.println("❌ Impossible de trouver la Stage via btnTerminer");
                return;
            }

            if (stage != null) {
                stage.setTitle("Résultats de la Partie");
                stage.setScene(new Scene(root));
                stage.show();
                System.out.println("✅ Navigation vers Résultats réussie");
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'affichage des résultats");
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