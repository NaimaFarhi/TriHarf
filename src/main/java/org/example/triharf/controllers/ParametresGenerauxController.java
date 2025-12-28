package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;

import java.io.IOException;

/**
 * Contrôleur pour les paramètres généraux de l'app (Configuration.fxml)
 */
public class ParametresGenerauxController {

    @FXML
    private Button btnRetour; // Le bouton "←"

    @FXML
    private RadioButton rbFrancais;
    @FXML
    private RadioButton rbArabic;
    @FXML
    private RadioButton rbEnglish;
    @FXML
    private ToggleGroup langueGroup;

    @FXML
    private TextField tfPseudo;

    @FXML
    private CheckBox checkboxSon;

    @FXML
    private Slider sliderVolume;

    @FXML
    private Label labelVolume; // Pour afficher "70%"

    @FXML
    private Button btnJouer; // "▶ JOUER"

    private String langueSelectionnee = "Français";
    private String pseudo = "";
    private double volume = 70.0;
    private boolean sonActive = true;

    @FXML
    public void initialize() {
        // RadioButtons pour la langue
        if (rbFrancais != null) {
            rbFrancais.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) langueSelectionnee = "Français";
            });
        }
        if (rbArabic != null) {
            rbArabic.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) langueSelectionnee = "Arabe";
            });
        }
        if (rbEnglish != null) {
            rbEnglish.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) langueSelectionnee = "English";
            });
        }

        // Pseudo
        if (tfPseudo != null) {
            tfPseudo.textProperty().addListener((obs, oldVal, newVal) -> {
                pseudo = newVal;
                System.out.println("Pseudo : " + pseudo);
            });
        }

        // Son
        if (checkboxSon != null) {
            checkboxSon.selectedProperty().addListener((obs, oldVal, newVal) -> {
                sonActive = newVal;
                System.out.println("Son " + (newVal ? "activé" : "désactivé"));
                if (sliderVolume != null) {
                    sliderVolume.setDisable(!newVal);
                }
            });
        }

        // Volume
        if (sliderVolume != null) {
            sliderVolume.valueProperty().addListener((obs, oldVal, newVal) -> {
                volume = newVal.doubleValue();
                if (labelVolume != null) {
                    labelVolume.setText((int) volume + "%");
                }
                System.out.println("Volume : " + (int) volume + "%");
            });
        }
    }

    @FXML
    public void handleRetour() {
        retourMenu();
    }

    @FXML
    public void handleLangue() {
        System.out.println("Langue sélectionnée : " + langueSelectionnee);
    }

    @FXML
    public void handleSon() {
        System.out.println("Son " + (sonActive ? "activé" : "désactivé"));
        if (sliderVolume != null) {
            sliderVolume.setDisable(!sonActive);
        }
    }

    @FXML
    public void handleVolume() {
        System.out.println("Volume : " + (int) volume + "%");
    }

    @FXML
    public void handleJouer() {
        allerAuMenu();
    }

    private void allerAuMenu() {
        System.out.println("Paramètres sauvegardés :");
        System.out.println("- Langue : " + langueSelectionnee);
        System.out.println("- Pseudo : " + pseudo);
        System.out.println("- Son : " + (sonActive ? "activé" : "désactivé"));
        System.out.println("- Volume : " + (int) volume + "%");

        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
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
}