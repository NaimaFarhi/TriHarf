package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
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
    private Slider sliderDifficulte;

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
        // Écouter les changements de difficulté
        if (sliderDifficulte != null) {
            sliderDifficulte.valueProperty().addListener((obs, oldVal, newVal) -> {
                niveauDifficulte = newVal.intValue();
                System.out.println("Niveau de difficulté : " + getDifficulteLabel());
            });
        }

        // Charger toutes les catégories depuis le DAO
        toutesLesCategories = categorieDAO.getAll();
        System.out.println("Catégories chargées depuis DAO : " + toutesLesCategories.size());

        // Créer dynamiquement les checkboxes
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

        containerCategories.getChildren().clear();
        checkboxMap.clear();

        for (Categorie cat : toutesLesCategories) {
            CheckBox checkbox = new CheckBox(cat.getNom());
            checkbox.setStyle("-fx-font-size: 14;");
            checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> mettreAJourCategories());
            checkboxMap.put(cat.getNom(), checkbox);
            containerCategories.getChildren().add(checkbox);

            System.out.println("Checkbox créée : " + cat.getNom());
        }
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
     * Méthode appelée par le FXML lorsque le slider est relâché
     */
    @FXML
    private void handleDifficulte(MouseEvent event) {
        if (sliderDifficulte != null) {
            niveauDifficulte = (int) sliderDifficulte.getValue();
            System.out.println("Difficulté (clic) : " + getDifficulteLabel());
        }
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
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setTitle("Mode Solo");
            stage.setScene(scene);
            stage.show();

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
