package org.example.triharf.models;

import jakarta.persistence.*;

@Entity
@Table(name = "resultats_partie")
public class ResultatPartie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "partie_id", nullable = false)
    private Partie partie;

    // We store categorie_id to link back to categories table for SQL JOINs
    @ManyToOne
    @JoinColumn(name = "categorie_id", nullable = true) // Can be null if category deleted? preferably not
    private Categorie categorieObj; 

    // Legacy field for display, kept for now or transient? 
    // StatisticsService joins on categories table, so we need the ID.
    @Column(name = "nom_categorie")
    private String categorie; 

    @Column(length = 50)
    private String mot;

    private boolean valide;
    private int points;
    
    @Column(length = 255)
    private String message;

    public ResultatPartie() {} // Required by Hibernate

    public ResultatPartie(String categorie, String mot, boolean valide, int points, String message) {
        this.categorie = categorie;
        this.mot = mot;
        this.valide = valide;
        this.points = points;
        this.message = message;
    }

    // Setters for relationship
    public void setPartie(Partie partie) { this.partie = partie; }
    public void setCategorieObj(Categorie categorieObj) { 
        this.categorieObj = categorieObj; 
        if(categorieObj != null) this.categorie = categorieObj.getNom();
    }

    // Getters
    public Long getId() { return id; }
    public Partie getPartie() { return partie; }
    public Categorie getCategorieObj() { return categorieObj; }

    public String getCategorie() { return categorie; }
    public String getMot() { return mot; }
    public boolean isValide() { return valide; }
    public int getPoints() { return points; }
    public String getMessage() { return message; }
    public String getStatut() { return valide ? "✓ Validé" : "✗ Rejeté"; }
}