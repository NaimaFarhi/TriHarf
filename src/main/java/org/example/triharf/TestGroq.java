package org.example.triharf;

import org.example.triharf.enums.Langue;
import org.example.triharf.services.GroqValidator;

public class TestGroq {
    public static void main(String[] args) {
        System.out.println("Testing Groq AI...\n");

        GroqValidator validator = new GroqValidator();

        String[][] tests = {
                {"Cow", "Animal", "C"},
                {"Marseille", "Ville", "M"},
                {"XYZ", "Animal", "X"},
                {"Chevalier", "Animal", "C"}
        };

        for (String[] test : tests) {

            var result = validator.validateWord(
                    test[0], test[1], test[2].charAt(0), Langue.ENGLISH
            );

            System.out.printf("%-10s | %-10s | %s | Rarity: %d\n",
                    test[0], test[1],
                    result.isValid() ? "✓" : "✗",
                    result.getRarityScore()
            );
        }
    }
}