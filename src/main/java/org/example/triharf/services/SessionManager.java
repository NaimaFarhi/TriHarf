package org.example.triharf.services;

public class SessionManager {

    private static GameSession gameSession;

    public static void setGameSession(GameSession session) {
        gameSession = session;
    }

    public static GameSession getGameSession() {
        return gameSession;
    }

    public static void clear() {
        gameSession = null;
    }
}
