package com.chrionline.shared.models;

public class LigneCommande {

    private int idLigne;
    private String idCommande;
    private int idProduit;
    private String nomProduit;
    private int quantite;
    private double prixUnitaire;

    // ───── Constructeur complet ─────
    public LigneCommande(int idLigne, String idCommande, int idProduit,
                         String nomProduit, int quantite, double prixUnitaire) {
        this.idLigne = idLigne;
        this.idCommande = idCommande;
        this.idProduit = idProduit;
        this.nomProduit = nomProduit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    // ───── Constructeur vide ─────
    public LigneCommande() {}

    // ───── Getters ─────
    public int getIdLigne() { return idLigne; }
    public String getIdCommande() { return idCommande; }
    public int getIdProduit() { return idProduit; }
    public String getNomProduit() { return nomProduit; }
    public int getQuantite() { return quantite; }
    public double getPrixUnitaire() { return prixUnitaire; }

    // ───── Setters ─────
    public void setIdLigne(int idLigne) { this.idLigne = idLigne; }
    public void setIdCommande(String idCommande) { this.idCommande = idCommande; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    // ───── Méthode utilitaire ─────
    public double getSousTotal() {
        return quantite * prixUnitaire;
    }

    // ───── toString ─────
    @Override
    public String toString() {
        return "LigneCommande{" +
                "idLigne=" + idLigne +
                ", idProduit=" + idProduit +
                ", nomProduit='" + nomProduit + '\'' +
                ", quantite=" + quantite +
                ", prixUnitaire=" + prixUnitaire +
                ", sousTotal=" + getSousTotal() +
                '}';
    }
}