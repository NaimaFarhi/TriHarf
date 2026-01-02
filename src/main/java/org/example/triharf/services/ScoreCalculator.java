package org.example.triharf.services;

import org.example.triharf.dao.MotDAO;
import org.example.triharf.models.Mot;

public class ScoreCalculator {
    private static final int BASE_POINTS = 5; // Reduced base
    private static final int LENGTH_BONUS_PER_CHAR = 1;
    private static final int LENGTH_BONUS_THRESHOLD = 4;
    private static final int SPEED_BONUS_MAX = 15;
    private static final int RARITY_BONUS_MAX = 10;

    private MotDAO motDAO = new MotDAO();

    /**
     * Calculate total score for a word
     * 
     * @param texte          The word
     * @param elapsedSeconds Time taken to submit (from game start)
     * @param totalTime      Total game duration
     * @param rarityScore    Rarity from AI (1-10)
     * @param isOriginal     True if no other player used this word (multiplayer)
     * @return Total score
     */
    public int calculateTotalScore(String texte, long elapsedSeconds, int totalTime,
            int rarityScore, boolean isOriginal) {
        int score = 0;

        // 1. Base points (only if word length > 2)
        if (texte.length() <= 2) {
            return 0; // Too short = no points
        }
        score += BASE_POINTS;

        // 2. Length bonus (1 point per character above threshold)
        if (texte.length() >= LENGTH_BONUS_THRESHOLD) {
            score += (texte.length() - LENGTH_BONUS_THRESHOLD + 1) * LENGTH_BONUS_PER_CHAR;
        }

        // 3. Speed bonus (diminishes over time)
        double speedRatio = (double) elapsedSeconds / totalTime;
        if (speedRatio <= 0.2) {
            score += SPEED_BONUS_MAX; // Super fast: 0-20% of time
        } else if (speedRatio <= 0.4) {
            score += (int) (SPEED_BONUS_MAX * 0.7); // Fast: 20-40%
        } else if (speedRatio <= 0.6) {
            score += (int) (SPEED_BONUS_MAX * 0.4); // Medium: 40-60%
        } else if (speedRatio <= 0.8) {
            score += (int) (SPEED_BONUS_MAX * 0.2); // Slow: 60-80%
        }
        // No bonus if > 80% of time used

        // 4. Rarity bonus from AI (1-10)
        score += rarityScore;

        // 5. Originality penalty (multiplayer)
        if (!isOriginal) {
            score = (int) (score * 0.5); // 50% penalty if duplicate
        }

        return Math.max(score, 1); // Minimum 1 point for valid word
    }

    /**
     * Overload for solo mode (always original)
     */
    public int calculateTotalScore(String texte, long elapsedSeconds, int totalTime, int rarityScore) {
        return calculateTotalScore(texte, elapsedSeconds, totalTime, rarityScore, true);
    }

    /**
     * Calculate originality for multiplayer
     */
    public int calculateOriginalityCount(String texte, long categorieId) {
        // This will be used in multiplayer to count duplicate words
        return 1; // Placeholder for Sprint 3
    }

    /**
     * Get local rarity based on usage count
     */
    /**
     * Get local rarity based on usage count
     */
    public static int getLocalRarityScore(Mot mot) {
        return getLocalRarityScore(mot.getNbUtilisations());
    }

    /**
     * Overload for direct usage count (useful for validation service before Mot
     * exists)
     */
    public static int getLocalRarityScore(int nbUtilisations) {
        if (nbUtilisations == 1)
            return 10; // First time used
        if (nbUtilisations <= 3)
            return 8; // Rare
        if (nbUtilisations <= 10)
            return 5; // Uncommon
        if (nbUtilisations <= 50)
            return 3; // Common
        return 1; // Very common
    }
}