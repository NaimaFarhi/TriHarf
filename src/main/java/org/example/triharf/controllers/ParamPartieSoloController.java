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
 * Contrôleur pour les paramètres de la partie Solo (param_partie_solo.fxml)
 */
public class ParamPartieSoloController {

    @FXML
    private Button btnRetour;

    @FXML
    private Slider sliderDifficulte;

    @FXML
    private VBox containerCategories; // Le VBox où vont les checkboxes

    @FXML
    private Button btnCommencer;

    private List<String> categoriesSelectionnees = new ArrayList<>();
    private int niveauDifficulte = 1;
    private CategorieDAO categorieDAO = new CategorieDAO();
    private List<Categorie> toutesLesCategories = new ArrayList<>();
    private Map<String, CheckBox> checkboxMap = new HashMap<>();

    @FXML
    public void initialize() {
        // Écouter les changements de difficulté
        if (sliderDifficulte != null) {
            sliderDifficulte.valueProperty().addListener((obs, oldVal, newVal) -> {
                niveauDifficulte = newVal.intValue();
                System.out.println("Niveau de difficulté : " + getDifficulteLabel());
            });
        }

        // Charger TOUTES les catégories depuis le DAO
        toutesLesCategories = categorieDAO.getAll();

        System.out.println("Catégories chargées depuis DAO : " + toutesLesCategories.size());

        // Créer dynamiquement les checkboxes pour chaque catégorie
        chargerCategoriesDynamiquement();
    }

    /**
     * Crée les checkboxes dynamiquement à partir des catégories du DAO
     */
    private void chargerCategoriesDynamiquement() {
        if (containerCategories == null) {
            System.err.println("containerCategories n'existe pas dans le FXML !");
            return;
        }

        // Effacer les anciens contrôles
        containerCategories.getChildren().clear();

        // Créer une checkbox pour chaque catégorie
        for (Categorie cat : toutesLesCategories) {
            CheckBox checkbox = new CheckBox(cat.getNom());
            checkbox.setStyle("-fx-font-size: 14;");

            // Ajouter un listener à chaque checkbox
            checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> mettreAJourCategories());

            // Ajouter à la map pour retrouver la checkbox par nom
            checkboxMap.put(cat.getNom(), checkbox);

            // Ajouter au conteneur
            containerCategories.getChildren().add(checkbox);

            System.out.println("Checkbox créée : " + cat.getNom());
        }
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
    public void handleDifficulte() {
        // Cette méthode est appelée par le slider
        if (sliderDifficulte != null) {
            niveauDifficulte = (int) sliderDifficulte.getValue();
            System.out.println("Niveau de difficulté : " + getDifficulteLabel());
        }
    }

    /**
     * Met à jour la liste des catégories sélectionnées
     */
    private void mettreAJourCategories() {
        categoriesSelectionnees.clear();

        // Parcourir toutes les catégories et ajouter les cochées
        for (Categorie cat : toutesLesCategories) {
            CheckBox checkbox = checkboxMap.get(cat.getNom());
            if (checkbox != null && checkbox.isSelected()) {
                categoriesSelectionnees.add(cat.getNom());
            }
        }

        System.out.println("Catégories sélectionnées : " + categoriesSelectionnees);
    }

    private void commencerPartie() {
        if (categoriesSelectionnees.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Sélectionnez au moins une catégorie !");
            return;
        }
        System.out.println("Début partie solo - Difficulté: " + getDifficulteLabel());
        System.out.println("Catégories : " + categoriesSelectionnees);
        navigateTo("/fxml/partie.fxml", "Mode Solo");
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
            Scene scene = new Scene(root);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
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