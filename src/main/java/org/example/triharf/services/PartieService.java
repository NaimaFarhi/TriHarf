package org.example.triharf.services;

import org.example.triharf.dao.JoueurDAO;
import org.example.triharf.dao.PartieDAO;
import org.example.triharf.models.Joueur;
import org.example.triharf.models.Partie;

public class PartieService {
    private JoueurDAO joueurDAO = new JoueurDAO();
    private PartieDAO partieDAO = new PartieDAO();

    public Joueur getOrCreateJoueur(String pseudo) {
        Joueur joueur = joueurDAO.findByPseudo(pseudo);
        if (joueur == null) {
            joueur = new Joueur(pseudo);
            joueur = joueurDAO.save(joueur);
        }
        return joueur;
    }

    public Partie creerPartie(Joueur joueur, Character lettre, String mode) {
        Partie partie = new Partie(joueur, lettre, mode);
        return partieDAO.save(partie);
    }

    public void terminerPartie(Partie partie, int score, int dureeSeconde) {
        partie.setScore(score);
        partie.setDureeSeconde(dureeSeconde);
        partieDAO.update(partie);

        // Mettre à jour stats joueur
        Joueur joueur = partie.getJoueur();
        joueur.setScoreTotal(joueur.getScoreTotal() + score);
        joueur.setNbParties(joueur.getNbParties() + 1);
        joueurDAO.update(joueur);
    }
}