package org.example.triharf.dao;

import org.example.triharf.config.HibernateUtil;
import org.example.triharf.models.Mot;
import org.example.triharf.models.Categorie;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class MotDAO extends GenericDAO<Mot> {

    public MotDAO() {
        super(Mot.class);
    }

    public boolean exists(String texte, Categorie categorie, Character lettre) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(m) FROM Mot m WHERE LOWER(m.texte) = LOWER(:texte) " +
                            "AND m.categorie.id = :catId AND LOWER(m.lettre) = LOWER(:lettre)", Long.class);
            query.setParameter("texte", texte);
            query.setParameter("catId", categorie.getId());
            query.setParameter("lettre", lettre);
            return query.uniqueResult() > 0;
        }
    }

    public Mot findByTexteAndCategorie(String texte, Categorie categorie) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Mot> query = session.createQuery(
                    "FROM Mot WHERE LOWER(texte) = LOWER(:texte) AND categorie.id = :catId", Mot.class);
            query.setParameter("texte", texte);
            query.setParameter("catId", categorie.getId());
            return query.uniqueResult();
        }
    }

    public void incrementUtilisation(Mot mot) {
        mot.setNbUtilisations(mot.getNbUtilisations() + 1);
        update(mot);
    }
}