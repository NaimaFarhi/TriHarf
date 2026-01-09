package org.example.triharf.services;

import org.example.triharf.models.Joueur;
import java.util.*;
import java.util.stream.Collectors;

public class BattleRoyaleMode {
    private static final int MAX_PLAYERS = 10;
    private static final int MIN_PLAYERS = 3;
    private static final int ELIMINATION_COUNT = 1;  //Eliminate 1 per round

    private List<String> activePlayers;
    private Map<String, Integer> playerScores;
    private int currentRound;

    public BattleRoyaleMode() {
        this.activePlayers = new ArrayList<>();
        this.playerScores = new HashMap<>();
        this.currentRound = 0;
    }

    // Add player to battle
    public boolean addPlayer(String playerId) {
        if (activePlayers.size() < MAX_PLAYERS) {
            activePlayers.add(playerId);
            playerScores.put(playerId, 0);
            return true;
        }
        return false;
    }

    // Update score for player
    public void updateScore(String playerId, int score) {
        playerScores.merge(playerId, score, Integer::sum);
    }

    // Eliminate lowest scoring players using streams
    public List<String> eliminatePlayers() {
        List<String> eliminated = playerScores.entrySet().stream()
                .filter(e -> activePlayers.contains(e.getKey()))
                .sorted(Map.Entry.comparingByValue()) // Ascending (lowest first)
                .limit(ELIMINATION_COUNT)
                .map(Map.Entry::getKey)
                .toList();

        activePlayers.removeAll(eliminated);
        currentRound++;
        return eliminated;
    }

    public String getWinner() {
        return playerScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public List<Map.Entry<String, Integer>> getLeaderboard() {
        return playerScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();
    }
}