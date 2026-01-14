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
import org.example.triharf.services.GroqValidator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import org.example.triharf.services.GroqValidator.MultiplayerValidationResponse;

public class JeuMultiController {

    // ===== UI COMPONENTS (MATCHING FXML) =====
    @FXML
    private Button btnBack;
    @FXML
    private Label lblTimer;
    @FXML
    private Label lblJoueurs;
    @FXML
    private Label lblLettre;
    @FXML
    private Label lblCurrentRound;
    @FXML
    private Label lblTotalRounds;
    @FXML
    private VBox vboxPlayers;
    @FXML
    private HBox hboxCategoryHeaders;
    @FXML
    private VBox vboxPlayerRows;
    @FXML
    private VBox vboxMessages;
    @FXML
    private TextField tfMessage;
    @FXML
    private Button btnSend;
    @FXML
    private Button btnValider;
    @FXML
    private Button btnVoirResultats;
    @FXML
    private Label lblValidationStatus;

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
    private static final double SCORE_WIDTH = 60.0;

    // Store score labels for each player to update later
    private Map<String, Label> playerScoreLabels = new HashMap<>();
    private Map<String, Map<String, Label>> playerPointsLabels = new HashMap<>(); // player -> category -> points label

    // ===== VALIDATION STATE =====
    private boolean hasValidated = false;
    private Set<String> validatedPlayers = new HashSet<>();
    private boolean allPlayersValidated = false;
    private Map<String, Integer> playerFinalScores = new HashMap<>(); // Store final scores for results page
    private Map<String, Integer> cumulativeScores = new HashMap<>(); // Track scores across all rounds
    private Set<Character> usedLetters = new HashSet<>(); // Track used letters to avoid repeats

    // ===== INJECTED DATA =====
    private List<String> categoriesNoms;
    private List<Categorie> categories;
    private String joueur = "Joueur_Multi";
    private int gameDuration = 180;
    private final Gson gson = new Gson();
    private int currentRound = 1;
    private int totalRounds = 3;
    private org.example.triharf.enums.Langue langue = org.example.triharf.enums.Langue.FRANCAIS;

    // ===== DAO =====
    private CategorieDAO categorieDAO = new CategorieDAO();

    // ===== GROQ VALIDATOR =====
    private GroqValidator groqValidator = new GroqValidator();
    private ExecutorService validationExecutor = Executors.newFixedThreadPool(4);

    // ===== NETWORK =====
    private GameClient gameClient;
    private String roomId;
    private boolean isHost = false;
    private String gameMode = "MULTI";
    private Set<String> eliminatedPlayers = new HashSet<>();

    public void setGameMode(String mode) {
        this.gameMode = mode;
        System.out.println("Mode de jeu défini dans JeuMultiController: " + mode);
        updateUIForMode();
    }

    private void updateUIForMode() {
        if ("BATAILLE_ROYALE".equals(gameMode)) {
            // Apply Red Theme for Battle Royale
            if (hboxCategoryHeaders != null) {
                hboxCategoryHeaders.setStyle("-fx-border-color: #c0392b; -fx-border-width: 0 0 2 0;");
            }
            // Add other theme adjustments here
        }
    }

    public void setIsHost(boolean isHost) {
        this.isHost = isHost;
    }

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

