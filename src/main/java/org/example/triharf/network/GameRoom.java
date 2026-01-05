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

    public GameRoom(String roomId, int maxPlayers, Langue langue) {
        this.roomId = roomId;
        this.maxPlayers = maxPlayers;
        this.langue = langue;
        this.playerIds = new ArrayList<>();
        this.playerAnswers = new HashMap<>();
        this.gameStarted = false;
        this.readyPlayers = new HashSet<>();
    }

    public String getRoomId() {
        return roomId;
    }
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public List<String> getPlayerIds() {
        return playerIds;
    }
    public void setPlayerIds(List<String> playerIds) {
        this.playerIds = playerIds;
    }

    public Map<String, Map<Categorie, String>> getPlayerAnswers() {
        return playerAnswers;
    }
    public void setPlayerAnswers(Map<String, Map<Categorie, String>> playerAnswers) {
        this.playerAnswers = playerAnswers;
    }

    public Character getCurrentLetter() {
        return currentLetter;
    }
    public void setCurrentLetter(Character currentLetter) {
        this.currentLetter = currentLetter;
    }

    public Langue getLangue() {
        return langue;
    }
    public void setLangue(Langue langue) {
        this.langue = langue;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }
    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public boolean addPlayer(String playerId){
        if (!playerIds.contains(playerId) && !isFull()) {
            playerIds.add(playerId);
            playerAnswers.put(playerId, new HashMap<>());
            return true;
        }
        return false;
    }

    public void removePlayer(String playerId){
        this.playerIds.remove(playerId);
        this.readyPlayers.remove(playerId);
    }

    public void setPlayerReady(String playerId, boolean ready) {
        if (ready) {
            readyPlayers.add(playerId);
        } else {
            readyPlayers.remove(playerId);
        }
    }

    public Set<String> getReadyPlayers() {
        return readyPlayers;
    }

    public void submitAnswer(String playerId, Categorie cat, String word){
        playerAnswers.putIfAbsent(playerId, new HashMap<>());
        playerAnswers.get(playerId).put(cat, word);
    }

    public Boolean isFull(){
        return this.playerIds.size() == maxPlayers;
    }

    public Boolean canStart() {
        return playerIds.size() >= 2;
    }

    // Add methods:
    // - addPlayer(String playerId)
    // - removePlayer(String playerId)
    // - submitAnswer(String playerId, Categorie cat, String word)
    // - isFull()
    // - canStart() // returns true if enough players
}