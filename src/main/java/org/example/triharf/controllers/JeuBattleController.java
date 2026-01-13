package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import org.example.triharf.HelloApplication;
import org.example.triharf.enums.Langue;
import org.example.triharf.services.GameEngine;
import org.example.triharf.services.ResultsManager;
import org.example.triharf.services.ValidationService;
import org.example.triharf.models.Categorie;
import org.example.triharf.models.ResultatPartie;
import org.example.triharf.dao.CategorieDAO;

import java.io.IOException;
import java.util.*;

/**
 * JeuBattleController.java
 * Gère le jeu Battle Royale.
 * Similaire à JeuMultiController pour l'instant.
 */
public class JeuBattleController {

    // ===== UI COMPONENTS =====
    @FXML
    private Button btnRetour;

    @FXML
    private Label labelTimer;

    @FXML
    private Label labelNbJoueurs;

    @FXML
    private Label lblLettre;

    @FXML
    private VBox containerCategories;

    @FXML
    private VBox vboxPlayers;

    @FXML
    private VBox vboxMessages;

    @FXML
    private TextField tfMessageChat;

    @FXML
    private Button btnEnvoyer;

    @FXML
    private HBox hboxTableHeader;

    @FXML
    private Button btnTerminer;

    // ===== SERVICES (Backend Layer - F) =====
    private GameEngine gameEngine;
    private ResultsManager resultsManager;
    private ValidationService validationService;

    // ===== STATE MANAGEMENT =====
    private Character lettreActuelle;
    private int timeRemaining = 180; // 3 minutes
    private Timeline timeline;
    private int nbJoueurs = 1; // Sera mis à jour via réseau
    private final Map<String, TextField> textFieldsParCategorie = new HashMap<>();
    private final Map<Categorie, String> reponses = new HashMap<>();
    private Map<String, Integer> playerFinalScores = new HashMap<>(); // For multiplayer results

    // ===== INJECTED DATA =====
    private List<String> categoriesNoms;
    private List<Categorie> categories;
    private String joueur = "Joueur_Battle";
    private int difficulte = 1;
    private int gameDuration = 180;
    private Langue langue = Langue.FRANCAIS; // 👈 AJOUT: Langue par défaut en français

    // ===== DAO =====
    private CategorieDAO categorieDAO = new CategorieDAO();

