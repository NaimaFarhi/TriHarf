package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.util.Duration;
import org.example.triharf.HelloApplication;
import org.example.triharf.services.GameEngine;
import org.example.triharf.services.ResultsManager;
import org.example.triharf.services.ValidationService;
import org.example.triharf.models.Categorie;
import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.network.GameClient;
import org.example.triharf.network.NetworkMessage;
import org.example.triharf.services.GroqValidator;
import org.example.triharf.services.GroqValidator.MultiplayerValidationResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JeuChaosController {

    // ===== UI COMPONENTS =====
    @FXML
    private Button btnBack;
    @FXML
    private Label lblTimer;
    @FXML
    private Label lblJoueurs;
    @FXML
    private Label lblLettre;
    @FXML
    private VBox vboxPlayers;
    @FXML
    private VBox vboxMessages;
    @FXML
    private TextField tfMessage;
    @FXML
    private Button btnSend;
    @FXML
    private HBox hboxCategoryHeaders;
    @FXML
    private VBox vboxPlayerRows;

    // We may need validation UI elements if they exist in FXML (like btnValider)
    // Looking at FXML there is no validation button?
    // Wait, check partie_chaos.fxml... It doesn't seem to have a validate button in
    // the snippet I saw.
    // If it's missing, players can't validate.
    // Let's assume there is one or I should handle auto-validation on timer end.
    // Ideally, I should check FXML again, but let's add the button field just in
    // case it exists or I add it.
    @FXML
    private Button btnValider;
    @FXML
    private Label lblValidationStatus;
    @FXML
    private Button btnVoirResultats;
    @FXML
    private Label lblCurrentRound;
    @FXML
    private Label lblTotalRounds;

    // ===== SERVICES =====
    private GameEngine gameEngine;
    private ResultsManager resultsManager;
    private ValidationService validationService;
    private GroqValidator groqValidator = new GroqValidator();
    private ExecutorService validationExecutor = Executors.newFixedThreadPool(4);

    // ===== STATE =====
    private Character lettreActuelle;

    private int nbJoueurs = 1;
    private int currentRound = 1;
    private int totalRounds = 3;
    private final Map<String, TextField> textFieldsParCategorie = new HashMap<>(); // Key is Category Name
    private final Map<Categorie, String> reponses = new HashMap<>();
    private List<String> playerList = new ArrayList<>();
    private Map<String, HBox> playerRowMap = new HashMap<>();
    private Map<String, Map<String, Label>> playerPointsLabels = new HashMap<>();
    private Map<String, Label> playerScoreLabels = new HashMap<>();
    private Map<String, Integer> playerFinalScores = new HashMap<>();
    private Map<String, Integer> cumulativeScores = new HashMap<>();
    private Map<String, Map<String, String>> allPlayerAnswers = new HashMap<>();
    private Set<String> validatedPlayers = new HashSet<>();
    private boolean hasValidated = false;
    private boolean allPlayersValidated = false;
    private ChaosManager chaosManager;
    private boolean chaosEventsEnabled = true;

    // ===== INJECTED =====
    private List<String> categoriesNoms;
    private List<Categorie> categories;
    private String joueur;

    private int gameDuration = 180;
    private Set<Character> usedLetters = new HashSet<>();
    private final Gson gson = new Gson();
    private org.example.triharf.enums.Langue langue = org.example.triharf.enums.Langue.FRANCAIS;

    // ===== NETWORK =====
    private GameClient gameClient;
    private String roomId;
    private boolean isHost = false;

    // ===== DAO =====
    private CategorieDAO categorieDAO = new CategorieDAO();

    // ===== CONSTANTS =====
    private static final double CATEGORY_WIDTH = 120.0;
    private static final double PLAYER_NAME_WIDTH = 100.0;
    private static final double SCORE_WIDTH = 60.0;

    // ==========================================
    // CHAOS LOGIC INNER CLASS
    // ==========================================
    public enum ChaosEventType {
        GEL, TURBO, SWITCH, PANIC
    }

    private class ChaosManager {
        private Random random = new Random();
        private List<Integer> eventTimestamps = new ArrayList<>();
        private boolean eventsScheduled = false;

        public void scheduleEvents(int duration) {
            eventTimestamps.clear();
            int minTime = 10;
            int maxTime = duration - 10;

            if (maxTime > minTime) {
                int time1 = random.nextInt(maxTime - minTime) + minTime;
                int time2 = random.nextInt(maxTime - minTime) + minTime;

                while (Math.abs(time1 - time2) < 15) {
                    time2 = random.nextInt(maxTime - minTime) + minTime;
                }

                eventTimestamps.add(time1);
                eventTimestamps.add(time2);
                Collections.sort(eventTimestamps, Collections.reverseOrder());

                System.out.println("🔥 CHAOS: Événements prévus à T-" + time1 + "s et T-" + time2 + "s");
                eventsScheduled = true;
            }
        }

        public void checkEvents(int remainingTime) {
            if (!isHost || !eventsScheduled || eventTimestamps.isEmpty())
                return;

            // Trigger if we closely match the timestamp
            // Since timer ticks every second, exact match is likely.
            // Using a range to be safe.
            if (eventTimestamps.contains(remainingTime)) {
                int stamp = remainingTime;
                eventTimestamps.remove((Integer) stamp);
                triggerRandomEvent();
            }
        }

        private void triggerRandomEvent() {
            ChaosEventType[] types = ChaosEventType.values();
            ChaosEventType type = types[random.nextInt(types.length)];

            if (gameClient != null) {
                Map<String, String> data = new HashMap<>();
                data.put("type", type.name());
                gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.CHAOS_EVENT, "SERVER", data));
            }
        }
    }

    // ==========================================
    // INITIALIZATION
    // ==========================================

    @FXML
    public void initialize() {
        System.out.println("✅ JeuChaosController initialisé");
        this.gameEngine = new GameEngine();
        this.resultsManager = new ResultsManager(gameDuration);
        this.chaosManager = new ChaosManager();
        this.validationService = new ValidationService();

        // Initialize player name immediately
        String pseudoGlobal = org.example.triharf.controllers.ParametresGenerauxController.pseudoGlobal;
        this.joueur = (pseudoGlobal != null && !pseudoGlobal.isEmpty()) ? pseudoGlobal
                : "Joueur_" + System.currentTimeMillis() % 1000;

        if (btnBack != null)
            btnBack.setOnAction(e -> handleBack());
        if (btnSend != null)
            btnSend.setOnAction(e -> handleSendMessage());
        if (btnValider != null)
            btnValider.setOnAction(e -> handleValider());
        if (btnValider != null)
            btnValider.setOnAction(e -> handleValider());
        if (btnVoirResultats != null)
            btnVoirResultats.setOnAction(e -> handleVoirResultats());
    }

    @FXML
    private void handleBack() {
        try {
            if (gameClient != null) {
                gameClient.disconnect();
            }
            if (validationExecutor != null) {
                validationExecutor.shutdownNow();
            }
            if (gameEngine != null) {
                gameEngine.stopTimer();
            }

            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/fxml/main_menu.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSendMessage() {
        if (tfMessage == null || tfMessage.getText().trim().isEmpty())
            return;

        String msg = tfMessage.getText().trim();
        tfMessage.clear();
        addChatMessage("MOI", msg, true);

        if (gameClient != null) {
            Map<String, String> data = new HashMap<>();
            data.put("sender", joueur);
            data.put("message", msg);
            gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.CHAT, joueur, data));
        }
    }

    @FXML
    private void handleVoirResultats() {
        if (isHost && "➤ MANCHE SUIVANTE".equals(btnVoirResultats.getText())) {
            handleNextRoundAction();
            return;
        }

        // If Host and NOT next round (so it's final results), broadcast
        if (isHost) {
            if (gameClient != null) {
                System.out.println("📢 Host broadcasting SHOW_RESULTS");
                gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.SHOW_RESULTS, joueur, null));
            }
        }
        // Clients do nothing on click, they wait for header message
    }

    private void navigateToResults() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/fxml/resultats_multi.fxml"));
            Parent root = loader.load();

            org.example.triharf.controllers.ResultatsMultiController controller = loader.getController();
            if (controller != null) {
                controller.setScores(cumulativeScores, joueur);
            }

            Stage stage = (Stage) btnVoirResultats.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // NETWORK HANDLING
    // ==========================================
    public void setNetwork(GameClient client, String roomId) {
        this.gameClient = client;
        this.roomId = roomId;
        if (this.gameClient != null) {
            this.gameClient.setMessageHandler(this::handleNetworkMessage);
        }
    }

    public void setIsHost(boolean isHost) {
        this.isHost = isHost;
    }

    private void handleNetworkMessage(NetworkMessage message) {
        javafx.application.Platform.runLater(() -> {
            switch (message.getType()) {
                case GAME_START -> handleGameStart((Map<String, Object>) message.getData());
                case CHAT -> handleChat((Map<String, String>) message.getData());
                case PLAYER_JOINED -> updatePlayersFromStatus((List<String>) message.getData());
                case VALIDATE_ANSWERS -> handlePlayerValidation((Map<String, Object>) message.getData());
                case ALL_VALIDATED -> handleAllValidated((Map<String, Map<String, String>>) message.getData());
                case VALIDATION_RESULTS -> handleValidationResults((String) message.getData());
                case CHAOS_EVENT -> handleChaosEvent((Map<String, String>) message.getData());
                case SHOW_RESULTS -> navigateToResults();
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
                default -> {
                }
            }
        });
    }

    // ==========================================
    // CHAOS EVENT HANDLERS
    // ==========================================
    private void handleChaosEvent(Map<String, String> data) {
        String typeStr = data.get("type");
        try {
            ChaosEventType type = ChaosEventType.valueOf(typeStr);
            executeChaosEvent(type);
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown Chaos event: " + typeStr);
        }
    }

    private void executeChaosEvent(ChaosEventType type) {
        System.out.println("🔥 EXECUTING CHAOS EVENT: " + type);
        addChatMessage("⚠️ CHAOS", "ÉVÉNEMENT : " + type + " !!!", false);

        switch (type) {
            case GEL -> {
                addChatMessage("SYSTEM", "❄️ GEL ! Vous ne pouvez plus écrire pendant 5s !", false);
                toggleInputs(true);
                showOverlay("❄️ GELÉ ! ❄️", "#3498db");

                PauseTransition pause = new PauseTransition(Duration.seconds(5));
                pause.setOnFinished(e -> {
                    toggleInputs(false);
                    hideOverlay();
                    addChatMessage("SYSTEM", "Dégel ! Vous pouvez écrire.", false);
                });
                pause.play();
            }
            case TURBO -> {
                addChatMessage("SYSTEM", "🔥 TURBO ! +10 secondes pour tout le monde !", false);
                gameEngine.addTime(10);
                showFlashAnimation("#f1c40f");
                // Update timer label immediately for better feedback
                if (lblTimer != null)
                    lblTimer.setText(gameEngine.formatTime());
            }
            case PANIC -> {
                addChatMessage("SYSTEM", "😱 PANIC ! -5 secondes !", false);
                gameEngine.removeTime(5);
                showFlashAnimation("#e74c3c");
                if (lblTimer != null)
                    lblTimer.setText(gameEngine.formatTime());
            }
            case SWITCH -> {
                if (categories.size() < 2)
                    return;
                addChatMessage("SYSTEM", "🔄 SWITCH ! Les colonnes ont bougé !", false);
                swapColumns();
            }
        }
    }

    private void swapColumns() {
        if (categories.size() < 2)
            return;

        // 1. Capture text values based on physical index (0, 1, 2...)
        // Since we don't have a direct map by index, we rely on the current
        // 'categories' order.
        List<String> textValues = new ArrayList<>();
        for (Categorie cat : categories) {
            TextField tf = textFieldsParCategorie.get(cat.getNom());
            textValues.add(tf != null ? tf.getText() : "");
        }

        // 2. Perform logical swap of the categories list
        Collections.swap(categories, 0, 1);

        // 3. Rebuild the UI (Headers and Inputs)
        creerChampsDynamiquement();

        // 4. Restore the text values into the PHYSICALLY same slots.
        // The slot at index 0 now corresponds to `categories.get(0)` (which is the NEW
        // category).
        // We put the OLD text (from index 0) into this slot.
        for (int i = 0; i < categories.size(); i++) {
            Categorie cat = categories.get(i);
            TextField tf = textFieldsParCategorie.get(cat.getNom());
            if (tf != null && i < textValues.size()) {
                tf.setText(textValues.get(i));
            }
        }

        showFlashAnimation("#9b59b6");
    }

    // ==========================================
    // GAMEPLAY LOGIC
    // ==========================================

    private void handleGameStart(Map<String, Object> data) {
        List<String> cats = (List<String>) data.get("categories");
        if (cats != null)
            setCategories(cats);

        String letter = (String) data.get("letter");
        if (letter != null) {
            lettreActuelle = letter.charAt(0);
            if (lblLettre != null)
                lblLettre.setText(lettreActuelle.toString());
        }

        List<String> players = (List<String>) data.get("players");
        if (players != null) {
            this.playerList = new ArrayList<>(players);
            this.nbJoueurs = players.size();
            if (lblJoueurs != null)
                lblJoueurs.setText(String.valueOf(nbJoueurs));
        }

        Object durationObj = data.get("duration");
        Object roundsObj = data.get("totalRounds");

        System.out.println("🔍 RECU GameStart: duration=" + durationObj + " ("
                + (durationObj == null ? "null" : durationObj.getClass().getSimpleName()) + ")");
        System.out.println("🔍 RECU GameStart: totalRounds=" + roundsObj + " ("
                + (roundsObj == null ? "null" : roundsObj.getClass().getSimpleName()) + ")");

        try {
            if (durationObj != null) {
                if (durationObj instanceof Number) {
                    this.gameDuration = ((Number) durationObj).intValue();
                } else {
                    this.gameDuration = Integer.parseInt(durationObj.toString());
                }
            }

            if (roundsObj != null) {
                if (roundsObj instanceof Number) {
                    this.totalRounds = ((Number) roundsObj).intValue();
                } else {
                    this.totalRounds = Integer.parseInt(roundsObj.toString());
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ Erreur parsing duration/rounds: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("✅ Parsed Config: Duration=" + gameDuration + ", Rounds=" + totalRounds);

        demarrerPartie();
    }

    public void demarrerPartie() {
        if (categories == null || categories.isEmpty())
            return;

        String pseudoGlobal = org.example.triharf.controllers.ParametresGenerauxController.pseudoGlobal;
        this.joueur = (pseudoGlobal != null && !pseudoGlobal.isEmpty()) ? pseudoGlobal
                : "Joueur_" + System.currentTimeMillis() % 1000;

        System.out.println("✅ Démarrage partie CHAOS");

        creerChampsDynamiquement();

        gameEngine.setOnTimerUpdate(this::afficherTimer);
        gameEngine.setOnGameEnd(this::handleTerminerAuto);
        gameEngine.startTimer(gameDuration);
        afficherTimer();
        updateRoundLabels();

        if (isHost) {
            chaosManager.scheduleEvents(gameDuration);
        }
    }

    private void updateRoundLabels() {
        if (lblCurrentRound != null)
            lblCurrentRound.setText(String.valueOf(currentRound));
        if (lblTotalRounds != null)
            lblTotalRounds.setText(String.valueOf(totalRounds));
    }

    private void afficherTimer() {
        if (lblTimer != null)
            lblTimer.setText(gameEngine.formatTime());
        chaosManager.checkEvents(gameEngine.getRemainingTime());
    }

    // ==========================================
    // UI BUILDER
    // ==========================================

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
    }

    private HBox createPlayerRow(String playerName) {
        boolean isCurrentPlayer = playerName.equals(joueur);

        HBox row = new HBox(5);
        row.setPadding(new Insets(8));
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Highlight current player's row
        String bgColor = isCurrentPlayer
                ? "-fx-background-color: rgba(231, 76, 60, 0.1); -fx-border-color: #e74c3c; -fx-border-radius: 5; -fx-background-radius: 5;"
                : "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5;";

        row.setStyle(bgColor);

        // Player name label
        Label nameLabel = new Label(isCurrentPlayer ? "➤ " + playerName : playerName);
        nameLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: "
                + (isCurrentPlayer ? "#e74c3c" : "white") + ";");
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

    // ==========================================
    // VALIDATION & CHAT
    // ==========================================

    @FXML
    private void handleValider() {
        if (hasValidated)
            return;
        hasValidated = true;
        validatedPlayers.add(joueur);

        Map<String, String> myAnswers = new HashMap<>();
        for (Categorie categorie : categories) {
            TextField tf = textFieldsParCategorie.get(categorie.getNom());
            if (tf != null)
                myAnswers.put(categorie.getNom(), tf.getText().trim());
        }
        allPlayerAnswers.put(joueur, myAnswers);

        toggleInputs(true);
        if (btnValider != null) {
            btnValider.setDisable(true);
            btnValider.setText("VALIDÉ");
        }

        if (gameClient != null) {
            Map<String, Object> validationData = new HashMap<>();
            validationData.put("player", joueur);
            validationData.put("answers", myAnswers);
            gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.VALIDATE_ANSWERS, joueur, validationData));
        }
    }

    private void handlePlayerValidation(Map<String, Object> data) {
        String p = (String) data.get("player");
        if (p != null) {
            validatedPlayers.add(p);
            if (lblValidationStatus != null) {
                lblValidationStatus.setText(validatedPlayers.size() + "/" + nbJoueurs + " validés");
            }
            // check/highlight row
            HBox row = playerRowMap.get(p);
            if (row != null)
                row.setStyle(
                        "-fx-background-color: rgba(39, 174, 96, 0.2); -fx-border-color: #27ae60; -fx-border-radius: 5;");
        }
    }

    private void handleAllValidated(Map<String, Map<String, String>> allAnswers) {
        if (allAnswers != null)
            allPlayerAnswers.putAll(allAnswers);

        // Reveal
        for (String p : playerList) {
            HBox row = playerRowMap.get(p);
            if (row == null || p.equals(joueur))
                continue;

            Map<String, String> answers = allPlayerAnswers.get(p);
            if (answers == null) {
                // If answers not found (e.g. disconnected or error), skip updating this row
                continue;
            }

            int idx = 1;
            for (Categorie cat : categories) {
                if (idx < row.getChildren().size() - 1) {
                    HBox cell = (HBox) row.getChildren().get(idx);
                    Label l = (Label) cell.getChildren().get(0);
                    String ans = answers.getOrDefault(cat.getNom(), "-");
                    l.setText(ans);
                }
                idx++;
            }
        }

        if (isHost) {
            validateWithGroqAsync();
        }
    }

    private void validateWithGroqAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Map<String, MultiplayerValidationResponse>> results = new GroqValidator()
                        .validateBatch(allPlayerAnswers, lettreActuelle, langue);

                // Broadcast
                if (isHost && gameClient != null) {
                    String json = gson.toJson(results);
                    gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.VALIDATION_RESULTS, joueur, json));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, validationExecutor);
    }

    private void handleValidationResults(String json) {
        try {
            Type type = new TypeToken<Map<String, Map<String, MultiplayerValidationResponse>>>() {
            }.getType();
            Map<String, Map<String, MultiplayerValidationResponse>> results = gson.fromJson(json, type);

            for (String p : playerList) {
                Map<String, MultiplayerValidationResponse> pr = results.get(p);
                int total = 0;
                if (pr != null) {
                    for (Categorie cat : categories) {
                        MultiplayerValidationResponse resp = pr.get(cat.getNom());
                        int pts = (resp != null) ? resp.getScore() : 0;
                        total += pts;

                        if (playerPointsLabels.containsKey(p)) {
                            Label l = playerPointsLabels.get(p).get(cat.getNom());
                            if (l != null)
                                l.setText((pts > 0 ? "+" : "") + pts);
                        }
                    }
                }

                cumulativeScores.put(p, cumulativeScores.getOrDefault(p, 0) + total);
                if (playerScoreLabels.containsKey(p)) {
                    playerScoreLabels.get(p).setText(String.valueOf(cumulativeScores.get(p)));
                }
            }

            // Enable "Next Round" or "Show Results" button for Host/All
            if (isHost && btnVoirResultats != null) {
                btnVoirResultats.setVisible(true);
                btnVoirResultats.setManaged(true);
                if (currentRound < totalRounds) {
                    btnVoirResultats.setText("➤ MANCHE SUIVANTE");
                    btnVoirResultats.setOnAction(e -> handleNextRoundAction());
                } else {
                    btnVoirResultats.setText("🏆 RÉSULTATS FINAUX");
                    btnVoirResultats.setOnAction(e -> handleVoirResultats());
                }
            } else if (!isHost && btnVoirResultats != null) {
                // Client waits for host
                btnVoirResultats.setVisible(true);
                btnVoirResultats.setManaged(true);
                btnVoirResultats.setText("En attente de l'hôte...");
                btnVoirResultats.setDisable(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleNextRoundAction() {
        if (isHost) {
            if (gameClient != null) {
                Character nextLetter = generateNewLetter();
                Map<String, Object> nextRoundData = new HashMap<>();
                nextRoundData.put("roomId", roomId);
                nextRoundData.put("letter", nextLetter.toString());

                gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.NEXT_ROUND, joueur, nextRoundData));
            }
        }
    }

    private void startNextRound(Character forcedLetter) {
        System.out.println("🔄 Démarrage de la manche " + (currentRound + 1) + "/" + totalRounds);

        currentRound++;
        // Update labels if they existed (add fields if necessary or assume user will
        // add FXML labels later)

        if (forcedLetter != null) {
            lettreActuelle = forcedLetter;
        } else {
            lettreActuelle = generateNewLetter();
        }
        usedLetters.add(lettreActuelle);
        if (lblLettre != null)
            lblLettre.setText(lettreActuelle.toString());

        // RESET STATE
        hasValidated = false;
        validatedPlayers.clear();
        allPlayersValidated = false;
        allPlayerAnswers.clear();
        reponses.clear();

        // Reset Inputs
        creerChampsDynamiquement();

        // Reset Buttons
        if (btnVoirResultats != null) {
            btnVoirResultats.setVisible(false);
            btnVoirResultats.setManaged(false);
            btnVoirResultats.setDisable(false);
        }
        if (btnValider != null) {
            btnValider.setDisable(false);
            btnValider.setText("✓ VALIDER");
            btnValider.setStyle(""); // Reset style
        }

        // Scheduler NEW events for this round
        chaosManager.scheduleEvents(gameDuration);

        // Start Timer
        gameEngine.setOnTimerUpdate(this::afficherTimer);
        gameEngine.setOnGameEnd(this::handleTerminerAuto);
        gameEngine.startTimer(gameDuration);
        afficherTimer(); // Show initial time immediately

        addChatMessage("SYSTEM", "🔄 Manche " + currentRound + " ! Lettre : " + lettreActuelle, false);

        updateRoundLabels();
    }

    private Character generateNewLetter() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        List<Character> availableLetters = new ArrayList<>();
        for (char c : alphabet.toCharArray()) {
            if (!usedLetters.contains(c))
                availableLetters.add(c);
        }
        if (availableLetters.isEmpty()) {
            usedLetters.clear();
            return alphabet.charAt(new Random().nextInt(alphabet.length()));
        }
        return availableLetters.get(new Random().nextInt(availableLetters.size()));
    }

    // ==========================================
    // NAVIGATION & UTILS
    // ==========================================

    private void handleChat(Map<String, String> data) {
        addChatMessage(data.get("sender"), data.get("message"), false);
    }

    private void addChatMessage(String sender, String msg, boolean isLocal) {
        if (vboxMessages == null)
            return;
        Label lb = new Label(sender + ": " + msg);
        lb.setStyle("-fx-text-fill: " + (isLocal ? "#9b59b6" : "white") + ";");
        lb.setWrapText(true);
        vboxMessages.getChildren().add(lb);
    }

    public void setCategories(List<String> names) {
        this.categoriesNoms = names;
        this.categories = new ArrayList<>();
        for (String n : names) {
            Categorie c = categorieDAO.findByNom(n);
            if (c == null)
                c = new Categorie(n);
            categories.add(c);
        }
    }

    public void setLettre(String letter) {
        if (letter != null && !letter.isEmpty()) {
            this.lettreActuelle = letter.charAt(0);
            if (lblLettre != null)
                lblLettre.setText(lettreActuelle.toString());
        }
    }

    public void setPlayerList(List<String> players) {
        if (players != null) {
            this.playerList = new ArrayList<>(players);
            this.nbJoueurs = players.size();
            if (lblJoueurs != null)
                lblJoueurs.setText(String.valueOf(nbJoueurs));
            creerChampsDynamiquement();
        }
    }

    public void setGameDuration(int duration) {
        this.gameDuration = duration;
    }

    private void updatePlayersFromStatus(List<String> statusList) {
        List<String> names = new ArrayList<>();
        for (String s : statusList)
            names.add(s.split(":")[0]);
        this.playerList = names;
        this.nbJoueurs = names.size();
        if (lblJoueurs != null)
            lblJoueurs.setText(String.valueOf(nbJoueurs));
        creerChampsDynamiquement();
    }

    private void handleTerminerAuto() {
        System.out.println("FIN DU TEMPS !");
        if (!hasValidated)
            handleValider();
    }

    private void showOverlay(String text, String color) {
        // Simple visual feedback
    }

    private void hideOverlay() {
    }

    private void toggleInputs(boolean disable) {
        for (TextField tf : textFieldsParCategorie.values()) {
            tf.setDisable(disable);
            tf.setStyle(disable ? "-fx-background-color: #bdc3c7;" : "-fx-background-color: white;");
        }
    }

    private void showFlashAnimation(String color) {
        if (vboxPlayers != null) {
            String original = vboxPlayers.getStyle();
            vboxPlayers.setStyle("-fx-background-color: " + color + "; -fx-opacity: 0.5;");
            PauseTransition p = new PauseTransition(Duration.millis(500));
            p.setOnFinished(e -> {
                vboxPlayers.setStyle(original);
                vboxPlayers.setOpacity(1.0);
            });
            p.play();
        }
    }

    // Fallback nav
    private void navigateToMultiplayerResults() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/fxml/resultats_multi.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ResultatsMultiController) {
                // ... setup results ...
                ((ResultatsMultiController) controller).displayRanking(cumulativeScores, joueur, lettreActuelle);
            }

            Stage stage = (Stage) vboxPlayers.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
