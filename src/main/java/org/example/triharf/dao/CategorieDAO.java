package org.example.triharf.dao;

import org.example.triharf.config.HibernateUtil;
import org.example.triharf.models.Categorie;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.List;

public class CategorieDAO extends GenericDAO<Categorie> {

    public CategorieDAO() {
        super(Categorie.class);
    }

    public Categorie findByNom(String nom) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Categorie> query = session.createQuery(
                    "FROM Categorie WHERE nom = :nom", Categorie.class);
            query.setParameter("nom", nom);
            return query.uniqueResult();
        }
    }

    public List<Categorie> findAllActif() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Categorie> query = session.createQuery(
                    "FROM Categorie WHERE actif = true", Categorie.class);
            return query.getResultList();
        }
    }
}