                    // Get round configuration
                    Object durationObj = data.get("duration");
                    Object roundsObj = data.get("totalRounds");
                    if (durationObj instanceof Number && roundsObj instanceof Number) {
                        int duration = ((Number) durationObj).intValue();
                        int rounds = ((Number) roundsObj).intValue();
                        setRoundConfig(rounds, duration);
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
                case NEXT_ROUND -> {
                    Object data = message.getData();
                    Character forcedLetter = null;
                    if (data instanceof Map) {
                        Map<String, Object> nextRoundData = (Map<String, Object>) data;
                        String letterStr = (String) nextRoundData.get("letter");
                        if (letterStr != null && !letterStr.isEmpty()) {
                            forcedLetter = letterStr.charAt(0);
                        }
                    }
                    startNextRound(forcedLetter);
                }
                case SHOW_RESULTS -> {
                    navigateToMultiplayerResults();
                }
                case PLAYER_ELIMINATED -> {
                    String eliminatedPlayer = (String) message.getData();
                    handlePlayerEliminated(eliminatedPlayer);
                }
                case VALIDATION_RESULTS -> {
                    String json = (String) message.getData();
                    handleValidationResults(json);
                }
                case GAME_ENDED_HOST_LEFT -> {
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Fin de partie");
                        alert.setHeaderText("L'hôte a quitté la partie");
                        alert.setContentText("La partie est annulée. Retour au menu principal.");
                        alert.showAndWait();
                        handleBack();
                    });
                }
                case PLAYER_LEFT -> {
                    String leftPlayer = (String) message.getData();
                    System.out.println("👋 Joueur parti : " + leftPlayer);
                    playerList.remove(leftPlayer);
                    // Refresh UI
                    creerChampsDynamiquement();
                    if (lblPlayerCount != null)
                        lblPlayerCount.setText(String.valueOf(playerList.size()));
                }
                default -> {
                }
            }
        });
    }

    private void handleNextRoundAction() {
        if (isHost) {
            // Battle Royale Logic: Eliminate lowest score before next round
            if ("BATAILLE_ROYALE".equals(gameMode)) {
                String playerToEliminate = findLowestScoringPlayer();
                if (playerToEliminate != null) {
                    System.out.println("💀 Élimination de: " + playerToEliminate);
                    if (gameClient != null) {
                        gameClient.sendMessage(
                                new NetworkMessage(NetworkMessage.Type.PLAYER_ELIMINATED, joueur, playerToEliminate));
                    }
                    // Wait a moment for elimination message to propagate before sending next round?
                    // For now, send immediately, clients handle ordering usually fine or closely.
                }
            }

            if (gameClient != null) {
                Character nextLetter = generateNewLetter();
                Map<String, Object> nextRoundData = new HashMap<>();
                nextRoundData.put("roomId", roomId);
                nextRoundData.put("letter", nextLetter.toString());

                gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.NEXT_ROUND, joueur, nextRoundData));
            }
        } else {
            showAlert("Attente de l'hôte", "Seul l'hôte peut lancer la manche suivante.");
        }
    }

    private String findLowestScoringPlayer() {
        String lowestPlayer = null;
        int minScore = Integer.MAX_VALUE;
        int activeCount = 0;

        // Count active players first
        for (String p : playerList) {
            if (!eliminatedPlayers.contains(p))
                activeCount++;
        }

        // Don't eliminate if only 1 (or 0) players left (Winner)
        if (activeCount <= 1)
            return null;

        // Ensure we have scores for all active players
        for (String p : playerList) {
            if (eliminatedPlayers.contains(p))
                continue;

            int score = cumulativeScores.getOrDefault(p, 0);
            if (score < minScore) {
                minScore = score;
                lowestPlayer = p;
            } else if (score == minScore) {
                // Tie-breaker? For now, random or first found.
                // Maybe keep the one who joined first? Or random?
                // Current logic: First found keeps lowest.
            }
        }
        return lowestPlayer;
    }

    private void handlePlayerEliminated(String pName) {
        eliminatedPlayers.add(pName);
        System.out.println("💀 JOUEUR ÉLIMINÉ: " + pName);

        // Show notification
        addChatMessage("SYSTÈME", "💀 " + pName + " a été éliminé !", false);

        // Update UI
        HBox row = playerRowMap.get(pName);
        if (row != null) {
            row.setDisable(true);
            row.setStyle("-fx-background-color: rgba(231, 76, 60, 0.3); -fx-opacity: 0.6;");
            // Add skull icon or label change?
        }

        // Check if I am eliminated
        if (pName.equals(joueur)) {
            showAlert("ÉLIMINÉ", "Vous avez été éliminé du Battle Royale !");
            disableMyInputs();
            if (btnValider != null)
                btnValider.setDisable(true);
        }
    }

    private void handleShowResultsAction() {
        if (isHost) {
            if (gameClient != null) {
                gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.SHOW_RESULTS, joueur, roomId));
            }
        } else {
            showAlert("Attente de l'hôte", "Seul l'hôte peut afficher les résultats.");
        }
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
        if (vboxMessages == null)
            return;

        Label msgLabel = new Label(sender + ": " + message);
        String style = isLocal ? "-fx-text-fill: #9b59b6; -fx-font-size: 12px; -fx-font-weight: bold;"
                : "-fx-text-fill: white; -fx-font-size: 12px;";
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
        if (playerRow == null || playerName.equals(joueur))
            return; // Don't update own row

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

    /*
     * =======================
     * INJECTION METHODS
     * =======================
     */

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

    public void setRoundConfig(int totalRounds, int roundDuration) {
        this.totalRounds = totalRounds;
        this.gameDuration = roundDuration;
        this.currentRound = 1;
        System.out.println("✅ Configuration manches: " + totalRounds + " manches, " + roundDuration + "s chacune");
        updateRoundDisplay();
    }

    public void setCurrentRound(int round) {
        this.currentRound = round;
        updateRoundDisplay();
    }

    private void updateRoundDisplay() {
        if (lblCurrentRound != null) {
            lblCurrentRound.setText(String.valueOf(currentRound));
        }
        if (lblTotalRounds != null) {
            lblTotalRounds.setText(String.valueOf(totalRounds));
        }
    }

    /*
     * =======================
     * INITIALIZATION
     * =======================
     */

    @FXML
    public void initialize() {
        System.out.println("✅ JeuMultiController initialisé");

        this.gameEngine = new GameEngine();
        this.validationService = new ValidationService();
        this.resultsManager = new ResultsManager(gameDuration);
    }

    /*
     * =======================
     * DÉMARRAGE DE LA PARTIE
     * =======================
     */

    public void demarrerPartie() {
        if (categories == null || categories.isEmpty()) {
            System.err.println("❌ ERREUR : Aucune catégorie reçue !");
            System.err.println("   categoriesNoms: " + (categoriesNoms != null ? categoriesNoms.size() : "null"));
            showAlert("Erreur", "Aucune catégorie sélectionnée !");
            return;
        }

        // Set player name with fallback
        String pseudoGlobal = ParametresGenerauxController.pseudoGlobal;
        this.joueur = (pseudoGlobal != null && !pseudoGlobal.isEmpty()) ? pseudoGlobal
                : "Joueur_" + System.currentTimeMillis() % 1000;
        this.langue = ParametresGenerauxController.langueGlobale != null ? ParametresGenerauxController.langueGlobale
                : org.example.triharf.enums.Langue.FRANCAIS;

        System.out.println("✅ Démarrage partie multijoueur");
        System.out.println("   Joueur: " + joueur);
        System.out.println("   Langue: " + langue);
        System.out.println("   Catégories: " + categories.size());
        System.out.println("   Manches: " + currentRound + "/" + totalRounds);

        try {
            // Track the first letter as used
            if (lettreActuelle != null) {
                usedLetters.add(lettreActuelle);
            }

            // Update round display
            updateRoundDisplay();

            // Create category input fields in player list area
            creerChampsDynamiquement();

            // Start timer
            gameEngine.setOnTimerUpdate(this::afficherTimer);
            gameEngine.setOnGameEnd(this::handleTerminerAuto);
            gameEngine.startTimer(gameDuration);

            System.out.println("✅ Partie multijoueur démarrée - Manche " + currentRound);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Start the next round after current round is completed
     */
    private void startNextRound(Character forcedLetter) {
        System.out.println("🔄 Démarrage de la manche " + (currentRound + 1) + "/" + totalRounds);

        // Increment round counter
        currentRound++;
        updateRoundDisplay();

        // Use forced letter if provided, otherwise generate (fallback)
        if (forcedLetter != null) {
            lettreActuelle = forcedLetter;
        } else {
            lettreActuelle = generateNewLetter();
        }

        usedLetters.add(lettreActuelle);
        afficherLettre();

        // Reset validation state for new round
        hasValidated = false;
        validatedPlayers.clear();
        allPlayersValidated = false;
        allPlayerAnswers.clear();
        reponses.clear();

        // Hide results button
        if (btnVoirResultats != null) {
            btnVoirResultats.setVisible(false);
            btnVoirResultats.setManaged(false);
            // Reset the action to show results (in case it was changed to startNextRound)
            btnVoirResultats.setOnAction(e -> handleVoirResultats());
        }

        // Re-enable validate button only if NOT eliminated
        if (btnValider != null) {
            if (eliminatedPlayers.contains(joueur)) {
                btnValider.setText("💀 ÉLIMINÉ");
                btnValider.setDisable(true);
                btnValider.setStyle("-fx-background-color: #c0392b; -fx-font-size: 13px; -fx-padding: 8 25;");
            } else {
                btnValider.setText("✓ VALIDER MES RÉPONSES");
                btnValider.setDisable(false);
                btnValider.setStyle("-fx-font-size: 13px; -fx-padding: 8 25;");
            }
        }

        // Clear status
        if (lblValidationStatus != null) {
            lblValidationStatus.setText("");
        }

        // Rebuild the player rows to clear answers
        creerChampsDynamiquement();

        // Start new timer
        gameEngine.setOnTimerUpdate(this::afficherTimer);
        gameEngine.setOnGameEnd(this::handleTerminerAuto);
        gameEngine.startTimer(gameDuration);

        addChatMessage("SYSTÈME", "🔄 Manche " + currentRound + " commencée! Nouvelle lettre: " + lettreActuelle,
                false);
        System.out.println("✅ Manche " + currentRound + " démarrée avec lettre " + lettreActuelle);
    }

    /**
     * Generate a new random letter that hasn't been used yet
     */
    private Character generateNewLetter() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        List<Character> availableLetters = new ArrayList<>();

        for (char c : alphabet.toCharArray()) {
            if (!usedLetters.contains(c)) {
                availableLetters.add(c);
            }
        }

        if (availableLetters.isEmpty()) {
            // All letters used, reset and pick random
            usedLetters.clear();
            return alphabet.charAt(new Random().nextInt(alphabet.length()));
        }

        return availableLetters.get(new Random().nextInt(availableLetters.size()));
    }

    /*
     * =======================
     * UI DYNAMIQUE
     * =======================
     */

    private void creerChampsDynamiquement() {
        if (vboxPlayerRows == null && vboxPlayers == null)
            return;

        textFieldsParCategorie.clear();
        reponses.clear();
        playerRowMap.clear();
        playerScoreLabels.clear();
        playerPointsLabels.clear();

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

            // Score column header
            Label scoreHeader = new Label("Score");
            scoreHeader.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #f39c12;");
            scoreHeader.setMinWidth(SCORE_WIDTH);
            scoreHeader.setPrefWidth(SCORE_WIDTH);
            scoreHeader.setAlignment(javafx.geometry.Pos.CENTER);
            hboxCategoryHeaders.getChildren().add(scoreHeader);
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
        String bgColor = isCurrentPlayer
                ? "-fx-background-color: rgba(155, 89, 182, 0.2); -fx-border-color: #9b59b6; -fx-border-radius: 5; -fx-background-radius: 5;"
                : "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5;";

        // Apply eliminated style if applicable
        if (eliminatedPlayers.contains(playerName)) {
            bgColor = "-fx-background-color: rgba(231, 76, 60, 0.3); -fx-border-color: #c0392b; -fx-border-radius: 5; -fx-opacity: 0.6;";
            row.setDisable(true);
        }

        row.setStyle(bgColor);

        // Player name label
        Label nameLabel = new Label(isCurrentPlayer ? "➤ " + playerName : playerName);
        nameLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: "
                + (isCurrentPlayer ? "#9b59b6" : "white") + ";");
        nameLabel.setMinWidth(PLAYER_NAME_WIDTH);
        nameLabel.setPrefWidth(PLAYER_NAME_WIDTH);
        row.getChildren().add(nameLabel);

        // Initialize points labels map for this player
        playerPointsLabels.putIfAbsent(playerName, new HashMap<>());

        // Create input fields or display labels for each category
        for (Categorie categorie : categories) {
            // Container for answer + points (split 2/3 + 1/3)
            HBox cellContainer = new HBox(2);
            cellContainer.setMinWidth(CATEGORY_WIDTH);
            cellContainer.setPrefWidth(CATEGORY_WIDTH);
            cellContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            if (isCurrentPlayer) {
                // Current player gets editable text fields (takes ~80% width)
                TextField textField = new TextField();
                textField.setPromptText(
                        categorie.getNom().substring(0, Math.min(3, categorie.getNom().length())) + "...");
                textField.setPrefWidth(CATEGORY_WIDTH * 0.75);
                textField.setMinWidth(CATEGORY_WIDTH * 0.75);
                textField.setStyle("-fx-font-size: 10;");

                textFieldsParCategorie.put(categorie.getNom(), textField);
                reponses.put(categorie, "");

                // Points label (hidden until validation)
                Label pointsLabel = new Label("");
                pointsLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #f39c12; -fx-font-weight: bold;");
                pointsLabel.setMinWidth(CATEGORY_WIDTH * 0.25);
                pointsLabel.setPrefWidth(CATEGORY_WIDTH * 0.25);
                pointsLabel.setAlignment(javafx.geometry.Pos.CENTER);
                playerPointsLabels.get(playerName).put(categorie.getNom(), pointsLabel);

                cellContainer.getChildren().addAll(textField, pointsLabel);
            } else {
                // Other players get read-only labels
                Label answerLabel = new Label("...");
                answerLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #aaa;");
                answerLabel.setMinWidth(CATEGORY_WIDTH * 0.75);
                answerLabel.setPrefWidth(CATEGORY_WIDTH * 0.75);
                answerLabel.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                // Points label (hidden until all validate)
                Label pointsLabel = new Label("");
                pointsLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #f39c12; -fx-font-weight: bold;");
                pointsLabel.setMinWidth(CATEGORY_WIDTH * 0.25);
                pointsLabel.setPrefWidth(CATEGORY_WIDTH * 0.25);
                pointsLabel.setAlignment(javafx.geometry.Pos.CENTER);
                playerPointsLabels.get(playerName).put(categorie.getNom(), pointsLabel);

                cellContainer.getChildren().addAll(answerLabel, pointsLabel);
            }

            row.getChildren().add(cellContainer);
        }

        // Score cell at the end
        int currentScore = cumulativeScores.getOrDefault(playerName, 0);
        Label scoreLabel = new Label(String.valueOf(currentScore));
        scoreLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #f39c12;");
        scoreLabel.setMinWidth(SCORE_WIDTH);
        scoreLabel.setPrefWidth(SCORE_WIDTH);
        scoreLabel.setAlignment(javafx.geometry.Pos.CENTER);
        playerScoreLabels.put(playerName, scoreLabel);
        row.getChildren().add(scoreLabel);

        return row;
    }

    /*
     * =======================
     * AFFICHAGE
     * =======================
     */

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

    /*
     * =======================
     * CHAT
     * =======================
     */

    @FXML
    private void handleSendMessage() {
        if (tfMessage == null || vboxMessages == null)
            return;

        String message = tfMessage.getText().trim();
        if (message.isEmpty())
            return;

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
                    chatData));
            System.out.println("📤 Chat envoyé: " + message);
        }
    }

    /*
     * =======================
     * VALIDATION DES RÉPONSES
     * =======================
     */

    @FXML
    private void handleValider() {
        if (hasValidated)
            return; // Already validated

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
                    validationData));
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
        if (playerRow == null)
            return;

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
        playerRow.setStyle(
                "-fx-background-color: rgba(39, 174, 96, 0.2); -fx-border-color: #27ae60; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    private void revealAllAnswers() {
        System.out.println("🎉 Tous les joueurs ont validé - révélation des réponses!");

        // Stop the timer
        if (gameEngine != null) {
            gameEngine.stopTimer();
        }
        if (lblTimer != null) {
            lblTimer.setText("TERMINÉ");
            lblTimer.setStyle("-fx-text-fill: #27ae60;");
        }

        // Clear previous scores
        playerFinalScores.clear();

        // Reveal answers first
        revealAnswersDisplay();

        // CENTRALIZED VALIDATION LOGIC
        if (isHost) {
            // Host performs validation and broadcasts results
            if (lblValidationStatus != null) {
                lblValidationStatus.setText("✓ Validation CENTRALISÉE en cours (Hôte)...");
                lblValidationStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
            validateWithGroqAsync();
        } else {
            // Clients wait for host
            if (lblValidationStatus != null) {
                lblValidationStatus.setText("✓ En attente de la validation par l'Hôte...");
                lblValidationStatus.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
        }
    }

    private void revealAnswersDisplay() {
        // Display all answers first (before validation)
        for (String playerName : playerList) {
            Map<String, String> answers = allPlayerAnswers.get(playerName);
            if (answers == null)
                continue;

            HBox playerRow = playerRowMap.get(playerName);
            boolean isCurrentPlayer = playerName.equals(joueur);

            int cellIndex = 1; // Start after player name label

            for (Categorie cat : categories) {
                String answer = answers.getOrDefault(cat.getNom(), "");

                // Update answer display for other players (not current player)
                if (!isCurrentPlayer && playerRow != null && cellIndex < playerRow.getChildren().size() - 1) {
                    javafx.scene.Node node = playerRow.getChildren().get(cellIndex);
                    if (node instanceof HBox cellContainer && !cellContainer.getChildren().isEmpty()) {
                        javafx.scene.Node firstChild = cellContainer.getChildren().get(0);
                        if (firstChild instanceof Label answerLabel) {
                            answerLabel.setText(answer.isEmpty() ? "-" : answer);
                            answerLabel.setStyle("-fx-font-size: 10; -fx-text-fill: white;");
                        }
                    }
                }

                cellIndex++;
            }
        }
    }

    private void validateWithGroqAsync() {
        if (lblValidationStatus != null) {
            javafx.application.Platform
                    .runLater(() -> lblValidationStatus.setText("✓ Validation par lot en cours (IA)..."));
        }

        // Use a single async task for the batch request
        CompletableFuture.runAsync(() -> {
            try {
                // Perform SINGLE batch request
                System.out.println("🤖 Envoi de la requête de validation groupée (Batch)...");
                Map<String, Map<String, MultiplayerValidationResponse>> results = new GroqValidator()
                        .validateBatch(allPlayerAnswers, lettreActuelle, langue);

                // Apply locally - REMOVED to avoid double counting (Host receives broadcast
                // too)
                // javafx.application.Platform.runLater(() -> applyValidationResults(results));

                // Broadcast
                if (isHost && gameClient != null) {
                    String json = gson.toJson(results);
                    gameClient.sendMessage(new NetworkMessage(
                            NetworkMessage.Type.VALIDATION_RESULTS,
                            joueur,
                            json));
                    System.out.println("📢 Résultats BATCH diffusés aux clients");
                }
            } catch (Exception e) {
                System.err.println("Erreur validation batch: " + e.getMessage());
                e.printStackTrace();
            }
        }, validationExecutor);
    }

    private void handleValidationResults(String jsonResults) {
        try {
            Type type = new TypeToken<Map<String, Map<String, MultiplayerValidationResponse>>>() {
            }.getType();
            Map<String, Map<String, MultiplayerValidationResponse>> results = gson.fromJson(jsonResults, type);
            System.out.println("📥 Résultats de validation reçus de l'hôte");
            applyValidationResults(results);
        } catch (Exception e) {
            System.err.println("Erreur décodage résultats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyValidationResults(
            Map<String, Map<String, GroqValidator.MultiplayerValidationResponse>> validationResults) {
        System.out.println("📊 Applying validation results...");

        for (String playerName : playerList) {
            Map<String, GroqValidator.MultiplayerValidationResponse> playerResults = validationResults.get(playerName);
            if (playerResults == null)
                continue;

            int totalScore = 0;

            for (Categorie cat : categories) {
                GroqValidator.MultiplayerValidationResponse response = playerResults.get(cat.getNom());

                int points = 0;
                if (response != null) {
                    points = response.getScore();
                }

                totalScore += points;

                // Update the points label for this category
                Map<String, Label> playerPoints = playerPointsLabels.get(playerName);
                if (playerPoints != null) {
                    Label pointsLabel = playerPoints.get(cat.getNom());
                    if (pointsLabel != null) {
                        if (points > 0) {
                            pointsLabel.setText("+" + points);
                            pointsLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        } else {
                            pointsLabel.setText("0");
                            pointsLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        }
                    }
                }
            }

            // Add round score to cumulative total
            int previousCumulative = cumulativeScores.getOrDefault(playerName, 0);
            cumulativeScores.put(playerName, previousCumulative + totalScore);

            // Store current round score for display
            playerFinalScores.put(playerName, totalScore);

            // Update total score label (show cumulative)
            Label scoreLabel = playerScoreLabels.get(playerName);
            if (scoreLabel != null) {
                int cumulativeTotal = cumulativeScores.get(playerName);
                scoreLabel.setText(String.valueOf(cumulativeTotal));
                scoreLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #f39c12;");
            }

            System.out.println("📊 Score manche " + currentRound + " de " + playerName + ": " + totalScore +
                    " (Total: " + cumulativeScores.get(playerName) + ")");
        }

        // Check if this was the last round
        if (currentRound >= totalRounds)

        {
            // Final round - show results
            if (lblValidationStatus != null) {
                lblValidationStatus.setText("✓ Partie terminée! Cliquez pour voir les résultats");
                lblValidationStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-font-weight: bold;");
            }

            // Show the results button
            if (btnVoirResultats != null) {
                btnVoirResultats.setText("🏆 VOIR LES RÉSULTATS FINAUX");
                btnVoirResultats.setVisible(true);
                btnVoirResultats.setManaged(true);
                btnVoirResultats.setOnAction(e -> handleShowResultsAction());
            }

            // Copy cumulative scores to final scores for results page
            playerFinalScores.clear();
            playerFinalScores.putAll(cumulativeScores);

            // Add system message to chat
            addChatMessage("SYSTÈME",
                    "Partie terminée après " + totalRounds + " manches! Cliquez pour voir le classement final.", false);
        } else {
            // More rounds to play - show next round button
            if (lblValidationStatus != null) {
                lblValidationStatus.setText("✓ Manche " + currentRound + "/" + totalRounds
                        + " terminée! Préparez-vous pour la suivante...");
                lblValidationStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 12px; -fx-font-weight: bold;");
            }

            // Change button to start next round
            if (btnVoirResultats != null) {
                btnVoirResultats.setText("▶ MANCHE SUIVANTE");
                btnVoirResultats.setOnAction(e -> handleNextRoundAction());
                btnVoirResultats.setVisible(true);
                btnVoirResultats.setManaged(true);
            }
        }

        addChatMessage("SYSTÈME",
                "Manche " + currentRound + " terminée! Prochaine manche dans quelques secondes...", false);
    }

    @FXML
    private void handleVoirResultats() {
        navigateToMultiplayerResults();
    }

    private void navigateToMultiplayerResults() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/fxml/resultats_multi.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ResultatsMultiController) {
                ResultatsMultiController rc = (ResultatsMultiController) controller;
                rc.displayRanking(playerFinalScores, joueur, lettreActuelle);
            }

            Stage stage = (Stage) btnVoirResultats.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Résultats - Classement");

        } catch (IOException e) {
            System.err.println("Erreur navigation vers résultats: " + e.getMessage());
            e.printStackTrace();
            // Fallback to menu if results page doesn't exist
            retourMenu();
        }
    }

    /*
     * =======================
     * FIN DE PARTIE
     * =======================
     */

    @FXML
    private void handleTerminer() {
        // This is usually the "Surrender" or "Finish early" debug button
        // In multiplayer, it should just validate what we have
        handleValider();
    }

    private void handleTerminerAuto() {
        System.out.println("⏰ Temps écoulé ! Validation automatique...");

        javafx.application.Platform.runLater(() -> {
            if (!hasValidated) {
                handleValider();
            }
        });
    }

    private void terminerPartie() {
        // Deprecated method - redirecting to handleValider
        handleValider();
    }

    /*
     * =======================
     * NAVIGATION
     * =======================
     */

    private void retourMenu() {
        if (timeline != null) {
            timeline.stop();
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/main_menu.fxml"));
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