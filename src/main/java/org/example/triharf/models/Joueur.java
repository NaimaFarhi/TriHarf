package org.example.triharf.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "joueurs")
public class Joueur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String pseudo;

    @Column(name = "score_total")
    private Integer scoreTotal = 0;

    @Column(name = "nb_parties")
    private Integer nbParties = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructeurs
    public Joueur() {}

    public Joueur(String pseudo) {
        this.pseudo = pseudo;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPseudo() { return pseudo; }
    public void setPseudo(String pseudo) { this.pseudo = pseudo; }

    public Integer getScoreTotal() { return scoreTotal; }
    public void setScoreTotal(Integer scoreTotal) { this.scoreTotal = scoreTotal; }

    public Integer getNbParties() { return nbParties; }
    public void setNbParties(Integer nbParties) { this.nbParties = nbParties; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return pseudo;
    }
}