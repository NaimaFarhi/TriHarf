package org.example.triharf.services;

import org.example.triharf.models.Categorie;
import org.example.triharf.models.ResultatPartie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResultsManager {
    private ValidationService validationService = new ValidationService();
    private ScoreCalculator scoreCalculator = new ScoreCalculator();
    private List<ResultatPartie> resultats = new ArrayList<>();
    private int scoreTotal = 0;
    private long startTime;
    private int gameDuration;

    public ResultsManager(int gameDurationSeconds) {
        this.startTime = System.currentTimeMillis();
        this.gameDuration = gameDurationSeconds;
    }

    public void validerMots(Map<Categorie, String> reponses, Character lettre) {
        resultats.clear();
        scoreTotal = 0;

        for (Map.Entry<Categorie, String> entry : reponses.entrySet()) {
            Categorie categorie = entry.getKey();
            String mot = entry.getValue().trim();

            if (mot.isEmpty()) {
                resultats.add(new ResultatPartie(
                        categorie.getNom(), "-", false, 0, "Pas de réponse"
                ));
                continue;
            }

            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            var validationResult = validationService.validateMot(mot, categorie, lettre);

            if (validationResult.isValid()) {
                int points = scoreCalculator.calculateScore(mot, elapsedSeconds, gameDuration);
                scoreTotal += points;
                resultats.add(new ResultatPartie(
                        categorie.getNom(), mot, true, points, validationResult.getMessage()
                ));
            } else {
                resultats.add(new ResultatPartie(
                        categorie.getNom(), mot, false, 0, validationResult.getMessage()
                ));
            }
        }
    }

    public List<ResultatPartie> getResultats() { return resultats; }
    public int getScoreTotal() { return scoreTotal; }
    public long getDureePartie() { return (System.currentTimeMillis() - startTime) / 1000; }
}