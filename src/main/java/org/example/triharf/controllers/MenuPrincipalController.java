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
    public void initialize() {
        // Les actions des boutons
        btnSolo.setOnAction(e -> navigateTo("/fxml/param_partie_solo.fxml", "Paramètres - Mode Solo"));
        btnMultijoueur.setOnAction(e -> navigateTo("/fxml/param_partie_multi.fxml", "Paramètres - Multijoueur"));
        btnBattleRoyale.setOnAction(e -> navigateTo("/fxml/param_partie_multi.fxml", "Paramètres - Battle Royale"));
        btnChaos.setOnAction(e -> navigateTo("/fxml/param_partie_multi.fxml", "Paramètres - Chaos Mode"));

        btnParametres.setOnAction(e -> navigateTo("/fxml/Configuration.fxml", "Paramètres"));
    }

    /**
     * Navigue vers une autre vue FXML
     */
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) btnSolo.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de " + fxmlPath);
            e.printStackTrace();
        }
    }
}