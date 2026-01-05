package org.example.triharf.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.example.triharf.HelloApplication;

import java.io.IOException;
import java.util.Random;

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
    @FXML
    private javafx.scene.control.TextField tfCodePartie;
    @FXML
    private javafx.scene.control.Button btnRejoindre;

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
        if (btnRejoindre != null) {
            btnRejoindre.setOnAction(e -> handleRejoindre());
        }

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

    private void handleRejoindre() {
        if (tfCodePartie == null) return;
        final String code = tfCodePartie.getText().trim();
        if (code.isEmpty()) return;

        // Désactiver le bouton pour éviter des clics multiples
        if (btnRejoindre != null) btnRejoindre.setDisable(true);

        new Thread(() -> {
            try {
                // Initialiser le client et se connecter
                org.example.triharf.network.GameClient client = new org.example.triharf.network.GameClient();
                
                // Tentative de connexion
                client.connect();
                
                // Si connecté, on envoie le message de join
                String pseudo = ParametresGenerauxController.pseudoGlobal;
                if (pseudo == null || pseudo.isEmpty()) pseudo = "Joueur_" + new Random().nextInt(1000);
                
                client.sendMessage(new org.example.triharf.network.NetworkMessage(
                    org.example.triharf.network.NetworkMessage.Type.JOIN_ROOM,
                    pseudo,
                    code
                ));

                // Naviguer sur le thread UI
                javafx.application.Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/fxml/liste_attente.fxml"));
                        Parent root = loader.load();

                        ListeAttenteController controller = loader.getController();
                        if (controller != null) {
                            controller.setGameMode("MULTI");
                            controller.setNetwork(client, null, code);
                        }

                        Stage stage = (Stage) btnRejoindre.getScene().getWindow();
                        stage.getScene().setRoot(root);
                        stage.setTitle("Salle d'attente");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            } catch (IOException e) {
                javafx.application.Platform.runLater(() -> {
                    if (btnRejoindre != null) btnRejoindre.setDisable(false);
                    showAlert("Erreur de connexion", "Impossible de rejoindre la partie. Vérifiez le code ou assurez-vous que le serveur est lancé.");
                });
            }
        }).start();
    }

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}