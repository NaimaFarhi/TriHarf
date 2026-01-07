i
import org.example.triharf.dao.*;
import org.example.triharf.enums.Langue;
import org.example.triharf.models.*;
import org.example.triharf.services.ValidationService;

public class TestDAO {
    public static void main(String[] args) {
        CategorieDAO catDAO = new CategorieDAO();
        ValidationService validator = new ValidationService();

        // Check if exists, else create
        Categorie cat = catDAO.findByNom("Animal");
        if (cat == null) {
            cat = new Categorie("Animal");
            cat = catDAO.save(cat);
        }
        System.out.println("✅ Catégorie: " + cat.getId());

        // Test Validation
        var result = validator.validateMot("Apple", cat, 'A', Langue.ENGLISH);
        System.out.println("✅ Validation: " + result.getMessage());

        System.out.println("✅ Sprint 1 - DAOs fonctionnels!");
    }
}