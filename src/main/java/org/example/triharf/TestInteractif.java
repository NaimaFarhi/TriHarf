package org.example.triharf;

import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.enums.Langue;
import org.example.triharf.models.Categorie;
import org.example.triharf.services.ValidationService;
import org.example.triharf.services.ScoreCalculator;
import java.util.Random;
import java.util.Scanner;

public class TestInteractif {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        CategorieDAO catDAO = new CategorieDAO();
        ValidationService validator = new ValidationService();
        ScoreCalculator calculator = new ScoreCalculator();

        // Generate random letter
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        char lettre = letters.charAt(new Random().nextInt(letters.length()));
        System.out.flush(); // Force output
        System.out.println("DEBUG: Letter = " + lettre); // Add this

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║  Baccalauréat+ - Test AI Scoring ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println("\n🎲 Lettre tirée: " + lettre);

        // Get categories
        var categories = catDAO.findAllActif();
        if (categories.isEmpty()) {
            System.out.println("❌ Aucune catégorie! Lancez SetupDatabase d'abord.");
            return;
        }

        Categorie categorie = categories.get(0);
        System.out.println("📁 Catégorie: " + categorie.getNom());

        long startTime = System.currentTimeMillis();

        System.out.print("\n✏️  Votre mot: ");
        String mot = scanner.nextLine().trim();

        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;

        System.out.println("\n⏳ Validation en cours...");
        var result = validator.validateMot(mot, categorie, lettre, Langue.FRANCAIS);

        if (result.isValid()) {
            int score = calculator.calculateTotalScore(mot, elapsedSeconds, 180, result.getRarityScore());

            System.out.println("\n✅ MOT VALIDE!");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("📝 Mot: " + mot);
            System.out.println("⏱️  Temps: " + elapsedSeconds + "s");
            System.out.println("🎯 Longueur: " + mot.length() + " caractères");
            System.out.println("💎 Rareté AI: " + result.getRarityScore() + "/10");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🏆 SCORE TOTAL: " + score + " points");
        } else {
            System.out.println("\n❌ MOT INVALIDE!");
            System.out.println("Raison: " + result.getMessage());
            System.out.println("🏆 SCORE: 0 points");
        }

        scanner.close();
    }
}