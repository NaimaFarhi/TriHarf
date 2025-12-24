import org.example.triharf.config.HibernateUtil;
import org.hibernate.Session;

public class TestHibernate {
    public static void main(String[] args) {
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            System.out.println("✅ Connexion Hibernate réussie!");
            System.out.println("✅ Tables générées automatiquement");
            session.close();
            HibernateUtil.shutdown();
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
