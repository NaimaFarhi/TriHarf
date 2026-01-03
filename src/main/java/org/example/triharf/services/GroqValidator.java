package org.example.triharf.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.triharf.enums.Langue;
import org.example.triharf.utils.PropertiesManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GroqValidator {
    private static final String API_URL = PropertiesManager.getProperty("groq.api.url");
    private static final String API_KEY = PropertiesManager.getProperty("groq.api.key", "");
    private static final String MODEL = PropertiesManager.getProperty("groq.model", "llama-3.3-70b-versatile");

    /**
     * Validate word using Groq API
     */
    public ValidationResponse validateWord(String word, String categoryName,
                                           Character letter, Langue langue) {
        if (API_KEY.isEmpty()) {
            return new ValidationResponse(false, "API key not configured", 0);
        }

        try {
            String prompt = buildPrompt(word, categoryName, letter, langue);
            String response = callGroqAPI(prompt);
            return parseResponse(response);
        } catch (Exception e) {
            System.err.println("Groq error: " + e.getMessage());
            return new ValidationResponse(false, "AI validation failed", 5);
        }
    }

    /**
     * Build validation prompt
     */
    private String buildPrompt(String word, String categoryName,
                               Character letter, Langue langue) {
        String langName = switch(langue) {
            case FRANCAIS -> "French";
            case ANGLAIS -> "English";
            case ARABE -> "Arabic";
        };

        return String.format(
                "Validate strictly: Is '%s' a real, commonly-known %s in %s that starts with '%s'? " +
                        "Rules: " +
                        "- Must be a legitimate word in %s language " +
                        "- Must fit the category exactly (e.g., 'Dog' is Animal, not Person) " +
                        "- Must start with letter '%s' " +
                        "- No slang, abbreviations, or proper nouns unless category requires it " +
                        "- Rarity: 1=extremely common (cat, dog), 10=very rare/technical " +
                        "Respond ONLY: {\"valid\":true/false,\"rarity\":1-10}",
                word, categoryName, langName, letter, langName, letter
        );
    }

    /**
     * Call Groq API
     */
    private String callGroqAPI(String prompt) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);

        // Build request body (OpenAI-compatible format)
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.1);
        requestBody.addProperty("max_tokens", 50);

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        // Check response code
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("API returned: " + responseCode);
        }

        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    /**
     * Parse Groq response
     */
    private ValidationResponse parseResponse(String apiResponse) {
        try {
            JsonObject root = JsonParser.parseString(apiResponse).getAsJsonObject();

            // Extract message content
            String content = root.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            // Clean and parse JSON
            content = content.replaceAll("```json|```", "").trim();
            JsonObject result = JsonParser.parseString(content).getAsJsonObject();

            boolean valid = result.get("valid").getAsBoolean();
            int rarity = result.get("rarity").getAsInt();

            return new ValidationResponse(valid, valid ? "Valid (Groq)" : "Invalid", rarity);
        } catch (Exception e) {
            System.err.println("Parse error: " + e.getMessage());
            return new ValidationResponse(false, "Parse error", 5);
        }
    }

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