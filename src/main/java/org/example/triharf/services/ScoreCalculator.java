package org.example.triharf.services;

import org.example.triharf.dao.MotDAO;
import org.example.triharf.models.Mot;

public class ScoreCalculator {
    private static final int BASE_POINTS = 10;
    private static final int LENGTH_BONUS_THRESHOLD = 6;
    private static final int LENGTH_BONUS = 5;
    private static final int SPEED_BONUS_MAX = 10;

    private MotDAO motDAO = new MotDAO();

    public int calculateScore(String texte, long elapsedSeconds, int totalTime) {
        int score = BASE_POINTS;

        // Bonus longueur
        if (texte.length() >= LENGTH_BONUS_THRESHOLD) {
            score += LENGTH_BONUS;
        }

        // Bonus rapidité
        double speedRatio = (double) elapsedSeconds / totalTime;
        if (speedRatio < 0.3) {
            score += SPEED_BONUS_MAX;
        } else if (speedRatio < 0.5) {
            score += SPEED_BONUS_MAX / 2;
        }

        return score;
    }

    public int calculateRarityBonus(Mot mot) {
        int nbUtilisations = mot.getNbUtilisations();

        // Plus rare = plus de points
        if (nbUtilisations == 1) return 10;
        if (nbUtilisations <= 3) return 5;
        if (nbUtilisations <= 10) return 2;
        return 0;
    }

    public int calculateTotalScore(Mot mot, long elapsedSeconds, int totalTime) {
        int baseScore = calculateScore(mot.getTexte(), elapsedSeconds, totalTime);
        int rarityBonus = calculateRarityBonus(mot);
        return baseScore + rarityBonus;
    }
}