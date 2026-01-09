package org.example.triharf;

import org.example.triharf.services.GameEngine;
import org.example.triharf.services.ScoreCalculator;
import org.example.triharf.utils.PropertiesManager;

public class TestGameEngine {
    public static void main(String[] args) {
        // Test PropertiesManager
        String apiUrl = PropertiesManager.getProperty("api.dictionary.url");
        System.out.println("✅ API URL: " + apiUrl);

        // Test GameEngine
        GameEngine engine = new GameEngine();
        Character letter = engine.generateRandomLetter();
        System.out.println("✅ Lettre générée: " + letter);

        // Test ScoreCalculator
        ScoreCalculator calculator = new ScoreCalculator();
        int score = calculator.calculateTotalScore("Apple", 30, 180, 2);
        System.out.println("✅ Score calculé: " + score);

        System.out.println("✅ Sprint 1 [F] - Terminé!");
    }
}