package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;
import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.enums.Langue;
import org.example.triharf.models.Categorie;
import org.example.triharf.network.GameClient;
import org.example.triharf.network.GameServer;
import org.example.triharf.network.NetworkMessage;
import org.example.triharf.utils.NetworkUtils;

import java.io.IOException;
import java.util.*;

/**
 * Unified controller for all multiplayer modes: Multi, Battle Royale, Chaos
 */
public class ParamPartieMultiController {

    @FXML
    private Button btnRetour;
    @FXML
    private Label lblModeTitle;
    @FXML
    private Label lblModeDescription;
    @FXML
    private Spinner<Integer> spinnerMaxPlayers;
    @FXML
    private Spinner<Integer> spinnerNbRounds;
    @FXML
    private Spinner<Integer> spinnerRoundDuration;
    @FXML
    private TextField txtServerIP;
    @FXML
    private Button btnCopier;
    @FXML
    private VBox containerCategories;
    @FXML
    private Button btnCommencer;
    @FXML
    private Label lblCategoriesInfo;

    private List<String> categoriesSelectionnees = new ArrayList<>();
    private CategorieDAO categorieDAO = new CategorieDAO();
    private List<Categorie> toutesLesCategories = new ArrayList<>();
    private Map<String, CheckBox> checkboxMap = new HashMap<>();

    private GameClient gameClient;
    private GameServer gameServer;
    private String roomId;
    private String gameMode = "MULTI";

    public void setGameMode(String mode) {
        this.gameMode = mode;
        updateModeUI();
        System.out.println("Mode de jeu défini sur : " + mode);
    }

    @FXML
    public void initialize() {
        setupPlayerCountSpinner();
        setupRoundsSpinner();
        setupRoundDurationSpinner();
        chargerCategoriesParLangue();
        initialiserReseau();
    }

    /**
     * Setup spinner for max player count based on mode
     */
    private void setupPlayerCountSpinner() {
        int minPlayers = gameMode.equals("BATAILLE_ROYALE") ? 4 : 2;
        int maxPlayers = 10;
        int defaultPlayers = gameMode.equals("BATAILLE_ROYALE") ? 4 : 4;

        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(minPlayers,
                maxPlayers, defaultPlayers);

        if (spinnerMaxPlayers != null) {
            spinnerMaxPlayers.setValueFactory(valueFactory);
            spinnerMaxPlayers.setEditable(true);
        }
    }

