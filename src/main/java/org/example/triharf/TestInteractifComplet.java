package org.example.triharf;

import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.enums.Langue;
import org.example.triharf.models.Categorie;
import org.example.triharf.services.*;

import java.util.*;

public class TestInteractifComplet {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Setup
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  Test Complet - Sprint 1 & 2        ║");
        System.out.println("╚══════════════════════════════════════╝\n");


        CategorieDAO catDAO = new CategorieDAO();
        GameEngine engine = new GameEngine();
        ResultsManager resultsManager = new ResultsManager(180);

        // Test 1: Letter Generation
        System.out.println("=== Test 1: Génération Lettre (Streams) ===");
        Character lettre = engine.generateRandomLetter();
        System.out.println("🎲 Lettre: " + lettre + "\n");

        // Test 2: Load Categories
        System.out.println("=== Test 2: Catégories (DAO + Streams) ===");
        List<Categorie> categories = catDAO.findAllActif();
        List<String> catNames = catDAO.getActiveCategorieNames();
        System.out.println("📁 Catégories actives: " + catNames + "\n");

        // Test 3: Choose language
        System.out.println("=== Test 3: Choix Langue ===");
        System.out.println("1. Français  2. English  3. العربية");
        System.out.print("Choix: ");
        int langChoice = scanner.nextInt();
        scanner.nextLine();

        Langue langue = switch(langChoice) {
            case 1 -> Langue.FRANCAIS;
            case 2 -> Langue.ENGLISH;
            case 3 -> Langue.ARABE;
            default -> Langue.FRANCAIS;
        };
        System.out.println("✓ Langue: " + langue + "\n");

        // Test 4: Enter words for 2 categories
        Map<Categorie, String> reponses = new HashMap<>();
        System.out.println("=== Test 4: Saisie Mots ===");

        for (int i = 0; i < Math.min(2, categories.size()); i++) {
            Categorie cat = categories.get(i);
            resultsManager.recordSubmissionTime(cat);

            System.out.print(cat.getNom() + " (lettre " + lettre + "): ");
            String mot = scanner.nextLine().trim();
            reponses.put(cat, mot);

            try { Thread.sleep(2000); } catch (Exception e) {}
        }

        // Test 5: Validation (AI + Threads)
        System.out.println("\n=== Test 5: Validation AI (Threads) ===");
        System.out.println("⏳ Validation en cours...\n");

        resultsManager.validerMots(reponses, lettre, langue);

        // Test 6: Results with Streams
        System.out.println("=== Test 6: Résultats (Streams) ===");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        resultsManager.getResultats().forEach(r -> {
            String icon = r.isValide() ? "✓" : "✗";
            System.out.printf("%s %-15s | %-15s | %3d pts | %s\n",
                    icon, r.getCategorie(), r.getMot(), r.getPoints(), r.getMessage());
        });

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🏆 SCORE TOTAL: " + resultsManager.getScoreTotalStreams() + " points");
        System.out.println("✓ Mots valides: " + resultsManager.countValid() + "/" + reponses.size());

        // Test 7: Stream operations
        System.out.println("\n=== Test 7: Analyses Streams ===");
        var validResults = resultsManager.getValidResults();
        System.out.println("Résultats valides: " + validResults.size());

        var scoresByCategory = resultsManager.getScoresByCategory();
        System.out.println("Scores par catégorie: " + scoresByCategory);

        System.out.println("\n✅ Tests terminés!");
        scanner.close();
    }
}