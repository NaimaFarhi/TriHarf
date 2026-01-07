package org.example.triharf.services;

import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.enums.Langue;
import org.example.triharf.models.Categorie;
import org.example.triharf.models.Joueur;
import org.example.triharf.models.Partie;
import org.example.triharf.network.GameClient;
import org.example.triharf.network.NetworkMessage;
import org.example.triharf.utils.PropertiesManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameSession {
    private GameEngine gameEngine;
    private ResultsManager resultsManager;
    private PartieService partieService;
    private CategorieDAO categorieDAO;

    private Joueur joueur;
    private Partie partie;
    private List<Categorie> categories;
    private Map<Categorie, String> reponses;
    private Character lettre;
    private Langue langue;

    // Multiplayer support
    private GameClient client;
    private String roomId;
    private boolean isMultiplayer;

    private EventManager eventManager;
    private EventManager.Event activeEvent;

    // Solo mode constructor
    public GameSession(String pseudoJoueur, Langue langue) {
        this(pseudoJoueur, langue, null, null);
    }

    // Multiplayer mode constructor
    public GameSession(String pseudoJoueur, Langue langue, GameClient client, String roomId) {
        this.gameEngine = new GameEngine();
        this.partieService = new PartieService();
        this.categorieDAO = new CategorieDAO();
        this.joueur = partieService.getOrCreateJoueur(pseudoJoueur);
        this.reponses = new HashMap<>();
        this.langue = langue;
        this.client = client;
        this.roomId = roomId;
        this.eventManager = new EventManager();
        this.isMultiplayer = (client != null);

        if (isMultiplayer) {
            setupMultiplayerHandlers();
        }
    }

    private void setupMultiplayerHandlers() {
        client.setMessageHandler(message -> {
            switch (message.getType()) {
                case GAME_START -> handleGameStart(message);
                case GAME_END -> handleGameEnd(message);
                case PLAYER_JOINED -> System.out.println("Player joined: " + message.getSenderId());
            }
        });
    }

    private void handleGameStart(NetworkMessage message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) message.getData();
        this.lettre = ((String) data.get("letter")).charAt(0);
        // UI will be notified via callback
    }

    private void handleGameEnd(NetworkMessage message) {
        terminerPartie();
    }

    public void demarrerPartie() {
        int duration = PropertiesManager.getInt("game.timer.default", 180);
        demarrerPartie(duration);

        // Trigger random event (30% chance)
        activeEvent = eventManager.triggerRandomEvent(0.3);
        if (activeEvent != null) {
            eventManager.applyEvent(activeEvent, gameEngine, resultsManager);
            System.out.println(eventManager.getEventDescription(activeEvent));

            // Broadcast event in multiplayer
            if (isMultiplayer) {
                NetworkMessage msg = new NetworkMessage(
                        NetworkMessage.Type.GAME_START,
                        joueur.getPseudo(),
                        Map.of("event", activeEvent.getType().name())
                );
                client.sendMessage(msg);
            }
        }
    }

    public void demarrerPartie(int durationSeconds) {
        categories = categorieDAO.findAllActif();

        if (!isMultiplayer) {
            lettre = gameEngine.generateRandomLetter();
            partie = partieService.creerPartie(joueur, lettre, "SOLO", langue);
        } else {
            partie = partieService.creerPartie(joueur, lettre, "MULTI", langue);
        }

        resultsManager = new ResultsManager(durationSeconds);
        gameEngine.startTimer(durationSeconds);
    }

    public void terminerPartie() {
        gameEngine.stopTimer();
        resultsManager.validerMots(reponses, lettre, langue);

        if (partie != null) {
            partieService.terminerPartie(
                    partie,
                    resultsManager.getScoreTotal(),
                    (int) resultsManager.getDureePartie(),
                    resultsManager.getResultats()
            );
        }

        if (isMultiplayer) {
            // Send final answers to server
            NetworkMessage msg = new NetworkMessage(
                    NetworkMessage.Type.SUBMIT_ANSWER,
                    joueur.getPseudo(),
                    reponses
            );
            client.sendMessage(msg);
        }
    }

    public void setReponse(Categorie categorie, String mot) {
        reponses.put(categorie, mot);
    }

    // Getters
    public GameEngine getGameEngine() { return gameEngine; }
    public ResultsManager getResultsManager() { return resultsManager; }
    public List<Categorie> getCategories() { return categories; }
    public Character getLettre() { return lettre; }
    public Joueur getJoueur() { return joueur; }
    public boolean isMultiplayer() { return isMultiplayer; }
    public EventManager.Event getActiveEvent() { return activeEvent; }
}