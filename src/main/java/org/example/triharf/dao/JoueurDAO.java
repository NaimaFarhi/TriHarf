package org.example.triharf.dao;

import org.example.triharf.config.HibernateUtil;
import org.example.triharf.models.Joueur;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class JoueurDAO extends GenericDAO<Joueur> {

    public JoueurDAO() {
        super(Joueur.class);
    }

    public Joueur findByPseudo(String pseudo) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Joueur> query = session.createQuery(
                    "FROM Joueur WHERE pseudo = :pseudo", Joueur.class);
            query.setParameter("pseudo", pseudo);
            return query.uniqueResult();
        }
    }
}