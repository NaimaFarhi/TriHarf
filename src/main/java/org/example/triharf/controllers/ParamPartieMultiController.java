package org.example.triharf.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;
import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.models.Categorie;
import org.example.triharf.services.NetworkService;
import org.example.triharf.utils.NgrokManager;

import java.io.IOException;
import java.util.*;

public class ParamPartieMultiController {

    @FXML private Button btnRetour;
    @FXML private TextField txtLien;
    @FXML private Button btnCopier;
    @FXML private VBox containerCategories;
    @FXML private Button btnCommencer;

    private List<String> categoriesSelectionnees = new ArrayList<>();
    private CategorieDAO categorieDAO = new CategorieDAO();
    private List<Categorie> toutesLesCategories = new ArrayList<>();
    private Map<String, CheckBox> checkboxMap = new HashMap<>();

    private NetworkService networkService;
    private NgrokManager ngrokManager; // Use NgrokManager
    private String roomId;
    private String gameMode = "MULTI";

    public void setGameMode(String mode) {
        this.gameMode = mode;
    }

    @FXML
    public void initialize() {
        toutesLesCategories = categorieDAO.getAll();
        chargerCategoriesDynamiquement();
        
        // Start Host Process
        startHostSession();
    }

    private void startHostSession() {
        if (txtLien != null) {
            txtLien.setText("Initialisation du serveur...");
            txtLien.setDisable(true);
        }

        new Thread(() -> {
            try {
                // 1. Start Game Server
                networkService = new NetworkService();
                
                // We use a latch or callback wrapper to sync the async startHost
                // But startHost is async... let's wrap it nicely or assume it's fast enough 
                // Actually startHost uses a thread inside.
                // We want to chain: Server Start -> Ngrok Start.
                
                // Let's modify logic to wait for server or just start Ngrok in parallel/sequence
                // Current startHost takes a callback.
                
                Platform.runLater(() -> {
                    networkService.startHost(
                        org.example.triharf.enums.Langue.FRANCAIS, // Or implicit
                        () -> { 
                            // Server Started Success
                            roomId = networkService.getRoomId();
                            Platform.runLater(() -> txtLien.setText("Démarrage de Ngrok..."));
                            
                            // 2. Start Ngrok in Background
                            new Thread(() -> {
                                try {
                                    ngrokManager = new NgrokManager();
                                    ngrokManager.start(8888); // Blocking call
                                    String url = ngrokManager.getPublicUrl();
                                    
                                    Platform.runLater(() -> {
                                        if (txtLien != null) {
                                            String fullUrl = (url != null ? url : "Erreur") + "/" + roomId;
                                            txtLien.setText(fullUrl);
                                            txtLien.setDisable(false);
                                        }
                                        System.out.println("✅ Ngrok démarré: " + url);
                                    });
                                    
                                } catch (Exception e) {
                                    Platform.runLater(() -> {
                                        if (e.getMessage() != null && e.getMessage().contains("NGROK_AUTH_REQUIRED")) {
                                            // Prompt for token
                                            TextInputDialog dialog = new TextInputDialog();
                                            dialog.setTitle("Configuration Ngrok");
                                            dialog.setHeaderText("Authentification requise");
                                            dialog.setContentText("Veuillez entrer votre authtoken Ngrok :");
                                            
                                            Optional<String> result = dialog.showAndWait();
                                            if (result.isPresent()) {
                                                String token = result.get();
                                                new Thread(() -> {
                                                    try {
                                                        ngrokManager.setAuthToken(token);
                                                        // Retry start
                                                        Platform.runLater(() -> txtLien.setText("Redémarrage de Ngrok..."));
                                                        ngrokManager.start(8888);
                                                        String retryUrl = ngrokManager.getPublicUrl();
                                                        
                                                        Platform.runLater(() -> {
                                                            if (txtLien != null) {
                                                                String fullUrl = (retryUrl != null ? retryUrl : "Erreur") + "/" + roomId;
                                                                txtLien.setText(fullUrl);
                                                                txtLien.setDisable(false);
                                                            }
                                                        });
                                                    } catch (Exception ex) {
                                                        // Fallback to LAN if token invalid
                                                        Platform.runLater(() -> {
                                                            String localIp = org.example.triharf.utils.NetworkUtils.getLocalIpAddress();
                                                            String lanUrl = localIp + ":8888/" + roomId;
                                                            if (txtLien != null) {
                                                                txtLien.setText(lanUrl);
                                                                txtLien.setPromptText("Lien LAN");
                                                                txtLien.setDisable(false);
                                                            }
                                                            showAlert(Alert.AlertType.WARNING, "Mode LAN", "Echec Ngrok. Passage en mode réseau local (Wifi).\nLe lien affiché fonctionne pour les joueurs sur le même réseau que vous.");
                                                        });
                                                    }
                                                }).start();
                                                return; // Exit error handler
                                            } else {
                                                // User cancelled token dialog -> Fallback to LAN
                                                String localIp = org.example.triharf.utils.NetworkUtils.getLocalIpAddress();
                                                String lanUrl = localIp + ":8888/" + roomId;
                                                if (txtLien != null) {
                                                    txtLien.setText(lanUrl);
                                                    txtLien.setDisable(false);
                                                }
                                                return;
                                            }
                                        }
                                        
                                        // General Ngrok Error -> Fallback to LAN
                                        String localIp = org.example.triharf.utils.NetworkUtils.getLocalIpAddress();
                                        String lanUrl = localIp + ":8888/" + roomId;
                                        if (txtLien != null) {
                                            txtLien.setText(lanUrl);
                                            txtLien.setDisable(false);
                                        }
                                        showAlert(Alert.AlertType.WARNING, "Mode LAN Activé", 
                                            "Ngrok n'a pas pu démarrer (" + e.getMessage() + ").\n\n" +
                                            "Le jeu est passé en mode LAN (Réseau Local).\n" +
                                            "Les joueurs doivent être connectés au même Wifi que vous pour rejoindre avec ce lien.");
                                    });
                                }
                            }).start();
                        },
                        (error) -> {
                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.ERROR, "Erreur Serveur", "Impossible de démarrer le serveur: " + error);
                                txtLien.setText("Erreur Serveur");
                            });
                        }
                    );
                });

            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
            }
        }).start();
    }

    // ... Standard methods (chargerCategories, handlers ...)
    
    // Condensed for brevity in replacement, essentially keep existing UI logic
    
    private void chargerCategoriesDynamiquement() {
        // ... (Keep existing implementation)
        if (containerCategories == null) return;
        containerCategories.getChildren().clear();
        checkboxMap.clear();

        javafx.scene.layout.FlowPane flowPane = new javafx.scene.layout.FlowPane();
        flowPane.setHgap(10);
        flowPane.setVgap(10);
        flowPane.setAlignment(javafx.geometry.Pos.CENTER);

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

    @FXML public void handleRetour() {
        stopServices();
        retourMenu();
    }

    private void stopServices() {
        if (ngrokManager != null) {
            ngrokManager.stop();
        }
        if (networkService != null) {
            // networkService.stop(); // Assuming stop method exists or needed
        }
    }

    @FXML public void handleCommencer() {
        if (categoriesSelectionnees.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Sélectionnez au moins une catégorie !");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/fxml/liste_attente.fxml"));
            Parent root = loader.load();
            ListeAttenteController controller = loader.getController();
            
            if (controller != null) {
                controller.setGameMode(this.gameMode);
                controller.setNetwork(networkService);
                controller.setCategories(categoriesSelectionnees);
                if (txtLien != null && !txtLien.getText().isEmpty() && !txtLien.isDisable()) {
                    controller.setNgrokUrl(txtLien.getText());
                }
            }
            
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML public void handleCopier() {
        if (txtLien == null) return;
        String lien = txtLien.getText();
        if (lien.isEmpty() || lien.startsWith("Init") || lien.startsWith("Démarrage")) return;
        
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(lien);
        clipboard.setContent(content);
        // Show small feedback (optional)
        btnCopier.setText("Copié !");
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override public void run() { Platform.runLater(() -> btnCopier.setText("Copier")); }
        }, 1000);
    }

    private void mettreAJourCategories() {
        categoriesSelectionnees.clear();
        for (Categorie cat : toutesLesCategories) {
            CheckBox checkbox = checkboxMap.get(cat.getNom());
            if (checkbox != null && checkbox.isSelected()) {
                categoriesSelectionnees.add(cat.getNom());
            }
        }
    }

    private void retourMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/fxml/main_menu.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
