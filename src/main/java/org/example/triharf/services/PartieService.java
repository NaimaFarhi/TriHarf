package org.example.triharf.services;

import org.example.triharf.dao.JoueurDAO;
import org.example.triharf.dao.PartieDAO;
import org.example.triharf.enums.Langue;
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

    public Partie creerPartie(Joueur joueur, Character lettre, String mode, Langue langue) {
        Partie partie = new Partie(joueur, lettre, mode);
        partie.setLangue(langue);
        return partieDAO.save(partie);
    }

    private org.example.triharf.dao.ResultatPartieDAO resultatPartieDAO = new org.example.triharf.dao.ResultatPartieDAO();

    public void terminerPartie(Partie partie, int score, int dureeSeconde, java.util.List<org.example.triharf.models.ResultatPartie> resultats) {
        partie.setScore(score);
        partie.setDureeSeconde(dureeSeconde);
        partieDAO.update(partie);

        // Sauvegarder les détails (pour les stats par catégorie)
        if (resultats != null) {
            for (org.example.triharf.models.ResultatPartie res : resultats) {
                res.setPartie(partie); // Lier à la partie
                resultatPartieDAO.save(res);
            }
        }

        Joueur joueur = partie.getJoueur();
        joueur.setScoreTotal(joueur.getScoreTotal() + score);
        joueur.setNbParties(joueur.getNbParties() + 1);
        joueurDAO.update(joueur);
    }
}