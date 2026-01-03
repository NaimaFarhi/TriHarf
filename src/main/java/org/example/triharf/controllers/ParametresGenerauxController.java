package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;
import org.example.triharf.enums.Langue;
import org.example.triharf.services.GameSession;
import org.example.triharf.services.SessionManager;

import java.io.IOException;

/**
 * Contrôleur pour les paramètres généraux (Configuration.fxml)
 */
public class ParametresGenerauxController {

    // Trigger recompile
    // ================= STATIC SETTINGS =================
    // Accessible depuis n'importe quel contrôleur
    public static Langue langueGlobale = Langue.FRANCAIS;
    public static String pseudoGlobal = "Joueur" + (int)(Math.random() * 9000 + 1000);

    @FXML private Button btnRetour;
    @FXML private Button btnJouer;

    @FXML private RadioButton rbFrancais;
    @FXML private RadioButton rbArabe;
    @FXML private RadioButton rbAnglais;
    @FXML private ToggleGroup langueGroup;

    @FXML private TextField txtPseudo;
    @FXML private CheckBox cbMute;
    @FXML private Slider sliderVolume;
    // @FXML private Label labelVolume; // N'existe pas dans le FXML

    private String langueSelectionnee = "Français";
    private String pseudo = "";
    private double volume = 70.0;
    private boolean sonActive = true;

    @FXML
    public void initialize() {

        // Sélection par défaut
        if (rbFrancais != null) rbFrancais.setSelected(true);
        // labelVolume.setText((int) volume + "%"); // Retiré car pas de label

        // Langue
        langueGroup.selectedToggleProperty().addListener((obs, old, selected) -> {
            if (selected == rbFrancais) langueSelectionnee = "Français";
            else if (selected == rbArabe) langueSelectionnee = "Arabe";
            else if (selected == rbAnglais) langueSelectionnee = "English";
        });

        // Pseudo
        if (txtPseudo != null) {
            // Afficher le pseudo global actuel (généré aléatoirement ou modifié)
            txtPseudo.setText(pseudoGlobal);
            pseudo = pseudoGlobal;
            
            txtPseudo.textProperty().addListener((obs, old, value) -> pseudo = value);
        }

        // Son (cbMute = true => sonActive = false)
        if (cbMute != null) {
            cbMute.selectedProperty().addListener((obs, old, isMuted) -> {
                sonActive = !isMuted;
                if (sliderVolume != null) sliderVolume.setDisable(isMuted);
            });
        }

        // Volume
        if (sliderVolume != null) {
            sliderVolume.valueProperty().addListener((obs, old, value) -> {
                volume = value.doubleValue();
                // if (labelVolume != null) labelVolume.setText((int) volume + "%");
            });
        }
    }

    // ================= ACTIONS =================

    @FXML
    public void handleJouer() {

        sauvegarderParametres();

        if (pseudo == null || pseudo.isBlank()) {
            pseudo = pseudoGlobal; // Keep existing random pseudo instead of resetting to "Joueur"
        }

        // Mise à jour des globales
        pseudoGlobal = pseudo;
        langueGlobale = getLangueEnum();

        System.out.println("🎮 Paramètres Validés : " + pseudoGlobal + " | " + langueGlobale);

        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
    }

    @FXML
    public void handleRetour() {
        navigateTo("/fxml/main_menu.fxml", "Menu Principal");
    }
    @FXML
    public void handleEnregistrer() {
        sauvegarderParametres();
        
        // Mise à jour des globales
        pseudoGlobal = pseudo;
        langueGlobale = getLangueEnum();
        
        System.out.println("💾 Enregistrement terminé: " + pseudoGlobal + " / " + langueGlobale);
    }
    // ================= LOGIQUE =================

    private void sauvegarderParametres() {
        System.out.println("✅ Paramètres sauvegardés :");
        System.out.println("- Langue : " + langueSelectionnee);
        System.out.println("- Pseudo : " + pseudo);
        System.out.println("- Son : " + (sonActive ? "activé" : "désactivé"));
        System.out.println("- Volume : " + (int) volume + "%");
    }

    private Langue getLangueEnum() {
        return switch (langueSelectionnee) {
            case "Français" -> Langue.FRANCAIS;
            case "Arabe" -> Langue.ARABE;
            case "English" -> Langue.ENGLISH;
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
