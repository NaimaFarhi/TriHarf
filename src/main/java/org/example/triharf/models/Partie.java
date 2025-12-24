package org.example.triharf.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parties")
public class Partie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "joueur_id", nullable = false)
    private Joueur joueur;

    @Column(nullable = false, length = 1)
    private Character lettre;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(nullable = false, length = 50)
    private String mode;

    @Column(name = "duree_seconde")
    private Integer dureeSeconde;

    @Column(name = "date_partie")
    private LocalDateTime datePartie;

    @PrePersist
    protected void onCreate() {
        datePartie = LocalDateTime.now();
    }

    // Constructeurs
    public Partie() {}

    public Partie(Joueur joueur, Character lettre, String mode) {
        this.joueur = joueur;
        this.lettre = lettre;
        this.mode = mode;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Joueur getJoueur() { return joueur; }
    public void setJoueur(Joueur joueur) { this.joueur = joueur; }

    public Character getLettre() { return lettre; }
    public void setLettre(Character lettre) { this.lettre = lettre; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Integer getDureeSeconde() { return dureeSeconde; }
    public void setDureeSeconde(Integer dureeSeconde) { this.dureeSeconde = dureeSeconde; }

    public LocalDateTime getDatePartie() { return datePartie; }

    @Override
    public String toString() {
        return "Partie " + id + " - " + mode;
    }
}