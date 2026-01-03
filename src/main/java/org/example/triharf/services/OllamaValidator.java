package org.example.triharf.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.triharf.enums.Langue;
import org.example.triharf.utils.PropertiesManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OllamaValidator {
    // Ollama API endpoint (runs locally on Docker)
    private static final String API_URL = PropertiesManager.getProperty(
            "ollama.api.url", "http://localhost:11434/api/generate");

    // Model name (small, fast model for word validation)
    private static final String MODEL = PropertiesManager.getProperty(
            "ollama.model", "llama3.2:1b");

    /**
     * Validate word using local Ollama LLM
     * @param word The word to validate
     * @param categoryName Category name (e.g., "Animal")
     * @param letter Required starting letter
     * @param langue Language of the word
     * @return ValidationResponse with valid status and rarity score
     */
    public ValidationResponse validateWord(String word, String categoryName,
                                           Character letter, Langue langue) {
        try {
            // Build prompt for AI
            String prompt = buildPrompt(word, categoryName, letter, langue);

            // Call Ollama API
            String response = callOllamaAPI(prompt);

            // Parse JSON response
            return parseResponse(response);
        } catch (Exception e) {
            System.err.println("Ollama error: " + e.getMessage());
            // Return default on error (medium rarity)
            return new ValidationResponse(false, "AI unavailable", 5);
        }
    }

    /**
     * Build prompt for LLM in structured format
     */
    private String buildPrompt(String word, String categoryName, Character letter, Langue langue) {
        String langInstruction = switch(langue) {
            case FRANCAIS -> "Réponds en français. Le mot doit être français.";
            case ENGLISH -> "Answer in English. The word must be English.";
            case ARABE -> "أجب بالعربية. يجب أن تكون الكلمة عربية.";
        };

        return String.format(
                "%s\nIs '%s' a valid %s starting with '%s'? " +
                        "Rate rarity 1-10. Reply ONLY: {\"valid\":true/false,\"rarity\":1-10}",
                langInstruction, word, categoryName, letter
        );
    }

    /**
     * Make HTTP POST request to Ollama
     */
    private String callOllamaAPI(String prompt) throws Exception {
        // Connect to local Ollama Docker container
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);

        // Build request body
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);      // Which model to use
        body.addProperty("prompt", prompt);    // Our question
        body.addProperty("stream", false);     // Get complete response at once

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Parse Ollama response and extract validity + rarity
     */
    private ValidationResponse parseResponse(String apiResponse) {
        try {
            // Ollama wraps response in {"response": "..."}
            JsonObject root = JsonParser.parseString(apiResponse).getAsJsonObject();
            String text = root.get("response").getAsString();

            // Clean markdown formatting if present
            text = text.replaceAll("```json|```", "").trim();

            // Parse the actual JSON from AI
            JsonObject result = JsonParser.parseString(text).getAsJsonObject();

            boolean valid = result.get("valid").getAsBoolean();
            int rarity = result.get("rarity").getAsInt();

            return new ValidationResponse(valid, valid ? "Valid (Ollama)" : "Invalid", rarity);
        } catch (Exception e) {
            System.err.println("Parse error: " + e.getMessage());
            // On parse error, assume medium rarity
            return new ValidationResponse(false, "Parse error", 5);
        }
    }

    /**
     * Response container
     */
    public static class ValidationResponse {
        private boolean valid;
        private String message;
        private int rarityScore;

        public ValidationResponse(boolean valid, String message, int rarityScore) {
            this.valid = valid;
            this.message = message;
            this.rarityScore = rarityScore;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public int getRarityScore() { return rarityScore; }
    }
}