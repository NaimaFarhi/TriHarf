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
    private javafx.scene.control.Label lblVictoires;
    @FXML
    private javafx.scene.control.Label lblParties;
    @FXML
    private javafx.scene.control.Label lblBestScore;
    @FXML
    private javafx.scene.layout.VBox vboxRecords;

    private org.example.triharf.services.StatisticsService statisticsService = new org.example.triharf.services.StatisticsService();

    @FXML
    public void initialize() {
        // Cleaning (Décommenter pour remettre à zéro)
        // new org.example.triharf.dao.MotDAO().deleteAll();

        // Les actions des boutons
        btnSolo.setOnAction(e -> navigateTo("/fxml/param_partie_solo.fxml", "Paramètres - Mode Solo"));
        btnMultijoueur.setOnAction(e -> navigateTo("/fxml/param_partie_multi.fxml", "Paramètres - Multijoueur"));
        btnBattleRoyale.setOnAction(e -> navigateTo("/fxml/param_partie_multi.fxml", "Paramètres - Battle Royale"));
        btnChaos.setOnAction(e -> navigateTo("/fxml/param_partie_chaos.fxml", "Paramètres - Mode Chaos"));

        btnParametres.setOnAction(e -> navigateTo("/fxml/Configuration.fxml", "Paramètres"));

        loadStatistics();
    }

    private void loadStatistics() {
        String joueur = ParametresGenerauxController.pseudoGlobal;
        if (joueur == null || joueur.isEmpty()) {
            joueur = "Invité"; // Fallback if no pseudo set yet
        }

        // Fetch Global Stats
        java.util.Map<String, Object> stats = statisticsService.getGlobalStats(joueur);
        
        if (lblParties != null) {
            lblParties.setText(String.valueOf(stats.getOrDefault("totalParties", 0)));
        }
        if (lblBestScore != null) {
            lblBestScore.setText(String.valueOf(stats.getOrDefault("meilleurScore", 0)));
        }
        if (lblVictoires != null) {
            // "Victoires" logic: strictly speaking, we don't have a "Win/Loss" in Solo yet, 
            // but we can simulate it or calculate "Success Rate" instead.
            // For now, let's display success rate as "Victoires" or just hide/change label meaning?
            // The FXML says "Victoires", let's map it to Success Rate or Wins if available.
            // StatisticsService has getGlobalSuccessRate.
            double successRate = statisticsService.getGlobalSuccessRate(joueur);
            lblVictoires.setText(String.format("%.1f%%", successRate));
        }

        // Fetch Records (Top Categories)
        if (vboxRecords != null) {
            vboxRecords.getChildren().clear();
            var catStats = statisticsService.getStatsByCategorie(joueur);
            
            // Sort by success rate descending
            catStats.entrySet().stream()
                .sorted((e1, e2) -> {
                    double r1 = (double) ((java.util.Map) e1.getValue()).get("tauxReussite");
                    double r2 = (double) ((java.util.Map) e2.getValue()).get("tauxReussite");
                    return Double.compare(r2, r1);
                })
                .limit(5)
                .forEach(entry -> {
                    String catName = entry.getKey();
                    java.util.Map val = (java.util.Map) entry.getValue();
                    double rate = (double) val.get("tauxReussite");
                    int reussies = (int) val.get("reussies");
                    
                    javafx.scene.control.Label lbl = new javafx.scene.control.Label(
                        String.format("%s: %.0f%% (%d mots)", catName, rate, reussies)
                    );
                    lbl.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");
                    vboxRecords.getChildren().add(lbl);
                });
        }
    }

    /**
     * Navigue vers une autre vue FXML
     */
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent root = loader.load();

            // Set Game Mode if navigating to Multiplayer Parameters
            Object controller = loader.getController();
            if (controller instanceof ParamPartieMultiController) {
                if (title.contains("Battle Royale")) {
                    ((ParamPartieMultiController) controller).setGameMode("BATTLE");
                } else {
                    ((ParamPartieMultiController) controller).setGameMode("MULTI");
                }
            }

            Stage stage = (Stage) btnSolo.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de " + fxmlPath);
            e.printStackTrace();
        }
    }
}