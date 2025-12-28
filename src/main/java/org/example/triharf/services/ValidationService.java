package org.example.triharf.services;

import org.example.triharf.dao.MotDAO;
import org.example.triharf.models.Mot;
import org.example.triharf.models.Categorie;

public class ValidationService {
    private MotDAO motDAO = new MotDAO();
    private APIValidator apiValidator = new APIValidator();

    public ValidationResult validateMot(String texte, Categorie categorie, Character lettre) {
        // Vérifier si commence par la bonne lettre
        if (texte.isEmpty() || Character.toLowerCase(texte.charAt(0)) != Character.toLowerCase(lettre)) {
            return new ValidationResult(false, "Le mot doit commencer par " + lettre);
        }

        // 1. Vérifier dans la base locale
        if (motDAO.exists(texte, categorie, lettre)) {
            Mot mot = motDAO.findByTexteAndCategorie(texte, categorie);
            motDAO.incrementUtilisation(mot);
            return new ValidationResult(true, "Mot validé (base locale)");
        }

        // 2. Valider via API
        boolean valideAPI = apiValidator.validateWord(texte);

        if (valideAPI) {
            // 3. Insérer dans la base
            Mot nouveauMot = new Mot(texte, categorie, lettre);
            motDAO.save(nouveauMot);
            return new ValidationResult(true, "Mot validé (API)");
        }

        return new ValidationResult(false, "Mot non trouvé");
    }

    public static class ValidationResult {
        private boolean valid;
        private String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}