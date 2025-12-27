package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;

import java.io.IOException;

public class MenuPrincipalController {
    // smiyaat les boutons hta nchof fxml chno atsmihom oumnia
    @FXML
    private Button btnSolo;

    @FXML
    private Button btnMultijoueur;

    @FXML
    private Button btnBattleRoyale;

    @FXML
    private Button btnChaos;

    @FXML
    private Button btnParametres;

    @FXML
    private Button btnStatistiques;

    @FXML
    public void initialize() {

        btnSolo.setOnAction(e -> navigateTo("solo-view.fxml", "Mode Solo"));
        btnMultijoueur.setOnAction(e -> navigateTo("multijoueur-view.fxml", "Mode Multijoueur"));
        btnBattleRoyale.setOnAction(e -> navigateTo("batailleroyale-view.fxml", "Battle Royale"));
        btnChaos.setOnAction(e -> navigateTo("chaos-view.fxml", "Chaos Mode"));
        btnStatistiques.setOnAction(e -> navigateTo("statistiques-view.fxml", "Statistiques"));

    }

    /**
     * Navigue vers une autre vue FXML
     * @param fxmlFile nom du fichier FXML
     * @param title titre de la fenêtre
     */
    private void navigateTo(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("views/" + fxmlFile));
            Parent root = loader.load();

            Stage stage = (Stage) btnSolo.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de " + fxmlFile);
            e.printStackTrace();
        }
    }
}