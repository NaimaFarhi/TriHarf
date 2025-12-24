package org.example.triharf.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"texte", "categorie_id"}))
public class Mot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String texte;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    @Column(nullable = false, length = 1)
    private Character lettre;

    @Column(nullable = false)
    private Boolean valide = true;

    @Column(name = "nb_utilisations")
    private Integer nbUtilisations = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructeurs
    public Mot() {}

    public Mot(String texte, Categorie categorie, Character lettre) {
        this.texte = texte;
        this.categorie = categorie;
        this.lettre = lettre;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }

    public Character getLettre() { return lettre; }
    public void setLettre(Character lettre) { this.lettre = lettre; }

    public Boolean getValide() { return valide; }
    public void setValide(Boolean valide) { this.valide = valide; }

    public Integer getNbUtilisations() { return nbUtilisations; }
    public void setNbUtilisations(Integer nbUtilisations) { this.nbUtilisations = nbUtilisations; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return texte;
    }
}