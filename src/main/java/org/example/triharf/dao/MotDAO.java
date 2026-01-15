package org.example.triharf.dao;

import org.example.triharf.config.HibernateUtil;
import org.example.triharf.enums.Langue;
import org.example.triharf.models.Mot;
import org.example.triharf.models.Categorie;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.Map;
import java.util.stream.Collectors;

public class MotDAO extends GenericDAO<Mot> {

    public MotDAO() {
        super(Mot.class);
    }

    public boolean exists(String texte, Categorie categorie, Character lettre, Langue langue) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(m) FROM Mot m WHERE LOWER(m.texte) = LOWER(:texte) " +
                            "AND m.categorie.id = :catId AND LOWER(m.lettre) = LOWER(:lettre) " +
                            "AND m.langue = :langue", Long.class);
            query.setParameter("texte", texte);
            query.setParameter("catId", categorie.getId());
            query.setParameter("lettre", lettre);
            query.setParameter("langue", langue); // Pass enum directly, remove LOWER()
            return query.uniqueResult() > 0;
        }
    }

    public Mot findByTexteAndCategorie(String texte, Categorie categorie, Langue langue) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Mot> query = session.createQuery(
                    "FROM Mot WHERE LOWER(texte) = LOWER(:texte) AND categorie.id = :catId " +
                            "AND langue = :langue", Mot.class);
            query.setParameter("texte", texte);
            query.setParameter("catId", categorie.getId());
            query.setParameter("langue", langue); // Pass enum directly
            return query.uniqueResult();
        }
    }

    public void incrementUtilisation(Mot mot) {
        mot.setNbUtilisations(mot.getNbUtilisations() + 1);
        update(mot);
    }

    public Map<Character, Long> countMotsByLetter() {
        return findAll().stream()
                .collect(Collectors.groupingBy(Mot::getLettre, Collectors.counting()));
    }

    public void deleteAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.createMutationQuery("DELETE FROM Mot").executeUpdate();
            session.getTransaction().commit();
            System.out.println("✅ Base de données mots nettoyée !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}