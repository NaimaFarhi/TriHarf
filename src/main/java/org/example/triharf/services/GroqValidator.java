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
     * Build validation prompt for solo mode
     */
    private String buildPrompt(String word, String categoryName,
                               Character letter, Langue langue) {
        String langName = getLanguageName(langue);

        return String.format(
                "Validate strictly: Is '%s' a real, commonly-known %s in %s that starts with '%s'? " +
                        "Rules: " +
                        "- Must be a legitimate word in %s language " +
                        "- Must fit the category exactly " +
                        "- Must start with letter '%s' " +
                        "- PROHIBIT foreign names (e.g. 'España' in French is INVALID, must be 'Espagne') " +
                        "- PROHIBIT English spelling if target language is different (e.g. reject 'Zebra' in French, want 'Zèbre') " +
                        "- Proper nouns (Cities, Countries, Celebrities, Full Names, Brands) ARE ALLOWED if category implies it " +
                        "- Multi-word answers with spaces ARE ALLOWED (e.g. 'New York', 'Will Smith') " +
                        "- Rarity: 1=extremely common, 10=very rare " +
                        "Respond ONLY: {\"valid\":true/false,\"rarity\":1-10}",
                word, categoryName, langName, letter, langName, letter
        );
    }

    private String getLanguageName(Langue langue) {
        return switch(langue) {
            case FRANCAIS -> "French";
            case ANGLAIS -> "English";
            case ARABE -> "Arabic";
        };
    }

    /**
     * Validate word for multiplayer mode using enhanced prompt
     */
    public MultiplayerValidationResponse validateWordMultiplayer(String word, String categoryName,
                                                                  Character letter, Langue langue,
                                                                  String playerName) {
        if (API_KEY.isEmpty()) {
            return new MultiplayerValidationResponse(playerName, false, 0, 0);
        }

        try {
            String prompt = buildMultiplayerPrompt(word, categoryName, letter, langue, playerName);
            String response = callGroqAPI(prompt);
            return parseMultiplayerResponse(response, playerName);
        } catch (Exception e) {
            System.err.println("Groq multiplayer error: " + e.getMessage());
            return new MultiplayerValidationResponse(playerName, false, 5, 0);
        }
    }

    /**
     * Build validation prompt for multiplayer mode
     */
    private String buildMultiplayerPrompt(String word, String categoryName,
                                          Character letter, Langue langue, String playerName) {
        String langName = getLanguageName(langue);

        return String.format(
            "Tu es un arbitre EXPERT, STRICT et INTRANSIGEANT du jeu Petit Bac (Baccalauréat) en MODE MULTIJOUEUR.\n\n" +
            "CONTEXTE DE LA MANCHE :\n" +
            "- Langue attendue : \"%s\"\n" +
            "- Lettre imposée : \"%s\"\n" +
            "- Catégorie : \"%s\"\n\n" +
            "PROPOSITION DU JOUEUR :\n" +
            "- Joueur : \"%s\"\n" +
            "- Mot proposé : \"%s\"\n\n" +
            "RÈGLES DE VALIDATION (OBLIGATOIRES) :\n" +
            "1. Le mot DOIT commencer exactement par la lettre \"%s\".\n" +
            "2. Le mot DOIT réellement exister dans la langue \"%s\" (aucun mot inventé, approximatif ou obsolète).\n" +
            "3. Le mot DOIT appartenir LOGIQUEMENT et CORRECTEMENT à la catégorie donnée.\n" +
            "4. Orthographe STRICTE dans la langue cible :\n" +
            "   - AUCUN mot étranger (ex : \"España\" invalide en français → \"Espagne\" requis).\n" +
            "   - AUCUNE orthographe anglaise si la langue cible est différente (ex : \"Zebra\" invalide en français → \"Zèbre\").\n" +
            "5. Noms propres AUTORISÉS UNIQUEMENT si la catégorie l'implique :\n" +
            "   - Ville : uniquement des villes réelles existantes.\n" +
            "   - Pays : uniquement des pays reconnus.\n" +
            "   - Prénom : uniquement des prénoms réellement utilisés.\n" +
            "   - Célébrité : uniquement des personnes réelles et connues.\n" +
            "6. Les réponses multi-mots avec espaces sont AUTORISÉES (ex : \"New York\", \"Will Smith\").\n" +
            "7. En cas de DOUTE, d'ERREUR ou d'INCOHÉRENCE → la réponse est INVALIDÉE.\n\n" +
            "RÈGLES MULTIJOUEUR & SCORE :\n" +
            "8. Si le mot est INVALIDÉ → score = 0.\n" +
            "9. Si le mot est VALIDE mais IDENTIQUE à celui d'au moins un autre joueur → score = 5.\n" +
            "10. Si le mot est VALIDE et UNIQUE → score = 10 + bonus de rareté.\n\n" +
            "ÉVALUATION DE RARETÉ :\n" +
            "- 1 = extrêmement courant\n" +
            "- 10 = très rare\n" +
            "- Bonus de rareté = valeur de rareté (1 à 10).\n\n" +
            "IMPORTANT :\n" +
            "- Tu évalues UNIQUEMENT cette proposition, sans tenir compte des scores finaux.\n" +
            "- Tu DOIS estimer la rareté même si le mot est invalide.\n\n" +
            "FORMAT DE RÉPONSE (STRICT, SANS TEXTE SUPPLÉMENTAIRE) :\n" +
            "{\n" +
            "  \"player\": \"%s\",\n" +
            "  \"valid\": true|false,\n" +
            "  \"rarity\": 1-10,\n" +
            "  \"score\": number\n" +
            "}",
            langName, letter, categoryName,
            playerName, word,
            letter, langName,
            playerName
        );
    }

    /**
     * Parse multiplayer response from Groq API
     */
    private MultiplayerValidationResponse parseMultiplayerResponse(String apiResponse, String fallbackPlayerName) {
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

            String player = result.has("player") ? result.get("player").getAsString() : fallbackPlayerName;
            boolean valid = result.get("valid").getAsBoolean();
            int rarity = result.get("rarity").getAsInt();
            int score = result.has("score") ? result.get("score").getAsInt() : 0;

            return new MultiplayerValidationResponse(player, valid, rarity, score);
        } catch (Exception e) {
            System.err.println("Parse multiplayer error: " + e.getMessage());
            return new MultiplayerValidationResponse(fallbackPlayerName, false, 5, 0);
        }
    }

    /**
     * Response class for multiplayer validation
     */
    public static class MultiplayerValidationResponse {
        private String player;
        private boolean valid;
        private int rarity;
        private int score;

        public MultiplayerValidationResponse(String player, boolean valid, int rarity, int score) {
            this.player = player;
            this.valid = valid;
            this.rarity = rarity;
            this.score = score;
        }

        public String getPlayer() { return player; }
        public boolean isValid() { return valid; }
        public int getRarity() { return rarity; }
        public int getScore() { return score; }
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
        requestBody.addProperty("max_tokens", 150);

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