package org.example.triharf.controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.triharf.HelloApplication;
import org.example.triharf.enums.Langue;
import org.example.triharf.utils.PropertiesManager;

import java.io.IOException;

public class ParametresGenerauxController {

    public static Langue langueGlobale = Langue.FRANCAIS;
    public static String pseudoGlobal;

    @FXML private Button btnRetour;
    @FXML private Button btnEnregistrer;

    @FXML private RadioButton rbFrancais;
    @FXML private RadioButton rbArabe;
    @FXML private RadioButton rbAnglais;
    @FXML private ToggleGroup langueGroup;

    @FXML private TextField txtPseudo;
    @FXML private CheckBox cbMute;
    @FXML private Slider sliderVolume;
    @FXML private Label lblSaveConfirmation; // Add to FXML

    private String langueSelectionnee = "Français";
    private String pseudo = "";
    private double volume = 70.0;
    private boolean sonActive = true;

    static {
        // Generate random pseudo on first launch
        String savedPseudo = PropertiesManager.getProperty("player.pseudo");
        if (savedPseudo == null || savedPseudo.isBlank()) {
            pseudoGlobal = "Joueur" + (int)(Math.random() * 9000 + 1000);
            PropertiesManager.setProperty("player.pseudo", pseudoGlobal);
            PropertiesManager.saveProperties();
        } else {
            pseudoGlobal = savedPseudo;
        }

        // Load saved language
        String savedLangue = PropertiesManager.getProperty("player.langue", "FRANCAIS");
        langueGlobale = Langue.valueOf(savedLangue);
    }

    @FXML
    public void initialize() {
        // Set confirmation label invisible initially
        if (lblSaveConfirmation != null) {
            lblSaveConfirmation.setVisible(false);
        }

        // Load saved language selection
        switch (langueGlobale) {
            case FRANCAIS -> rbFrancais.setSelected(true);
            case ANGLAIS -> rbAnglais.setSelected(true);
            case ARABE -> rbArabe.setSelected(true);
        }

        // Language selection listener
        langueGroup.selectedToggleProperty().addListener((obs, old, selected) -> {
            if (selected == rbFrancais) langueSelectionnee = "Français";
            else if (selected == rbArabe) langueSelectionnee = "Arabe";
            else if (selected == rbAnglais) langueSelectionnee = "English";
        });

        // Pseudo field
        if (txtPseudo != null) {
            txtPseudo.setText(pseudoGlobal);
            pseudo = pseudoGlobal;
            txtPseudo.textProperty().addListener((obs, old, value) -> pseudo = value);
        }

        // Sound settings
        if (cbMute != null) {
            cbMute.selectedProperty().addListener((obs, old, isMuted) -> {
                sonActive = !isMuted;
                if (sliderVolume != null) sliderVolume.setDisable(isMuted);
            });
        }

        if (sliderVolume != null) {
            sliderVolume.valueProperty().addListener((obs, old, value) -> {
                volume = value.doubleValue();
            });
        }
    }

    @FXML
    public void handleEnregistrer() {
        sauvegarderParametres();

        // Update global variables
        if (pseudo != null && !pseudo.isBlank()) {
            pseudoGlobal = pseudo;
        }
        langueGlobale = getLangueEnum();

        // Save language preference
        PropertiesManager.setProperty("player.langue", langueGlobale.name());
        PropertiesManager.saveProperties();

        // Show confirmation message
        showSaveConfirmation();

        System.out.println("💾 Enregistrement terminé: " + pseudoGlobal + " / " + langueGlobale);
    }

    @FXML
    public void handleRetour() {
        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
    }

    private void showSaveConfirmation() {
        if (lblSaveConfirmation != null) {
            lblSaveConfirmation.setText("✅ Paramètres sauvegardés avec succès !");
            lblSaveConfirmation.setVisible(true);

            // Hide after 3 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> lblSaveConfirmation.setVisible(false));
            pause.play();
        }
    }

    private void sauvegarderParametres() {
        System.out.println("✅ Paramètres sauvegardés :");
        System.out.println("- Langue : " + langueSelectionnee);
        System.out.println("- Pseudo : " + pseudo);
        System.out.println("- Son : " + (sonActive ? "activé" : "désactivé"));
        System.out.println("- Volume : " + (int) volume + "%");

        if (pseudo != null && !pseudo.isBlank()) {
            PropertiesManager.setProperty("player.pseudo", pseudo);
            PropertiesManager.saveProperties();
        }
    }

    private Langue getLangueEnum() {
        return switch (langueSelectionnee) {
            case "Français" -> Langue.FRANCAIS;
            case "Arabe" -> Langue.ARABE;
            case "English" -> Langue.ANGLAIS;
            default -> Langue.FRANCAIS;
        };
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}