package org.example.triharf.services;

import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.enums.Langue;
import org.example.triharf.models.Categorie;
import org.example.triharf.models.Joueur;
import org.example.triharf.models.Partie;
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

    public GameSession(String pseudoJoueur, Langue langue) {
        this.gameEngine = new GameEngine();
        this.partieService = new PartieService();
        this.categorieDAO = new CategorieDAO();
        this.joueur = partieService.getOrCreateJoueur(pseudoJoueur);
        this.reponses = new HashMap<>();
        this.langue = langue;
    }

    public void demarrerPartie() {
        int duration = PropertiesManager.getInt("game.timer.default", 180);
        demarrerPartie(duration);
    }

    public void demarrerPartie(int durationSeconds) {
        // Charger catégories actives
        categories = categorieDAO.findAllActif();

        // Générer lettre
        lettre = gameEngine.generateRandomLetter();

        // Créer partie en DB
        partie = partieService.creerPartie(joueur, lettre, "SOLO", langue);

        // Démarrer timer avec la durée spécifiée
        resultsManager = new ResultsManager(durationSeconds);
        gameEngine.startTimer(durationSeconds);
    }

    public void terminerPartie() {
        gameEngine.stopTimer();

        // Valider tous les mots
        resultsManager.validerMots(reponses, lettre, langue);

        // Sauvegarder résultats si la partie a été bien créée
        if (partie != null) {
            partieService.terminerPartie(
                    partie,
                    resultsManager.getScoreTotal(),
                    (int) resultsManager.getDureePartie()
            );
        } else {
            System.err.println("❌ ERREUR: Impossible de sauvegarder la partie (partie est null)");
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

}