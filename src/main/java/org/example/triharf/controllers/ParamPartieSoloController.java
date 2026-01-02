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
import java.util.*;

/**
 * Contrôleur pour les paramètres de la partie Solo (param_partie_solo.fxml)
 */
public class ParamPartieSoloController {

    @FXML
    private Button btnRetour;

    @FXML
    private ToggleButton btnFacile;

    @FXML
    private ToggleButton btnMoyen;

    @FXML
    private ToggleButton btnDifficile;

    @FXML
    private VBox containerCategories; // Le VBox où vont les checkboxes

    @FXML
    private Button btnCommencer;

    private List<String> categoriesSelectionnees = new ArrayList<>();
    private int niveauDifficulte = 1;
    private final CategorieDAO categorieDAO = new CategorieDAO();
    private List<Categorie> toutesLesCategories = new ArrayList<>();
    private final Map<String, CheckBox> checkboxMap = new HashMap<>();

    @FXML
    public void initialize() {
        // Set up ToggleGroup so only one difficulty can be selected
        ToggleGroup difficultyGroup = new ToggleGroup();
        if (btnFacile != null)
            btnFacile.setToggleGroup(difficultyGroup);
        if (btnMoyen != null)
            btnMoyen.setToggleGroup(difficultyGroup);
        if (btnDifficile != null)
            btnDifficile.setToggleGroup(difficultyGroup);

        // Listen for difficulty changes
        difficultyGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == btnFacile) {
                niveauDifficulte = 0;
            } else if (newVal == btnMoyen) {
                niveauDifficulte = 1;
            } else if (newVal == btnDifficile) {
                niveauDifficulte = 2;
            }
            System.out.println("Niveau de difficulté : " + getDifficulteLabel());
        });

        // Charger toutes les catégories depuis le DAO
        toutesLesCategories = categorieDAO.getAll();
        System.out.println("Catégories chargées depuis DAO : " + toutesLesCategories.size());

        // Créer dynamiquement les checkboxes
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
            System.out.println("Chip créé : " + cat.getNom());
        }

        containerCategories.getChildren().add(flowPane);
    }

    /**
     * Met à jour la liste des catégories sélectionnées
     */
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

    @FXML
    public void handleRetour() {
        retourMenu();
    }

    @FXML
    public void handleCommencer() {
        commencerPartie();
    }

    /**
     * Commence la partie solo après validation des catégories
     */
    private void commencerPartie() {
        if (categoriesSelectionnees.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Sélectionnez au moins une catégorie !");
            return;
        }

        System.out.println("Début partie solo - Difficulté: " + getDifficulteLabel());
        System.out.println("Catégories : " + categoriesSelectionnees);

        navigateToJeuSolo();
    }

    /**
     * Navigue vers partie.fxml et passe les données au controller
     */
    private void navigateToJeuSolo() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/fxml/partie.fxml"));
            Parent root = loader.load();

            // Récupérer le controller de partie.fxml
            JeuSoloController controller = loader.getController();
            controller.setCategories(categoriesSelectionnees);
            controller.setDifficulte(niveauDifficulte);

            // Démarrer la partie après avoir injecté les données
            controller.demarrerPartie();

            // Afficher la nouvelle scène
            // Afficher la nouvelle scène
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Mode Solo");

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la scène partie.fxml");
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
            System.err.println("Erreur: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private String getDifficulteLabel() {
        return switch (niveauDifficulte) {
            case 0 -> "Facile";
            case 1 -> "Moyen";
            case 2 -> "Difficile";
            default -> "Moyen";
        };
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
