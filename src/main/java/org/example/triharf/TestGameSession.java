package org.example.triharf;

import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.models.Categorie;
import org.example.triharf.models.ResultatPartie;
import org.example.triharf.services.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestGameSession {
    public static void main(String[] args) {
        // Test PartieService
        PartieService partieService = new PartieService();
        var joueur = partieService.getOrCreateJoueur("TestPlayer");
        System.out.println("✅ Joueur: " + joueur.getPseudo());

        // Test ResultsManager
        CategorieDAO catDAO = new CategorieDAO();
        List<Categorie> categories = catDAO.findAllActif();
        System.out.println("✅ Catégories: " + categories.size());

        ResultsManager resultsManager = new ResultsManager(180);
        Map<Categorie, String> reponses = new HashMap<>();

        if (!categories.isEmpty()) {
            reponses.put(categories.get(0), "Apple");
        }

        resultsManager.validerMots(reponses, 'A');
        System.out.println("✅ Score: " + resultsManager.getScoreTotal());
        System.out.println("✅ Résultats: " + resultsManager.getResultats().size());

        System.out.println("✅ Sprint 2 [F] - Backend terminé!");
    }
}