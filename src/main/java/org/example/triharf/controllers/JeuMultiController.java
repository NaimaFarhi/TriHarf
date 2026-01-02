package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
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
 * JeuMultiController.java
 * Gère le jeu multijoueur
 * Lie GameEngine + ResultsManager + ValidationService + Réseau
 */
public class JeuMultiController {

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
    private TableView<?> tableJoueurs;

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField tfMessageChat;

    @FXML
    private Button btnEnvoyer;

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

    // ===== INJECTED DATA =====
    private List<String> categoriesNoms;
    private List<Categorie> categories;
    private String joueur = "Joueur_Multi";
    private int difficulte = 1;
    private int gameDuration = 180;

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
        System.out.println("✅ Catégories converties: " + categories.size());
    }

    public void setJoueur(String joueur) {
        this.joueur = joueur;
    }

    public void setDifficulte(int difficulte) {
        this.difficulte = difficulte;
    }

    /* =======================
       INITIALIZATION
       ======================= */

    @FXML
    public void initialize() {
        System.out.println("✅ JeuMultiController initialisé");

        this.gameEngine = new GameEngine();
        this.validationService = new ValidationService();
        this.resultsManager = new ResultsManager(gameDuration);

        if (btnRetour != null) btnRetour.setOnAction(e -> retourMenu());
        if (btnEnvoyer != null) btnEnvoyer.setOnAction(e -> envoyerMessage());
        if (btnTerminer != null) btnTerminer.setOnAction(e -> handleTerminer());
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

        System.out.println("✅ Démarrage partie multijoueur");
        System.out.println("   Joueur: " + joueur);
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

            // ============================================
            // 5️⃣ LIAISON RÉSEAU (TODO)
            // ============================================
            // connecterAuServeur();
            System.out.println("✅ Partie multijoueur démarrée");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* =======================
       UI DYNAMIQUE
       ======================= */

    private void creerChampsDynamiquement() {
        if (containerCategories == null) return;

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

    /* =======================
       AFFICHAGE
       ======================= */

    private void afficherLettre() {
        if (lblLettre != null) {
            lblLettre.setText(lettreActuelle.toString());
            lblLettre.setStyle("-fx-font-size: 48; -fx-font-weight: bold; -fx-text-fill: #FF6B6B;");
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

    /* =======================
       CHAT
       ======================= */

    @FXML
    private void envoyerMessage() {
        if (tfMessageChat == null || chatArea == null) return;

        String message = tfMessageChat.getText().trim();
        if (message.isEmpty()) return;

        // Ajouter au chat local
        String contenuChat = chatArea.getText();
        chatArea.setText(contenuChat + "\n" + joueur + ": " + message);
        tfMessageChat.clear();

        System.out.println("📤 Message envoyé: " + message);
        // TODO: Envoyer via réseau
    }

    /* =======================
       FIN DE PARTIE
       ======================= */

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

            System.out.println("🏁 Partie multijoueur terminée");

            // ============================================
            // 1️⃣ VALIDER LES MOTS via ResultsManager
            // ============================================
            //resultsManager.validerMots(reponses, lettreActuelle);

            // ============================================
            // 2️⃣ RÉCUPÉRER LES RÉSULTATS
            // ============================================
            List<ResultatPartie> resultats = resultsManager.getResultats();
            int scoreTotal = resultsManager.getScoreTotal();
            long dureePartie = resultsManager.getDureePartie();

            System.out.println("✅ Validation complète");
            System.out.println("   Score total: " + scoreTotal);
            System.out.println("   Durée: " + dureePartie + "s");

            // ============================================
            // 3️⃣ ENVOYER RÉSULTATS AU SERVEUR (TODO)
            // ============================================
            // envoyerResultatsAuServeur(resultats, scoreTotal);

            // ============================================
            // 4️⃣ NAVIGUER VERS RÉSULTATS
            // ============================================
            navigateToResults(resultats, scoreTotal, dureePartie);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la fermeture: " + e.getMessage());
            e.printStackTrace();
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

            // Passer les données au ResultatsController
            ResultatsController resultatsController = loader.getController();
            resultatsController.displayResults(resultats, scoreTotal, dureePartie, joueur, lettreActuelle);

            Stage stage = null;
            if (btnTerminer != null && btnTerminer.getScene() != null) {
                stage = (Stage) btnTerminer.getScene().getWindow();
            } else if (btnRetour != null && btnRetour.getScene() != null) {
                stage = (Stage) btnRetour.getScene().getWindow();
            }

            if (stage != null) {
                stage.setTitle("Résultats Multijoueur");
                stage.setScene(new Scene(root));
                stage.show();
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void retourMenu() {
        if (timeline != null) {
            timeline.stop();
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/main_menu.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setTitle("Menu Principal");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
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

    /* =======================
       RÉSEAU (TODO)
       ======================= */

    // private void connecterAuServeur() {
    //     // TODO: Connexion au GameServer
    // }

    // private void envoyerResultatsAuServeur(List<ResultatPartie> resultats, int score) {
    //     // TODO: Envoyer résultats via GameClient
    // }
}