package org.example.triharf.services;

import org.example.triharf.dao.MotDAO;
import org.example.triharf.enums.Langue;
import org.example.triharf.models.Mot;
import org.example.triharf.models.Categorie;

public class ValidationService {
    private MotDAO motDAO = new MotDAO();
    private OllamaValidator ollamaValidator = new OllamaValidator();

    public ValidationResult validateMot(String texte, Categorie categorie, Character lettre, Langue langue) {
        if (texte.isEmpty() || Character.toLowerCase(texte.charAt(0)) != Character.toLowerCase(lettre)) {
            return new ValidationResult(false, "Must start with " + lettre, 0);
        }

        // 1. Check local DB
        if (motDAO.exists(texte, categorie, lettre, langue)) {
            Mot mot = motDAO.findByTexteAndCategorie(texte, categorie, langue);
            motDAO.incrementUtilisation(mot);
            int rarity = ScoreCalculator.getLocalRarityScore(mot.getNbUtilisations()); // Use centralized logic
            return new ValidationResult(true, "Valid (local DB)", rarity);
        }

        // 2. Validate with AI
        var aiResponse = ollamaValidator.validateWord(texte, categorie.getNom(), lettre, langue);

        if (aiResponse.isValid()) {
            // 3. Save to DB
            Mot nouveauMot = new Mot(texte, categorie, lettre, langue);
            motDAO.save(nouveauMot);
            return new ValidationResult(true, aiResponse.getMessage(), aiResponse.getRarityScore());
        }

        return new ValidationResult(false, aiResponse.getMessage(), 0);
    }

    public static class ValidationResult {
        private boolean valid;
        private String message;
        private int rarityScore;

        public ValidationResult(boolean valid, String message, int rarityScore) {
            this.valid = valid;
            this.message = message;
            this.rarityScore = rarityScore;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public int getRarityScore() {
            return rarityScore;
        }
    }
}