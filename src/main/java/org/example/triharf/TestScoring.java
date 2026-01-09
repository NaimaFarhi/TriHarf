package org.example.triharf;

import org.example.triharf.services.ScoreCalculator;

public class TestScoring {
    public static void main(String[] args) {
        ScoreCalculator calc = new ScoreCalculator();

        // Test cases
        System.out.println("=== Scoring Tests ===");

        // Case 1: Short word "A" - should get 0
        int score1 = calc.calculateTotalScore("A", 10, 180, 5);
        System.out.println("Word 'A' (1 char): " + score1 + " pts (expected: 0)");

        // Case 2: Short word "AB" - should get 0
        int score2 = calc.calculateTotalScore("AB", 10, 180, 5);
        System.out.println("Word 'AB' (2 chars): " + score2 + " pts (expected: 0)");

        // Case 3: Minimum valid "Cat" - fast, common
        int score3 = calc.calculateTotalScore("Cat", 30, 180, 3);
        System.out.println("Word 'Cat' (fast, common): " + score3 + " pts");

        // Case 4: Long word "Crocodile" - medium speed, rare
        int score4 = calc.calculateTotalScore("Crocodile", 90, 180, 8);
        System.out.println("Word 'Crocodile' (long, rare): " + score4 + " pts");

        // Case 5: Very fast submission
        int score5 = calc.calculateTotalScore("Apple", 20, 180, 5);
        System.out.println("Word 'Apple' (very fast): " + score5 + " pts");

        // Case 6: Very slow submission
        int score6 = calc.calculateTotalScore("Apple", 160, 180, 5);
        System.out.println("Word 'Apple' (very slow): " + score6 + " pts");

        System.out.println("\n✅ Scoring tests complete!");
    }
}