    /*
     * =======================
     * INJECTION METHODS
     * =======================
     */

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
        System.out.println("✅ Catégories Battle converties: " + categories.size());
    }

    public void setJoueur(String joueur) {
        this.joueur = joueur;
    }

    public void setDifficulte(int difficulte) {
        this.difficulte = difficulte;
    }

    // 👈 AJOUT: Setter pour la langue si besoin
    public void setLangue(Langue langue) {
        this.langue = langue;
    }

    /*
     * =======================
     * INITIALIZATION
     * =======================
     */

    @FXML
    public void initialize() {
        System.out.println("✅ JeuBattleController initialisé");

        this.gameEngine = new GameEngine();
        this.validationService = new ValidationService();
        this.resultsManager = new ResultsManager(gameDuration);

        if (btnRetour != null)
            btnRetour.setOnAction(e -> retourMenu());

        if (btnTerminer != null)
            btnTerminer.setOnAction(e -> handleTerminer());
    }

    /*
     * =======================
     * DÉMARRAGE DE LA PARTIE
     * =======================
     */

    public void demarrerPartie() {
        if (categories == null || categories.isEmpty()) {
            System.err.println("❌ ERREUR : Aucune catégorie reçue !");
            return;
        }

        // UTILISATION DES PARAMETRES GLOBAUX
        this.joueur = ParametresGenerauxController.pseudoGlobal;
        this.langue = ParametresGenerauxController.langueGlobale;

        System.out.println("✅ Démarrage partie Battle Royale");
        System.out.println("   Joueur: " + joueur);
        System.out.println("   Langue: " + langue);
        System.out.println("   Catégories: " + categories.size());

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

            // ============================================
            // 4️⃣ DÉMARRER TIMER
            // ============================================
            gameEngine.setOnTimerUpdate(this::afficherTimer);
            gameEngine.setOnGameEnd(this::handleTerminerAuto);
            gameEngine.startTimer(gameDuration);

            System.out.println("✅ Partie Battle démarrée");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * =======================
     * UI DYNAMIQUE
     * =======================
     */

    private void creerChampsDynamiquement() {
        if (containerCategories == null)
            return;

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

            textFieldsParCategorie.put(categorie.getNom(), textField);
            reponses.put(categorie, "");

            ligne.getChildren().addAll(labelCategorie, textField);
            containerCategories.getChildren().add(ligne);
        }
    }

    private void ajouterListenersScore() {
        for (TextField tf : textFieldsParCategorie.values()) {
            tf.textProperty().addListener((obs, oldVal, newVal) -> {
                // Update reponses
            });
        }
    }

    /*
     * =======================
     * AFFICHAGE
     * =======================
     */

    private void afficherLettre() {
        if (lblLettre != null) {
            lblLettre.setText(lettreActuelle.toString());
            lblLettre.setStyle("-fx-font-size: 48; -fx-font-weight: bold; -fx-text-fill: #e74c3c;"); // Red for Battle
        }
    }

    private void afficherTimer() {
        if (labelTimer != null) {
            labelTimer.setText(gameEngine.formatTime());
        }
    }

    private void recupererReponses() {
        reponses.clear();
        for (Categorie categorie : categories) {
            TextField tf = textFieldsParCategorie.get(categorie.getNom());
            if (tf != null) {
                reponses.put(categorie, tf.getText().trim());
            }
        }
    }

    /*
     * =======================
     * CHAT
     * =======================
     */

    @FXML
    private void handleSendMessage() {
        if (tfMessageChat == null || vboxMessages == null)
            return;

        String message = tfMessageChat.getText().trim();
        if (message.isEmpty())
            return;

        // Ajouter un label pour le message
        Label msgLabel = new Label(joueur + ": " + message);
        msgLabel.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");
        vboxMessages.getChildren().add(msgLabel);

        tfMessageChat.clear();
    }

    /*
     * =======================
     * FIN DE PARTIE
     * =======================
     */

    @FXML
    private void handleTerminer() {
        terminerPartie();
    }

    private void handleTerminerAuto() {
        terminerPartie();
    }

    private void terminerPartie() {
        try {
            gameEngine.stopTimer();
            recupererReponses();

            System.out.println("🏁 Partie Battle terminée");

            // ============================================
            // 1️⃣ VALIDER
            // ============================================
            resultsManager.validerMots(reponses, lettreActuelle, langue);

            // ============================================
            // 2️⃣ RÉSULTATS
            // ============================================
            int scoreTotal = resultsManager.getScoreTotal();

            // Store score for multiplayer results
            playerFinalScores.clear();
            playerFinalScores.put(joueur, scoreTotal);

            System.out.println("✅ Score Battle: " + scoreTotal);

            // ============================================
            // 3️⃣ NAVIGUER vers résultats multijoueur
            // ============================================
            navigateToMultiplayerResults();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * =======================
     * NAVIGATION
     * =======================
     */

    private void navigateToMultiplayerResults() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/resultats_multi.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ResultatsMultiController) {
                ResultatsMultiController rc = (ResultatsMultiController) controller;
                rc.displayRanking(playerFinalScores, joueur, lettreActuelle);
            }

            Stage stage = null;
            if (btnTerminer != null && btnTerminer.getScene() != null) {
                stage = (Stage) btnTerminer.getScene().getWindow();
            } else if (btnRetour != null && btnRetour.getScene() != null) {
                stage = (Stage) btnRetour.getScene().getWindow();
            }

            if (stage != null) {
                stage.getScene().setRoot(root);
                stage.setTitle("Résultats Battle Royale");
            }

        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to menu
            retourMenu();
        }
    }

    private void retourMenu() {
        if (timeline != null) {
            timeline.stop();
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/main_menu.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setTitle("Menu Principal");
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}