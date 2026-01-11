package org.example.triharf.services;

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
import java.util.concurrent.CompletableFuture;

public class GeminiValidator {
    
    private static final String API_URL = PropertiesManager.getProperty("gemini.api.url");
    private static final String API_KEY = PropertiesManager.getProperty("gemini.api.key", "");
    private static final String LANGUAGE = PropertiesManager.getProperty("gemini.language", "en");
    private static final int TIMEOUT = 10000;

    public ValidationResponse validateWord(String word, String categoryName, Character letter, Langue langue) {
        if (API_KEY.isEmpty()) {
            return new ValidationResponse(false, "API key not configured", 0);
        }

        try {
            String prompt = buildPrompt(word, categoryName, letter, langue);
            String response = callGeminiAPI(prompt);
            return parseResponse(response, word);
        } catch (Exception e) {
            System.err.println("Gemini validation error: " + e.getMessage());
            return new ValidationResponse(false, "AI validation failed", 0);
        }
    }

    private String buildPrompt(String word, String categoryName, Character letter, Langue language) {
        String langName = switch(language) {
            case FRANCAIS -> "French";
            case ANGLAIS -> "English";
            case ARABE -> "Arabic";
        };

        return String.format(
                "Validate if '%s' is a valid %s starting with letter '%s'. " +
                        "Language: %s. " +
                        "Rate its rarity from 1-10 (1=very common, 10=very rare). " +
                        "Respond ONLY in this exact JSON format: {\"valid\": true/false, \"rarity\": 1-10}",
                word, categoryName, letter, langName
        );
    }

    private String callGeminiAPI(String prompt) throws Exception {
        int retries = 3;
        int delay = 2000; // 2 seconds

        for (int i = 0; i < retries; i++) {
            try {
            URL url = new URL(API_URL + "?key=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setDoOutput(true);

            // Build request body
            JsonObject requestBody = new JsonObject();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);

            JsonObject content = new JsonObject();
            content.add("parts", com.google.gson.JsonParser.parseString("[" + part + "]"));

            requestBody.add("contents", com.google.gson.JsonParser.parseString("[" + content + "]"));

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = conn.getResponseCode();
                    if (responseCode == 429) {
                        if (i < retries - 1) {
                            System.out.println("Rate limit hit, waiting " + delay + "ms...");
                            Thread.sleep(delay);
                            delay *= 2; // Exponential backoff
                            continue;
                        }
                    }
            if (responseCode != 200) {
                throw new RuntimeException("API returned: " + responseCode);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            return response.toString();
        } catch (Exception e) {
            if (i == retries - 1) throw e;
        }
        }
        throw new RuntimeException("Max retries exceeded");
    }

    private ValidationResponse parseResponse(String apiResponse, String word) {
        try {
            JsonObject root = JsonParser.parseString(apiResponse).getAsJsonObject();
            String text = root.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            // Extract JSON from response
            text = text.replaceAll("```json|```", "").trim();
            JsonObject result = JsonParser.parseString(text).getAsJsonObject();

            boolean valid = result.get("valid").getAsBoolean();
            int rarity = result.get("rarity").getAsInt();

            return new ValidationResponse(valid, valid ? "Valid by AI" : "Invalid word", rarity);
        } catch (Exception e) {
            System.err.println("Parse error: " + e.getMessage());
            return new ValidationResponse(false, "Parse error", 0);
        }
    }

    public CompletableFuture<ValidationResponse> validateWordAsync(String word, String categoryName, Character letter, Langue langue) {
        return CompletableFuture.supplyAsync(() -> validateWord(word, categoryName, letter, langue));
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