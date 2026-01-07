package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;
import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.models.Categorie;

import org.example.triharf.services.NetworkService;

import java.io.IOException;
import java.util.*;

/**
 * Contrôleur pour les paramètres de la partie Multijoueur
 * (param_partie_multi.fxml)
 * Passe les données au JeuMultiController via setCategories()
 */
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
    private String roomId;

    private String gameMode = "MULTI";

    public void setGameMode(String mode) {
        this.gameMode = mode;
        System.out.println("Mode de jeu défini sur : " + mode);
    }

    @FXML
    public void initialize() {
        toutesLesCategories = categorieDAO.getAll();
        System.out.println("Catégories chargées depuis DAO : " + toutesLesCategories.size());
        chargerCategoriesDynamiquement();
        initialiserReseau();
    }

    /**
     * Crée les ToggleButtons (chips) dynamiquement à partir des catégories du DAO
     */
    private void chargerCategoriesDynamiquement() {
        if (containerCategories == null) {
            System.err.println("containerCategories n'existe pas dans le FXML !");
            return;
        }

        containerCategories.getChildren().clear();
        checkboxMap.clear();

        // Create a FlowPane for chip layout
        javafx.scene.layout.FlowPane flowPane = new javafx.scene.layout.FlowPane();
        flowPane.setHgap(10);
        flowPane.setVgap(10);
        flowPane.setAlignment(javafx.geometry.Pos.CENTER);

        // Add "Select All" toggle button first
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

            // Store reference using a fake checkbox for the existing map
            CheckBox fakeCheckbox = new CheckBox();
            fakeCheckbox.selectedProperty().bindBidirectional(chip.selectedProperty());
            checkboxMap.put(cat.getNom(), fakeCheckbox);

            flowPane.getChildren().add(chip);
            System.out.println("Chip créé : " + cat.getNom());
        }

        containerCategories.getChildren().add(flowPane);
    }

    @FXML
    public void handleRetour() {
        retourMenu();
    }

    @FXML
    public void handleCommencer() {
        commencerPartie();
    }

    @FXML
    public void handleCopier() {
        copierLien();
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

    private void copierLien() {
        if (txtLien == null) {
            System.out.println("TextField txtLien not found!");
            return;
        }
        String lien = txtLien.getText();
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(lien);
        clipboard.setContent(content);
        showAlert(Alert.AlertType.INFORMATION, "Succès", "Lien copié !");
    }

    private void initialiserReseau() {
        this.networkService = new NetworkService();
        
        networkService.startHost(
            ParametresGenerauxController.langueGlobale,
            () -> { // On Success
                this.roomId = networkService.getRoomId();
                if (txtLien != null) {
                    txtLien.setText(this.roomId);
                }
                System.out.println("✅ Réseau initialisé et salle créée : " + this.roomId);
            },
            (errorMsg) -> { // On Error
                 showAlert(Alert.AlertType.ERROR, "Erreur Réseau", 
                        "Impossible d'initialiser le réseau : " + errorMsg);
            }
        );
    }

    /**
     * Commence la partie multijoueur
     * Redirige vers la salle d'attente
     */
    private void commencerPartie() {
        if (categoriesSelectionnees.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Sélectionnez au moins une catégorie !");
            return;
        }

        System.out.println("✅ Début partie multijoueur");
        System.out.println("   Catégories : " + categoriesSelectionnees);

        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/liste_attente.fxml"));
            Parent root = loader.load();

            // Pass mode and network to ListeAttenteController
            ListeAttenteController controller = loader.getController();
            if (controller != null) {
                controller.setGameMode(this.gameMode);
                controller.setNetwork(networkService);
                controller.setCategories(categoriesSelectionnees);
            }

            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Salle d'attente");

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de partie_multi.fxml");
            e.printStackTrace();
        }
    }

    private void retourMenu() {
        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            if (loader.getLocation() == null) {
                System.err.println("FXML not found: " + fxmlPath);
                return;
            }
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