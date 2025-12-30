package org.example.triharf.services;

import org.example.triharf.dao.MotDAO;
import org.example.triharf.models.Mot;
import org.example.triharf.models.Categorie;

public class ValidationService {
    private MotDAO motDAO = new MotDAO();
    private GeminiValidator geminiValidator = new GeminiValidator();

    public ValidationResult validateMot(String texte, Categorie categorie, Character lettre) {
        if (texte.isEmpty() || Character.toLowerCase(texte.charAt(0)) != Character.toLowerCase(lettre)) {
            return new ValidationResult(false, "Must start with " + lettre, 0);
        }

        // 1. Check local DB
        if (motDAO.exists(texte, categorie, lettre)) {
            Mot mot = motDAO.findByTexteAndCategorie(texte, categorie);
            motDAO.incrementUtilisation(mot);
            int rarity = 10 - Math.min(mot.getNbUtilisations(), 9); // More uses = less rare
            return new ValidationResult(true, "Valid (local DB)", rarity);
        }

        // 2. Validate with AI
        var aiResponse = geminiValidator.validateWord(texte, categorie.getNom(), lettre);

        if (aiResponse.isValid()) {
            // 3. Save to DB
            Mot nouveauMot = new Mot(texte, categorie, lettre);
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

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public int getRarityScore() { return rarityScore; }
    }
}