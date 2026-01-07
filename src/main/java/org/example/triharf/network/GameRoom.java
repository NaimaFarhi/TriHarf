package org.example.triharf.network;

import org.example.triharf.enums.Langue;
import org.example.triharf.models.Categorie;
import java.util.*;

public class GameRoom {
    private String roomId;
    private List<String> playerIds;
    private Map<String, Map<Categorie, String>> playerAnswers;
    private Character currentLetter;
    private Langue langue;
    private int maxPlayers;
    private boolean gameStarted;
    private Set<String> readyPlayers;
    private Map<String, String> playerPseudos;

    public GameRoom(String roomId, int maxPlayers, Langue langue) {
        this.roomId = roomId;
        this.maxPlayers = maxPlayers;
        this.langue = langue;
        this.playerIds = new ArrayList<>();
        this.playerAnswers = new HashMap<>();
        this.gameStarted = false;
        this.readyPlayers = new HashSet<>();
        this.playerPseudos = new HashMap<>();
    }

    public boolean addPlayer(String playerId, String pseudo) {
        if (!playerIds.contains(playerId) && !isFull()) {
            playerIds.add(playerId);
            playerPseudos.put(playerId, pseudo);
            playerAnswers.put(playerId, new HashMap<>());
            return true;
        }
        return false;
    }

    public void removePlayer(String playerId) {
        playerIds.remove(playerId);
        playerPseudos.remove(playerId);
        readyPlayers.remove(playerId);
    }

    public void setPlayerReady(String playerId, boolean ready) {
        if (ready) {
            readyPlayers.add(playerId);
        } else {
            readyPlayers.remove(playerId);
        }
    }

    public boolean allPlayersReady() {
        return !playerIds.isEmpty() && readyPlayers.size() == playerIds.size();
    }

    public void submitAnswer(String playerId, Categorie cat, String word) {
        playerAnswers.putIfAbsent(playerId, new HashMap<>());
        playerAnswers.get(playerId).put(cat, word);
    }

    public boolean isFull() {
        return playerIds.size() == maxPlayers;
    }

    public boolean canStart() {
        return playerIds.size() >= 2;
    }

    // Getters/Setters
    public String getRoomId() { return roomId; }
    public List<String> getPlayerIds() { return playerIds; }
    public Map<String, Map<Categorie, String>> getPlayerAnswers() { return playerAnswers; }
    public Character getCurrentLetter() { return currentLetter; }
    public void setCurrentLetter(Character currentLetter) { this.currentLetter = currentLetter; }
    public Langue getLangue() { return langue; }
    public int getMaxPlayers() { return maxPlayers; }
    public boolean isGameStarted() { return gameStarted; }
    public void setGameStarted(boolean gameStarted) { this.gameStarted = gameStarted; }
    public Set<String> getReadyPlayers() { return readyPlayers; }
    public String getPseudo(String playerId) { return playerPseudos.getOrDefault(playerId, playerId); }
}