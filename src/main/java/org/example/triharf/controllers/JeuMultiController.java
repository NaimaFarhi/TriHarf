package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import org.example.triharf.HelloApplication;
import org.example.triharf.services.GameEngine;
import org.example.triharf.services.ResultsManager;
import org.example.triharf.services.ValidationService;
import org.example.triharf.models.Categorie;
import org.example.triharf.models.ResultatPartie;
import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.network.GameClient;
import org.example.triharf.network.NetworkMessage;

import java.io.IOException;
import java.util.*;

public class JeuMultiController {

    // ===== UI COMPONENTS (MATCHING FXML) =====
    @FXML private Button btnBack;
    @FXML private Label lblTimer;
    @FXML private Label lblJoueurs;
    @FXML private Label lblLettre;
    @FXML private VBox vboxPlayers;
    @FXML private VBox vboxMessages;
    @FXML private TextField tfMessage;
    @FXML private Button btnSend;

    // ===== SERVICES =====
    private GameEngine gameEngine;
    private ResultsManager resultsManager;
    private ValidationService validationService;

    // ===== STATE MANAGEMENT =====
    private Character lettreActuelle;
    private Timeline timeline;
    private int nbJoueurs = 1;
    private final Map<String, TextField> textFieldsParCategorie = new HashMap<>();
    private final Map<Categorie, String> reponses = new HashMap<>();

    // ===== INJECTED DATA =====
    private List<String> categoriesNoms;
    private List<Categorie> categories;
    private String joueur = "Joueur_Multi";
    private int gameDuration = 180;
    private org.example.triharf.enums.Langue langue = org.example.triharf.enums.Langue.FRANCAIS;

    // ===== DAO =====
    private CategorieDAO categorieDAO = new CategorieDAO();

    // ===== NETWORK =====
    private GameClient gameClient;
    private String roomId;

    public void setNetwork(GameClient client, String roomId) {
        this.gameClient = client;
        this.roomId = roomId;
        if (this.gameClient != null) {
            this.gameClient.setMessageHandler(this::handleNetworkMessage);
        }
    }

    private void handleNetworkMessage(NetworkMessage message) {
        javafx.application.Platform.runLater(() -> {
            if (message.getType() == NetworkMessage.Type.GAME_START) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) message.getData();

                // Get categories
                @SuppressWarnings("unchecked")
                List<String> cats = (List<String>) data.get("categories");
                if (cats != null) {
                    setCategories(cats);
                    creerChampsDynamiquement();
                }

                // Get letter
                String letter = (String) data.get("letter");
                if (letter != null) {
                    lettreActuelle = letter.charAt(0);
                    afficherLettre();
                }

                demarrerPartie();
            }
        });
    }

    /* =======================
       INJECTION METHODS
       ======================= */

    public void setCategories(List<String> categoriesNoms) {
        this.categoriesNoms = categoriesNoms;
        this.categories = new ArrayList<>();
        for (String nom : categoriesNoms) {
            Categorie cat = categorieDAO.findByNom(nom);
            if (cat != null) {
                this.categories.add(cat);
            }
        }
        System.out.println("✅ Catégories converties: " + categories.size());
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

        this.joueur = ParametresGenerauxController.pseudoGlobal;
        this.langue = ParametresGenerauxController.langueGlobale;

        System.out.println("✅ Démarrage partie multijoueur");
        System.out.println("   Joueur: " + joueur);
        System.out.println("   Langue: " + langue);
        System.out.println("   Catégories: " + categories.size());

        try {
            // Create category input fields in player list area
            creerChampsDynamiquement();

            // Start timer
            gameEngine.setOnTimerUpdate(this::afficherTimer);
            gameEngine.setOnGameEnd(this::handleTerminerAuto);
            gameEngine.startTimer(gameDuration);

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
        if (vboxPlayers == null) return;

        vboxPlayers.getChildren().clear();
        textFieldsParCategorie.clear();
        reponses.clear();

        for (Categorie categorie : categories) {
            HBox ligne = new HBox(15);
            ligne.setPadding(new Insets(10));
            ligne.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-border-color: #555; -fx-border-radius: 5;");

            Label labelCategorie = new Label(categorie.getNom());
            labelCategorie.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
            labelCategorie.setMinWidth(120);

            TextField textField = new TextField();
            textField.setPromptText("Entrez une réponse...");
            textField.setPrefWidth(200);

            textFieldsParCategorie.put(categorie.getNom(), textField);
            reponses.put(categorie, "");

            ligne.getChildren().addAll(labelCategorie, textField);
            vboxPlayers.getChildren().add(ligne);
        }
    }

    /* =======================
       AFFICHAGE
       ======================= */

    private void afficherLettre() {
        if (lblLettre != null) {
            lblLettre.setText(lettreActuelle.toString());
        }
    }

    private void afficherTimer() {
        if (lblTimer != null) {
            lblTimer.setText(gameEngine.formatTime());
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

    @FXML
    private void handleBack() {
        retourMenu();
    }

    /* =======================
       CHAT
       ======================= */

    @FXML
    private void handleSendMessage() {
        if (tfMessage == null || vboxMessages == null) return;

        String message = tfMessage.getText().trim();
        if (message.isEmpty()) return;

        // Add to local chat
        Label msgLabel = new Label(joueur + ": " + message);
        msgLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        vboxMessages.getChildren().add(msgLabel);
        tfMessage.clear();

        // TODO: Send to server
        System.out.println("📤 Message envoyé: " + message);
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

            // Validate words
            resultsManager.validerMots(reponses, lettreActuelle, langue);

            // Get results
            List<ResultatPartie> resultats = resultsManager.getResultats();
            int scoreTotal = resultsManager.getScoreTotal();
            long dureePartie = resultsManager.getDureePartie();

            System.out.println("✅ Validation complète");
            System.out.println("   Score total: " + scoreTotal);

            // Send to server
            if (gameClient != null) {
                Map<String, String> data = new HashMap<>();
                data.put("score", String.valueOf(scoreTotal));
                gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.SUBMIT_ANSWER, joueur, data));
            }

            // Navigate to results
            navigateToResults(resultats, scoreTotal, dureePartie);

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
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

            ResultatsController resultatsController = loader.getController();
            resultatsController.displayResults(resultats, scoreTotal, dureePartie, joueur, lettreActuelle);

            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.setTitle("Résultats Multijoueur");
            stage.setScene(new Scene(root));
            stage.show();

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

            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.setTitle("Menu Principal");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
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