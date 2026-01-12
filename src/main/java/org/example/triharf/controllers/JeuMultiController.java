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
    @FXML private HBox hboxCategoryHeaders;
    @FXML private VBox vboxPlayerRows;
    @FXML private VBox vboxMessages;
    @FXML private TextField tfMessage;
    @FXML private Button btnSend;
    @FXML private Button btnValider;
    @FXML private Label lblValidationStatus;

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

    // ===== MULTIPLAYER STATE =====
    private List<String> playerList = new ArrayList<>();
    private Map<String, Map<String, String>> allPlayerAnswers = new HashMap<>();
    private Map<String, HBox> playerRowMap = new HashMap<>();
    private static final double CATEGORY_WIDTH = 120.0;
    private static final double PLAYER_NAME_WIDTH = 100.0;

    // ===== VALIDATION STATE =====
    private boolean hasValidated = false;
    private Set<String> validatedPlayers = new HashSet<>();
    private boolean allPlayersValidated = false;

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
            switch (message.getType()) {
                case GAME_START -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) message.getData();

                    // Get categories
                    @SuppressWarnings("unchecked")
                    List<String> cats = (List<String>) data.get("categories");
                    if (cats != null && !cats.isEmpty()) {
                        setCategories(cats);
                    }

                    // Get letter
                    String letter = (String) data.get("letter");
                    if (letter != null && !letter.isEmpty()) {
                        lettreActuelle = letter.charAt(0);
                        afficherLettre();
                    }

                    // Get players list
                    @SuppressWarnings("unchecked")
                    List<String> players = (List<String>) data.get("players");
                    if (players != null) {
                        this.playerList = new ArrayList<>(players);
                        updatePlayerCount();
                    }

                    demarrerPartie();
                }
                case CHAT -> {
                    // Receive chat message from another player
                    @SuppressWarnings("unchecked")
                    Map<String, String> chatData = (Map<String, String>) message.getData();
                    String sender = chatData.get("sender");
                    String chatMsg = chatData.get("message");
                    addChatMessage(sender, chatMsg, false);
                }
                case PLAYER_JOINED -> {
                    // Update player list
                    @SuppressWarnings("unchecked")
                    List<String> players = (List<String>) message.getData();
                    if (players != null) {
                        updatePlayersFromStatus(players);
                    }
                }
                case PLAYER_ANSWERS -> {
                    // Receive other player's answers for display
                    @SuppressWarnings("unchecked")
                    Map<String, Object> answerData = (Map<String, Object>) message.getData();
                    String playerName = (String) answerData.get("player");
                    @SuppressWarnings("unchecked")
                    Map<String, String> answers = (Map<String, String>) answerData.get("answers");
                    if (playerName != null && answers != null) {
                        updatePlayerAnswersDisplay(playerName, answers);
                    }
                }
                case VALIDATE_ANSWERS -> {
                    // A player has validated their answers
                    @SuppressWarnings("unchecked")
                    Map<String, Object> validationData = (Map<String, Object>) message.getData();
                    String playerName = (String) validationData.get("player");
                    @SuppressWarnings("unchecked")
                    Map<String, String> answers = (Map<String, String>) validationData.get("answers");
                    if (playerName != null) {
                        validatedPlayers.add(playerName);
                        // Store their answers for later display
                        if (answers != null) {
                            allPlayerAnswers.put(playerName, answers);
                        }
                        updateValidationStatus();
                        // Mark this player's row as validated (show checkmark)
                        markPlayerAsValidated(playerName);
                    }
                }
                case ALL_VALIDATED -> {
                    // All players have validated - reveal all answers
                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, String>> allAnswers = (Map<String, Map<String, String>>) message.getData();
                    if (allAnswers != null) {
                        allPlayerAnswers.putAll(allAnswers);
                    }
                    allPlayersValidated = true;
                    revealAllAnswers();
                }
                default -> {}
            }
        });
    }

    private void updatePlayersFromStatus(List<String> playersStatus) {
        playerList.clear();
        for (String ps : playersStatus) {
            String[] parts = ps.split(":");
            String name = parts[0];
            playerList.add(name);
        }
        updatePlayerCount();
        // Refresh player rows if game already started
        if (categories != null && !categories.isEmpty()) {
            creerChampsDynamiquement();
        }
    }

    private void updatePlayerCount() {
        nbJoueurs = playerList.size();
        if (lblJoueurs != null) {
            lblJoueurs.setText(String.valueOf(nbJoueurs));
        }
    }

    private void addChatMessage(String sender, String message, boolean isLocal) {
        if (vboxMessages == null) return;

        Label msgLabel = new Label(sender + ": " + message);
        String style = isLocal ?
            "-fx-text-fill: #9b59b6; -fx-font-size: 12px; -fx-font-weight: bold;" :
            "-fx-text-fill: white; -fx-font-size: 12px;";
        msgLabel.setStyle(style);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(200);
        vboxMessages.getChildren().add(msgLabel);

        // Auto-scroll to bottom
        if (vboxMessages.getParent() instanceof javafx.scene.control.ScrollPane scrollPane) {
            scrollPane.setVvalue(1.0);
        }
    }

    private void updatePlayerAnswersDisplay(String playerName, Map<String, String> answers) {
        HBox playerRow = playerRowMap.get(playerName);
        if (playerRow == null || playerName.equals(joueur)) return; // Don't update own row

        // Update the labels in the player's row (skip first child which is player name)
        int idx = 1;
        for (Categorie cat : categories) {
            if (idx < playerRow.getChildren().size()) {
                javafx.scene.Node node = playerRow.getChildren().get(idx);
                if (node instanceof Label label) {
                    String answer = answers.getOrDefault(cat.getNom(), "...");
                    label.setText(answer.isEmpty() ? "..." : answer);
                }
            }
            idx++;
        }
    }

    /* =======================
       INJECTION METHODS
       ======================= */

    public void setCategories(List<String> categoriesNoms) {
        if (categoriesNoms == null || categoriesNoms.isEmpty()) {
            System.err.println("⚠️ Liste de catégories vide ou null");
            return;
        }

        this.categoriesNoms = new ArrayList<>(categoriesNoms);
        this.categories = new ArrayList<>();

        for (String nom : categoriesNoms) {
            Categorie cat = categorieDAO.findByNom(nom);
            if (cat != null) {
                this.categories.add(cat);
                System.out.println("   ✓ Catégorie trouvée: " + nom);
            } else {
                // If not found in DB, create a temporary Categorie object for display
                System.out.println("   ⚠️ Catégorie non trouvée en DB, création locale: " + nom);
                Categorie tempCat = new Categorie(nom);
                tempCat.setLangue(langue);
                this.categories.add(tempCat);
            }
        }
        System.out.println("✅ Catégories converties: " + categories.size() + " sur " + categoriesNoms.size());
    }

    public void setLettre(String letter) {
        if (letter != null && !letter.isEmpty()) {
            this.lettreActuelle = letter.charAt(0);
            System.out.println("✅ Lettre définie: " + lettreActuelle);
            afficherLettre();
        }
    }

    public void setPlayerList(List<String> players) {
        if (players != null && !players.isEmpty()) {
            this.playerList = new ArrayList<>(players);
            this.nbJoueurs = players.size();
            System.out.println("✅ Liste des joueurs définie: " + players);
            if (lblJoueurs != null) {
                lblJoueurs.setText(String.valueOf(nbJoueurs));
            }
        }
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
            System.err.println("   categoriesNoms: " + (categoriesNoms != null ? categoriesNoms.size() : "null"));
            showAlert("Erreur", "Aucune catégorie sélectionnée !");
            return;
        }

        // Set player name with fallback
        String pseudoGlobal = ParametresGenerauxController.pseudoGlobal;
        this.joueur = (pseudoGlobal != null && !pseudoGlobal.isEmpty()) ? pseudoGlobal : "Joueur_" + System.currentTimeMillis() % 1000;
        this.langue = ParametresGenerauxController.langueGlobale != null ? ParametresGenerauxController.langueGlobale : org.example.triharf.enums.Langue.FRANCAIS;

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
        if (vboxPlayerRows == null && vboxPlayers == null) return;

        textFieldsParCategorie.clear();
        reponses.clear();
        playerRowMap.clear();

        // Create header row with category names
        if (hboxCategoryHeaders != null) {
            hboxCategoryHeaders.getChildren().clear();
            hboxCategoryHeaders.setSpacing(5);

            // First column: "Joueur" header
            Label playerHeader = new Label("Joueur");
            playerHeader.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #9b59b6;");
            playerHeader.setMinWidth(PLAYER_NAME_WIDTH);
            playerHeader.setPrefWidth(PLAYER_NAME_WIDTH);
            hboxCategoryHeaders.getChildren().add(playerHeader);

            // Category columns
            for (Categorie categorie : categories) {
                Label catLabel = new Label(categorie.getNom());
                catLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #9b59b6;");
                catLabel.setMinWidth(CATEGORY_WIDTH);
                catLabel.setPrefWidth(CATEGORY_WIDTH);
                catLabel.setAlignment(javafx.geometry.Pos.CENTER);
                hboxCategoryHeaders.getChildren().add(catLabel);
            }
        }

        // Use vboxPlayerRows if available, otherwise fallback to vboxPlayers
        VBox targetVBox = vboxPlayerRows != null ? vboxPlayerRows : vboxPlayers;
        targetVBox.getChildren().clear();

        // Ensure current player is in the list
        if (!playerList.contains(joueur)) {
            playerList.add(0, joueur);
        }

        // Create a row for each player
        for (String playerName : playerList) {
            HBox playerRow = createPlayerRow(playerName);
            targetVBox.getChildren().add(playerRow);
            playerRowMap.put(playerName, playerRow);
        }

        System.out.println("✅ Table créée: " + categories.size() + " catégories, " + playerList.size() + " joueurs");
    }

    private HBox createPlayerRow(String playerName) {
        boolean isCurrentPlayer = playerName.equals(joueur);

        HBox row = new HBox(5);
        row.setPadding(new Insets(8));
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Highlight current player's row
        String bgColor = isCurrentPlayer ?
            "-fx-background-color: rgba(155, 89, 182, 0.2); -fx-border-color: #9b59b6; -fx-border-radius: 5; -fx-background-radius: 5;" :
            "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5;";
        row.setStyle(bgColor);

        // Player name label
        Label nameLabel = new Label(isCurrentPlayer ? "➤ " + playerName : playerName);
        nameLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: " + (isCurrentPlayer ? "#9b59b6" : "white") + ";");
        nameLabel.setMinWidth(PLAYER_NAME_WIDTH);
        nameLabel.setPrefWidth(PLAYER_NAME_WIDTH);
        row.getChildren().add(nameLabel);

        // Create input fields or display labels for each category
        for (Categorie categorie : categories) {
            if (isCurrentPlayer) {
                // Current player gets editable text fields
                TextField textField = new TextField();
                textField.setPromptText(categorie.getNom().substring(0, Math.min(3, categorie.getNom().length())) + "...");
                textField.setPrefWidth(CATEGORY_WIDTH);
                textField.setMinWidth(CATEGORY_WIDTH);
                textField.setStyle("-fx-font-size: 11;");

                textFieldsParCategorie.put(categorie.getNom(), textField);
                reponses.put(categorie, "");

                row.getChildren().add(textField);
            } else {
                // Other players get read-only labels that will be updated via network
                Label answerLabel = new Label("...");
                answerLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #aaa;");
                answerLabel.setMinWidth(CATEGORY_WIDTH);
                answerLabel.setPrefWidth(CATEGORY_WIDTH);
                answerLabel.setAlignment(javafx.geometry.Pos.CENTER);

                row.getChildren().add(answerLabel);
            }
        }

        return row;
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

        // Add to local chat (highlighted as own message)
        addChatMessage(joueur, message, true);
        tfMessage.clear();

        // Send to server for broadcast to other players
        if (gameClient != null) {
            Map<String, String> chatData = new HashMap<>();
            chatData.put("sender", joueur);
            chatData.put("message", message);
            chatData.put("roomId", roomId);

            gameClient.sendMessage(new NetworkMessage(
                NetworkMessage.Type.CHAT,
                joueur,
                chatData
            ));
            System.out.println("📤 Chat envoyé: " + message);
        }
    }

    /* =======================
       VALIDATION DES RÉPONSES
       ======================= */

    @FXML
    private void handleValider() {
        if (hasValidated) return; // Already validated

        hasValidated = true;
        validatedPlayers.add(joueur);

        // Collect current answers
        Map<String, String> myAnswers = new HashMap<>();
        for (Categorie categorie : categories) {
            TextField tf = textFieldsParCategorie.get(categorie.getNom());
            if (tf != null) {
                myAnswers.put(categorie.getNom(), tf.getText().trim());
            }
        }
        allPlayerAnswers.put(joueur, myAnswers);

        // Disable my input fields
        disableMyInputs();

        // Update button appearance
        if (btnValider != null) {
            btnValider.setText("✓ VALIDÉ");
            btnValider.setDisable(true);
            btnValider.setStyle("-fx-background-color: #27ae60; -fx-font-size: 14px; -fx-padding: 10 30;");
        }

        // Mark my row as validated
        markPlayerAsValidated(joueur);

        // Update status
        updateValidationStatus();

        // Send validation to server
        if (gameClient != null) {
            Map<String, Object> validationData = new HashMap<>();
            validationData.put("player", joueur);
            validationData.put("answers", myAnswers);
            validationData.put("roomId", roomId);

            gameClient.sendMessage(new NetworkMessage(
                NetworkMessage.Type.VALIDATE_ANSWERS,
                joueur,
                validationData
            ));
            System.out.println("✅ Réponses validées et envoyées");
        }
    }

    private void disableMyInputs() {
        for (TextField tf : textFieldsParCategorie.values()) {
            tf.setEditable(false);
            tf.setStyle("-fx-font-size: 11; -fx-background-color: #444; -fx-text-fill: #aaa;");
        }
    }

    private void updateValidationStatus() {
        if (lblValidationStatus != null) {
            int validated = validatedPlayers.size();
            int total = playerList.size();
            lblValidationStatus.setText(validated + "/" + total + " joueurs ont validé");

            if (validated == total) {
                lblValidationStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
            }
        }
    }

    private void markPlayerAsValidated(String playerName) {
        HBox playerRow = playerRowMap.get(playerName);
        if (playerRow == null) return;

        // Update the player name label to show validated status
        if (!playerRow.getChildren().isEmpty()) {
            javafx.scene.Node firstNode = playerRow.getChildren().get(0);
            if (firstNode instanceof Label nameLabel) {
                String currentText = nameLabel.getText();
                if (!currentText.contains("✓")) {
                    nameLabel.setText("✓ " + currentText.replace("➤ ", ""));
                    nameLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
                }
            }
        }

        // Change row style to indicate validated
        playerRow.setStyle("-fx-background-color: rgba(39, 174, 96, 0.2); -fx-border-color: #27ae60; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    private void revealAllAnswers() {
        System.out.println("🎉 Tous les joueurs ont validé - révélation des réponses!");

        // Update status
        if (lblValidationStatus != null) {
            lblValidationStatus.setText("✓ Tous ont validé - Réponses révélées!");
            lblValidationStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-font-weight: bold;");
        }

        // Reveal answers for each player
        for (String playerName : playerList) {
            Map<String, String> answers = allPlayerAnswers.get(playerName);
            if (answers != null && !playerName.equals(joueur)) {
                // Update this player's row with their answers
                HBox playerRow = playerRowMap.get(playerName);
                if (playerRow != null) {
                    int idx = 1; // Skip player name label
                    for (Categorie cat : categories) {
                        if (idx < playerRow.getChildren().size()) {
                            javafx.scene.Node node = playerRow.getChildren().get(idx);
                            if (node instanceof Label label) {
                                String answer = answers.getOrDefault(cat.getNom(), "-");
                                label.setText(answer.isEmpty() ? "-" : answer);
                                label.setStyle("-fx-font-size: 11; -fx-text-fill: white;");
                            }
                        }
                        idx++;
                    }
                }
            }
        }

        // Add system message to chat
        addChatMessage("SYSTÈME", "Toutes les réponses ont été révélées!", false);
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