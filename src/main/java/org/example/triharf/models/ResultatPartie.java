package org.example.triharf.models;

public class ResultatPartie {
    private String categorie;
    private String mot;
    private boolean valide;
    private int points;
    private String message;

    public ResultatPartie(String categorie, String mot, boolean valide, int points, String message) {
        this.categorie = categorie;
        this.mot = mot;
        this.valide = valide;
        this.points = points;
        this.message = message;
    }

    // Getters
    public String getCategorie() { return categorie; }
    public String getMot() { return mot; }
    public boolean isValide() { return valide; }
    public int getPoints() { return points; }
    public String getMessage() { return message; }
    public String getStatut() { return valide ? "✓ Validé" : "✗ Rejeté"; }
}