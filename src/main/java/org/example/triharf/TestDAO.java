package org.example.triharf;

import org.example.triharf.dao.*;
import org.example.triharf.models.*;
import org.example.triharf.services.ValidationService;

public class TestDAO {
    public static void main(String[] args) {
        CategorieDAO catDAO = new CategorieDAO();
        ValidationService validator = new ValidationService();

        // Test Categorie
        Categorie cat = new Categorie("Animal");
        catDAO.save(cat);
        System.out.println("✅ Catégorie sauvegardée: " + cat.getId());

        // Test Validation
        var result = validator.validateMot("Apple", cat, 'A');
        System.out.println("✅ Validation: " + result.getMessage());

        System.out.println("✅ Sprint 1 - DAOs fonctionnels!");
    }
}