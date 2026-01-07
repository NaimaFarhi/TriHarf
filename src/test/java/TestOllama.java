package org.example.triharf;

import org.example.triharf.enums.Langue;
import org.example.triharf.services.OllamaValidator;

public class TestOllama {
    public static void main(String[] args) {
        System.out.println("Testing Ollama local AI...\n");

        OllamaValidator validator = new OllamaValidator();

        // Test cases
        String[][] tests = {
                {"mouton", "Animal", "m"},
                {"Tom hanks", "Celebrité", "T"},
                {"marseille", "Ville", "M"},
                {"XYZ", "Animal", "X"}
        };

        for (String[] test : tests) {
            var result = validator.validateWord(
                    test[0], test[1], test[2].charAt(0), Langue.FRANCAIS
            );

            System.out.printf("%-10s | %-10s | %s | Rarity: %d\n",
                    test[0], test[1],
                    result.isValid() ? "✓" : "✗",
                    result.getRarityScore()
            );
        }
    }
}