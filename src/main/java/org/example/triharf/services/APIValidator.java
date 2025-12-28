package org.example.triharf.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class APIValidator {
    private static final String API_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/";
    private static final int TIMEOUT = 5000;

    public boolean validateWord(String word) {
        try {
            URL url = new URL(API_URL + word.toLowerCase());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JsonArray jsonArray = JsonParser.parseString(response.toString()).getAsJsonArray();
                return jsonArray.size() > 0;
            }

            return false;
        } catch (Exception e) {
            System.err.println("API Validation error: " + e.getMessage());
            return false;
        }
    }
}