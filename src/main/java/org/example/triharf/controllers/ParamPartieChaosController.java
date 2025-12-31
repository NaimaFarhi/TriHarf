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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour les paramètres du mode CHAOS
 * (param_partie_chaos.fxml)
 */
public class ParamPartieChaosController {

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

    @FXML
    public void initialize() {
        toutesLesCategories = categorieDAO.getAll();
        System.out.println("Catégories chargées depuis DAO : " + toutesLesCategories.size());
        chargerCategoriesDynamiquement();
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
        if (txtLien == null)
            return;
        String lien = txtLien.getText();
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(lien);
        clipboard.setContent(content);
        showAlert(Alert.AlertType.INFORMATION, "Succès", "Lien copié !");
    }

    /**
     * Commence la partie chaos
     * Redirige directement vers partie_chaos.fxml
     */
    private void commencerPartie() {
        if (categoriesSelectionnees.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Sélectionnez au moins une catégorie !");
            return;
        }

        System.out.println("✅ Début partie CHAOS");
        System.out.println("   Catégories : " + categoriesSelectionnees);

        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/partie_chaos.fxml"));
            Parent root = loader.load();

            // Here we would pass checks to chaos controller if needed
            // JeuChaosController controller = loader.getController();
            // controller.setCategories(categoriesSelectionnees);

            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Mode CHAOS");

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de partie_chaos.fxml");
            e.printStackTrace();
        }
    }

    private void retourMenu() {
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
