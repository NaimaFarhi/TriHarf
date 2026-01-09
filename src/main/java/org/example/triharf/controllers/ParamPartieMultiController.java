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

import java.io.IOException;
import java.util.*;

public class ParamPartieMultiController {

    @FXML
    private Button btnRetour;
    @FXML
    private TextField txtLien;
    @FXML
    private Button btnCopier;
    @FXML
    private VBox containerCategories;
    @FXML
    private Button btnCommencer;

    private List<String> categoriesSelectionnees = new ArrayList<>();
    private CategorieDAO categorieDAO = new CategorieDAO();
    private List<Categorie> toutesLesCategories = new ArrayList<>();
    private Map<String, CheckBox> checkboxMap = new HashMap<>();

    private NetworkService networkService;
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

        // 1. Start Game Server
        networkService = new NetworkService();

        networkService.startHost(
                org.example.triharf.enums.Langue.FRANCAIS,
                () -> {
                    // Server Started Success
                    String localIp = org.example.triharf.utils.NetworkUtils.getLocalIpAddress();
                    String connectionString = localIp + ":8888";

                    Platform.runLater(() -> {
                        if (txtLien != null) {
                            txtLien.setText(connectionString);
                            txtLien.setDisable(false);
                            txtLien.setPromptText("IP à partager");
                        }
                        System.out.println("✅ Serveur démarré: " + connectionString);
                    });
                },
                (error) -> {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Erreur Serveur",
                                "Impossible de démarrer le serveur: " + error);
                        if (txtLien != null)
                            txtLien.setText("Erreur Serveur");
                    });
                });
    }

    // ... Standard methods (chargerCategories, handlers ...)

    // Condensed for brevity in replacement, essentially keep existing UI logic

    private void chargerCategoriesDynamiquement() {
        // ... (Keep existing implementation)
        if (containerCategories == null)
            return;
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

    @FXML
    public void handleRetour() {
        stopServices();
        retourMenu();
    }

    private void stopServices() {
        if (networkService != null) {
            networkService.stop();
        }
    }

    @FXML
    public void handleCommencer() {
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

    @FXML
    public void handleCopier() {
        if (txtLien == null)
            return;
        String lien = txtLien.getText();
        if (lien.isEmpty() || lien.startsWith("Init") || lien.startsWith("Démarrage"))
            return;

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(lien);
        clipboard.setContent(content);
        // Show small feedback (optional)
        btnCopier.setText("Copié !");
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> btnCopier.setText("Copier"));
            }
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
        } catch (IOException e) {
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
