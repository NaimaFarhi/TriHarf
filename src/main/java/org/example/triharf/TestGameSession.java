package org.example.triharf;

import org.example.triharf.enums.Langue;
import org.example.triharf.models.Categorie;
import org.example.triharf.services.GameSession;
import org.example.triharf.utils.SetupDatabase;

import java.util.Scanner;

public class TestGameSession {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SetupDatabase.insertDefaultCategories();

        System.out.println("=== Test GameSession Complet ===\n");

        // Créer session
        System.out.print("Pseudo: ");
        String pseudo = scanner.nextLine();

        System.out.println("Langue: 1=FR, 2=EN, 3=AR");
        int langChoice = scanner.nextInt();
        scanner.nextLine();

        Langue langue = switch(langChoice) {
            case 2 -> Langue.ANGLAIS;
            case 3 -> Langue.ARABE;
            default -> Langue.FRANCAIS;
        };

        GameSession session = new GameSession(pseudo, langue);
        session.demarrerPartie();

        System.out.println("\n🎲 Lettre: " + session.getLettre());
        System.out.println("⏱️  Temps: 180s\n");

        // Saisir 2 mots
        for (int i = 0; i < Math.min(2, session.getCategories().size()); i++) {
            Categorie cat = session.getCategories().get(i);
            System.out.print(cat.getNom() + ": ");
            String mot = scanner.nextLine();
            session.setReponse(cat, mot);
        }

        // Terminer
        System.out.println("\n⏳ Validation...");
        session.terminerPartie();

        // Résultats
        System.out.println("\n=== Résultats ===");
        session.getResultsManager().getResultats().forEach(r ->
                System.out.printf("%s %-15s | %3d pts\n",
                        r.isValide() ? "✓" : "✗", r.getMot(), r.getPoints())
        );

        System.out.println("\n🏆 Score: " + session.getResultsManager().getScoreTotal());
        System.out.println("👤 Joueur: " + session.getJoueur().getPseudo() +
                " (Total: " + session.getJoueur().getScoreTotal() + " pts)");

        scanner.close();
    }
}