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
 * Contrôleur pour les paramètres de la partie Multijoueur (param_partie_multi.fxml)
 */
public class ParamPartieMultiController {

    @FXML
    private Button btnRetour;

    @FXML
    private TextField txtLien;

    @FXML
    private Button btnCopier;

    @FXML
    private VBox containerCategories; // Le VBox où vont les checkboxes

    @FXML
    private Button btnCommencer;

    private List<String> categoriesSelectionnees = new ArrayList<>();
    private CategorieDAO categorieDAO = new CategorieDAO();
    private List<Categorie> toutesLesCategories = new ArrayList<>();
    private Map<String, CheckBox> checkboxMap = new HashMap<>(); // Pour retrouver les checkboxes par nom

    @FXML
    public void initialize() {
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
    public void handleCopier() {
        copierLien();
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

    private void commencerPartie() {
        if (categoriesSelectionnees.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Sélectionnez au moins une catégorie !");
            return;
        }
        System.out.println("Début partie multijoueur avec catégories : " + categoriesSelectionnees);
        navigateTo("/fxml/partie_multi.fxml", "Mode Multijoueur");
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}