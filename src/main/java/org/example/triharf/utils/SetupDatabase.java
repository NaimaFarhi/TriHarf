package org.example.triharf.utils;

import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.models.Categorie;

public class SetupDatabase {

    public static void insertDefaultCategories() {
        CategorieDAO dao = new CategorieDAO();

        String[] defaultCategories = {
                "Prénom", "Pays", "Ville", "Animal",
                "Métier", "Fruit/Légume", "Marque", "Célébrité"
        };

        for (String nom : defaultCategories) {
            if (dao.findByNom(nom) == null) {
                Categorie cat = new Categorie(nom);
                dao.save(cat);
            }
        }
    }
}