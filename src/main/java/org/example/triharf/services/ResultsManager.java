package org.example.triharf.services;

import org.example.triharf.enums.Langue;
import org.example.triharf.models.Categorie;
import org.example.triharf.models.ResultatPartie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ResultsManager {
    private ValidationService validationService = new ValidationService();
    private ScoreCalculator scoreCalculator = new ScoreCalculator();
    private List<ResultatPartie> resultats = new ArrayList<>();
    private int scoreTotal = 0;
    private long gameStartTime;
    private int gameDuration;
    private Map<Categorie, Long> submissionTimes = new HashMap<>();

    public ResultsManager(int gameDurationSeconds) {
        this.gameStartTime = System.currentTimeMillis();
        this.gameDuration = gameDurationSeconds;
    }

    /**
     * Record when a word was submitted for a category
     */
    public void recordSubmissionTime(Categorie categorie) {
        submissionTimes.put(categorie, System.currentTimeMillis());
    }

    public void validerMots(Map<Categorie, String> reponses, Character lettre, Langue langue) {
        // resultats.clear(); // Non nécessaire car on écrase la liste plus bas

        // Validate in parallel
        List<CompletableFuture<ResultatPartie>> futures = reponses.entrySet().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> {
                    Categorie categorie = entry.getKey();
                    String mot = entry.getValue().trim();

                    if (mot.isEmpty()) {
                        ResultatPartie res = new ResultatPartie(categorie.getNom(), "-", false, 0, "Pas de réponse");
                        res.setCategorieObj(categorie);
                        return res;
                    }

                    long wordSubmissionTime = submissionTimes.getOrDefault(categorie, System.currentTimeMillis());
                    long elapsedSeconds = (wordSubmissionTime - gameStartTime) / 1000;

                    var validationResult = validationService.validateMot(mot, categorie, lettre, langue);

                    if (validationResult.isValid()) {
                        int points = scoreCalculator.calculateTotalScore(mot, elapsedSeconds, gameDuration, validationResult.getRarityScore());
                        ResultatPartie res = new ResultatPartie(categorie.getNom(), mot, true, points, validationResult.getMessage() + " (+" + points + "pts)");
                        res.setCategorieObj(categorie); // Set object for DB link
                        return res;
                    } else {
                        ResultatPartie res = new ResultatPartie(categorie.getNom(), mot, false, 0, validationResult.getMessage());
                        res.setCategorieObj(categorie); // Set object for DB link
                        return res;
                    }
                }))
                .toList();

        // Wait for all and collect in a MUTABLE list
        resultats = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toCollection(ArrayList::new));
        scoreTotal = getScoreTotalStreams();
    }

    public List<ResultatPartie> getResultats() { return resultats; }
    public int getScoreTotal() { return scoreTotal; }
    public long getDureePartie() { return (System.currentTimeMillis() - gameStartTime) / 1000; }

    // Use streams for score total instead of manual counting
    public int getScoreTotalStreams() {
        return resultats.stream()
                .mapToInt(ResultatPartie::getPoints)
                .sum();
    }

    // Filter valid results only
    public List<ResultatPartie> getValidResults() {
        return resultats.stream()
                .filter(ResultatPartie::isValide)
                .toList();
    }

    // Group scores by category
    public Map<String, Integer> getScoresByCategory() {
        return resultats.stream()
                .collect(Collectors.groupingBy(
                        ResultatPartie::getCategorie,
                        Collectors.summingInt(ResultatPartie::getPoints)
                ));
    }

    // Count valid vs invalid
    public long countValid() {
        return resultats.stream().filter(ResultatPartie::isValide).count();
    }
}