package org.example.triharf.services;

import org.example.triharf.models.Joueur;
import java.util.*;
import java.util.stream.Collectors;

public class BattleRoyaleMode {
    private static final int MAX_PLAYERS = 10;
    private static final int MIN_PLAYERS = 3;
    private static final int ELIMINATION_COUNT = 1; // Eliminate 1 per round

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
        // TODO: Use streams to:
        // 1. Sort players by score (ascending)
        // 2. Take bottom ELIMINATION_COUNT players
        // 3. Remove from activePlayers
        // 4. Return eliminated player IDs
        return playerScores.entrySet().stream()
                .sorted(Comparator.reverseOrder())
                .limit(ELIMINATION_COUNT);
    }

    // Check if game should end
    public boolean isGameOver() {
        return activePlayers.size() <= MIN_PLAYERS;
    }

    // Get winner
    public String getWinner() {
        // TODO: Use streams to find player with highest score
    }

    // Get leaderboard
    public List<Map.Entry<String, Integer>> getLeaderboard() {
        // TODO: Use streams to sort by score descending
    }
}