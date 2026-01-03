package org.example.triharf.dao;

import org.example.triharf.config.HibernateUtil;
import org.example.triharf.models.ResultatPartie;
import org.hibernate.Session;

import java.util.List;

public class ResultatPartieDAO extends GenericDAO<ResultatPartie> {

    public ResultatPartieDAO() {
        super(ResultatPartie.class);
    }

    public void saveAll(List<ResultatPartie> resultats) {
        if (resultats == null || resultats.isEmpty()) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            for (ResultatPartie res : resultats) {
                session.persist(res);
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
