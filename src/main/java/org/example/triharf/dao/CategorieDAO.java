package org.example.triharf.dao;

import org.example.triharf.config.HibernateUtil;
import org.example.triharf.enums.Langue;
import org.example.triharf.models.Categorie;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CategorieDAO extends GenericDAO<Categorie> {

    public CategorieDAO() {
        super(Categorie.class);
    }

    /**
     * Recherche une catégorie par son nom
     */
    public Categorie findByNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            return null;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Categorie> query = session.createQuery(
                    "FROM Categorie WHERE nom = :nom", Categorie.class);
            query.setParameter("nom", nom);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getCategorieNames() {
        return findAll().stream()
                .map(Categorie::getNom)
                .collect(Collectors.toList());
    }

    public List<String> getActiveCategorieNames() {
        return findAll().stream()
                .filter(Categorie::getActif)
                .map(Categorie::getNom)
                .collect(Collectors.toList());
    }


    /**
     * Récupère toutes les catégories actives
     */
    public List<Categorie> findAllActif() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Categorie> query = session.createQuery(
                    "FROM Categorie WHERE actif = true ORDER BY nom", Categorie.class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Récupère toutes les catégories par langue
     */
    public List<Categorie> findByLangue(String langue) {
        if (langue == null || langue.trim().isEmpty()) {
            return List.of();
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Categorie> query = session.createQuery(
                    "FROM Categorie WHERE langue = :langue ORDER BY nom", Categorie.class);
            query.setParameter("langue", langue);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Récupère les catégories actives par langue (accepts Langue enum)
     */
    public List<Categorie> findActifByLangue(Langue langue) {
        if (langue == null) {
            return List.of();
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Categorie> query = session.createQuery(
                    "FROM Categorie WHERE langue = :langue AND actif = true ORDER BY nom",
                    Categorie.class);
            query.setParameter("langue", langue); // Pass enum directly
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Compte les catégories actives
     */
    public long countActif() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(c) FROM Categorie c WHERE c.actif = true", Long.class);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Vérifie si une catégorie existe par nom
     */
    public boolean existsByNom(String nom) {
        return findByNom(nom) != null;
    }

    public List<Categorie> getAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Categorie> query = session.createQuery("FROM Categorie ORDER BY nom", Categorie.class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

}