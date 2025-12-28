package org.example.triharf.services;

import org.example.triharf.dao.CategorieDAO;
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

    public GameSession(String pseudoJoueur) {
        this.gameEngine = new GameEngine();
        this.partieService = new PartieService();
        this.categorieDAO = new CategorieDAO();
        this.joueur = partieService.getOrCreateJoueur(pseudoJoueur);
        this.reponses = new HashMap<>();
    }

    public void demarrerPartie() {
        // Charger catégories actives
        categories = categorieDAO.findAllActif();

        // Générer lettre
        lettre = gameEngine.generateRandomLetter();

        // Créer partie en DB
        partie = partieService.creerPartie(joueur, lettre, "SOLO");

        // Démarrer timer
        int duration = PropertiesManager.getInt("game.timer.default", 180);
        resultsManager = new ResultsManager(duration);
        gameEngine.startTimer(duration);
    }

    public void terminerPartie() {
        gameEngine.stopTimer();

        // Valider tous les mots
        resultsManager.validerMots(reponses, lettre);

        // Sauvegarder résultats
        partieService.terminerPartie(
                partie,
                resultsManager.getScoreTotal(),
                (int) resultsManager.getDureePartie()
        );
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