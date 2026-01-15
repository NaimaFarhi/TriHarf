package org.example.triharf.models;

import jakarta.persistence.*;
import org.example.triharf.enums.Langue;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "langue")
    @Enumerated(EnumType.ORDINAL)
    private Langue langue;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructeurs
    public Categorie() {}

    public Categorie(String nom) {
        this.nom = nom;
    }

    public Categorie(String nom, Langue langue) {
        this.nom = nom;
        this.langue = langue;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Langue getLangue() { return langue; }
    public void setLangue(Langue langue) { this.langue = langue; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return nom + " (" + langue + ")";
    }
}