    /**
     * Setup spinner for number of rounds (1-8)
     */
    private void setupRoundsSpinner() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 8, 3);

        if (spinnerNbRounds != null) {
            spinnerNbRounds.setValueFactory(valueFactory);
            spinnerNbRounds.setEditable(true);
        }
    }

    /**
     * Setup spinner for round duration (60-180 seconds)
     */
    private void setupRoundDurationSpinner() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(60, 180, 120,
                10);

        if (spinnerRoundDuration != null) {
            spinnerRoundDuration.setValueFactory(valueFactory);
            spinnerRoundDuration.setEditable(true);
        }
    }

    /**
     * Update UI based on selected game mode
     */
    private void updateModeUI() {
        if (lblModeTitle != null && lblModeDescription != null) {
            switch (gameMode) {
                case "MULTI" -> {
                    lblModeTitle.setText("🎮 Mode Multijoueur");
                    lblModeDescription.setText("2-10 joueurs • Catégories choisies");
                }
                case "BATAILLE_ROYALE" -> {
                    lblModeTitle.setText("⚔️ Mode Battle Royale");
                    lblModeDescription.setText("4-10 joueurs • Élimination progressive • Catégories choisies");
                }
                case "CHAOS" -> {
                    lblModeTitle.setText("🌀 Mode Chaos");
                    lblModeDescription.setText("2-10 joueurs • Catégories aléatoires • Événements surprise");
                }
            }
        }
    }

    /**
     * Load categories based on global language setting
     */
    private void chargerCategoriesParLangue() {
        Langue langueActuelle = ParametresGenerauxController.langueGlobale;

        if (gameMode.equals("CHAOS")) {
            // Chaos mode: categories are random, inform user
            if (lblCategoriesInfo != null) {
                lblCategoriesInfo.setText("⚠️ Les catégories seront choisies aléatoirement");
                lblCategoriesInfo.setVisible(true);
            }
            if (containerCategories != null) {
                containerCategories.setVisible(false);
            }
            // Load all categories for later random selection
            toutesLesCategories = categorieDAO.findActifByLangue(langueActuelle);
            return;
        }

        // Normal mode: load and display categories
        toutesLesCategories = categorieDAO.findActifByLangue(langueActuelle);

        System.out.println("📚 Catégories chargées pour " + langueActuelle + ": " + toutesLesCategories.size());

        if (toutesLesCategories.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention",
                    "Aucune catégorie disponible pour la langue sélectionnée.");
        }

        chargerCategoriesDynamiquement();
    }

    /**
     * Create ToggleButton chips dynamically from loaded categories
     */
    private void chargerCategoriesDynamiquement() {
        if (containerCategories == null) {
            System.err.println("containerCategories n'existe pas dans le FXML !");
            return;
        }

        containerCategories.getChildren().clear();
        checkboxMap.clear();

        javafx.scene.layout.FlowPane flowPane = new javafx.scene.layout.FlowPane();
        flowPane.setHgap(10);
        flowPane.setVgap(10);
        flowPane.setAlignment(javafx.geometry.Pos.CENTER);

        // Add "Select All" toggle button
        ToggleButton selectAllBtn = new ToggleButton("✨ Tout sélectionner");
        selectAllBtn.getStyleClass().addAll("category-chip", "category-select-all");
        selectAllBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (javafx.scene.Node node : flowPane.getChildren()) {
                if (node instanceof ToggleButton && node != selectAllBtn) {
                    ((ToggleButton) node).setSelected(newVal);
                }
            }
            mettreAJourCategories();
        });
        flowPane.getChildren().add(selectAllBtn);

        // Add category chips
        for (Categorie cat : toutesLesCategories) {
            ToggleButton chip = new ToggleButton(cat.getNom());
            chip.getStyleClass().add("category-chip");
            chip.selectedProperty().addListener((obs, oldVal, newVal) -> mettreAJourCategories());

            CheckBox fakeCheckbox = new CheckBox();
            fakeCheckbox.selectedProperty().bindBidirectional(chip.selectedProperty());
            checkboxMap.put(cat.getNom(), fakeCheckbox);

            flowPane.getChildren().add(chip);
        }

        containerCategories.getChildren().add(flowPane);
    }

    private void mettreAJourCategories() {
        categoriesSelectionnees.clear();
        for (Categorie cat : toutesLesCategories) {
            CheckBox checkbox = checkboxMap.get(cat.getNom());
            if (checkbox != null && checkbox.isSelected()) {
                categoriesSelectionnees.add(cat.getNom());
            }
        }
        System.out.println("Catégories sélectionnées : " + categoriesSelectionnees);
    }

    /**
     * Initialize network with mobile hotspot support
     */
    private void initialiserReseau() {
        new Thread(() -> {
            try {
                // Start server
                gameServer = new GameServer();
                Thread serverThread = new Thread(() -> {
                    try {
                        gameServer.start();
                    } catch (IOException e) {
                        System.err.println("Erreur démarrage serveur: " + e.getMessage());
                    }
                });
                serverThread.setDaemon(true);
                serverThread.start();

                Thread.sleep(500);

                // Get local IP (mobile hotspot)
                String localIP = NetworkUtils.getLocalIPAddress();
                String serverAddress = NetworkUtils.formatIPPort(localIP, 8888);

                // Connect client
                gameClient = new GameClient(localIP, 8888);
                int attempts = 5;
                boolean connected = false;
                while (attempts > 0 && !connected) {
                    try {
                        gameClient.connect();
                        connected = true;
                    } catch (IOException e) {
                        attempts--;
                        if (attempts > 0) {
                            Thread.sleep(500);
                        } else {
                            throw e;
                        }
                    }
                }

                // Create room
                roomId = UUID.randomUUID().toString().substring(0, 8);
                int maxPlayers = spinnerMaxPlayers != null ? spinnerMaxPlayers.getValue() : 4;
                gameServer.createRoom(roomId, maxPlayers, ParametresGenerauxController.langueGlobale);

                gameClient.sendMessage(new NetworkMessage(
                        NetworkMessage.Type.JOIN_ROOM,
                        ParametresGenerauxController.pseudoGlobal,
                        roomId));

                // Update UI
                javafx.application.Platform.runLater(() -> {
                    if (txtServerIP != null) {
                        txtServerIP.setText("Code: " + roomId);
                    }
                    System.out.println("✅ Réseau initialisé | IP: " + serverAddress + " | Room: " + roomId);
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Erreur Réseau",
                            "Impossible d'initialiser le réseau : " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleCommencer() {
        // Validate categories (except for Chaos mode)
        if (!gameMode.equals("CHAOS") && categoriesSelectionnees.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Sélectionnez au moins une catégorie !");
            return;
        }

        // For Chaos mode, select random categories
        if (gameMode.equals("CHAOS")) {
            categoriesSelectionnees = selectRandomCategories(8);
        }
        // UPDATE categories in room BEFORE navigating
        if (gameServer != null && roomId != null) {
            gameServer.getRoom(roomId).setCategories(categoriesSelectionnees);
            // Set round configuration
            int nbRounds = spinnerNbRounds != null ? spinnerNbRounds.getValue() : 3;
            int duration = spinnerRoundDuration != null ? spinnerRoundDuration.getValue() : 120;
            gameServer.getRoom(roomId).setRoundConfig(nbRounds, duration);

            // CRITICAL FIX: Update max players from spinner value
            if (spinnerMaxPlayers != null) {
                gameServer.getRoom(roomId).setMaxPlayers(spinnerMaxPlayers.getValue());
            }

            System.out.println("   Manches: " + nbRounds + " x " + duration + "s");
        }

        System.out.println("✅ Début partie " + gameMode);
        System.out.println("   Catégories : " + categoriesSelectionnees);
        System.out.println("   Max joueurs : " + spinnerMaxPlayers.getValue());

        navigateToWaitingRoom();
    }

    /**
     * Select random categories for Chaos mode
     */
    private List<String> selectRandomCategories(int count) {
        List<String> allCategories = new ArrayList<>();
        toutesLesCategories.forEach(cat -> allCategories.add(cat.getNom()));

        Collections.shuffle(allCategories);
        return allCategories.subList(0, Math.min(count, allCategories.size()));
    }

    private void navigateToWaitingRoom() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/salle_attente.fxml"));
            Parent root = loader.load();

            ListeAttenteController controller = loader.getController();
            if (controller != null) {
                controller.setGameMode(this.gameMode);
                controller.setNetwork(gameClient, gameServer, roomId);
                controller.setMaxPlayers(spinnerMaxPlayers.getValue());
                controller.setCategories(categoriesSelectionnees);
                // Pass round configuration
                int nbRounds = spinnerNbRounds != null ? spinnerNbRounds.getValue() : 3;
                int roundDuration = spinnerRoundDuration != null ? spinnerRoundDuration.getValue() : 120;
                controller.setRoundConfig(nbRounds, roundDuration);
            }

            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Salle d'attente - " + gameMode);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCopier() {
        if (txtServerIP != null) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(txtServerIP.getText());
            clipboard.setContent(content);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Informations copiées !");
        }
    }

    @FXML
    public void handleRetour() {
        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Erreur: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}