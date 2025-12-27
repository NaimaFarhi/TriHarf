package org.example.triharf.services;

import org.example.triharf.dao.CategorieDAO;
import org.example.triharf.models.Categorie;

public class SetupService {

    private static final CategorieDAO categorieDAO = new CategorieDAO();

    // Langues supportées
    public enum Language {
        FRENCH("fr"),
        ARABIC("ar"),
        ENGLISH("en");

        private final String code;
        Language(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    /**
     * Initialiser les catégories par défaut au démarrage
     */
    public static void initializeDefaultCategories() {
        try {
            // Vérifier si les catégories existent déjà
            if (categorieDAO.getAll().isEmpty()) {
                System.out.println("Initialisation des catégories par défaut...");

                // Catégories multilingues
                String[][] defaultCategories = {
                        {"Pays", "دول", "Country"},
                        {"Ville", "مدينة", "City"},
                        {"Prénom", "اسم", "First Name"},
                        {"Animal", "حيوان", "Animal"},
                        {"Couleur", "لون", "Color"},
                        {"Fruit", "فاكهة", "Fruit"},
                        {"Métier", "مهنة", "Profession"},
                        {"Sport", "رياضة", "Sport"},
                        {"Film", "فيلم", "Movie"},
                        {"Chanson", "أغنية", "Song"}
                };

                for (String[] category : defaultCategories) {
                    String nameFR = category[0];
                    String nameAR = category[1];
                    String nameEN = category[2];

                    // Créer la catégorie en français
                    Categorie categorieFR = new Categorie();
                    categorieFR.setNom(nameFR);
                    categorieFR.setDescription("Catégorie: " + nameFR);
                    categorieFR.setLangue("fr");
                    categorieDAO.save(categorieFR);
                    System.out.println("✓ Catégorie (FR) ajoutée: " + nameFR);

                    // Créer la catégorie en arabe
                    Categorie categorieAR = new Categorie();
                    categorieAR.setNom(nameAR);
                    categorieAR.setDescription("الفئة: " + nameAR);
                    categorieAR.setLangue("ar");
                    categorieDAO.save(categorieAR);
                    System.out.println("✓ Catégorie (AR) ajoutée: " + nameAR);

                    // Créer la catégorie en anglais
                    Categorie categorieEN = new Categorie();
                    categorieEN.setNom(nameEN);
                    categorieEN.setDescription("Category: " + nameEN);
                    categorieEN.setLangue("en");
                    categorieDAO.save(categorieEN);
                    System.out.println("✓ Catégorie (EN) ajoutée: " + nameEN);
                }

                System.out.println("Catégories par défaut initialisées avec succès!");
            } else {
                System.out.println("Les catégories existent déjà, pas d'initialisation nécessaire.");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des catégories: " + e.getMessage());
            e.printStackTrace();
        }
    }
}