package com.chrionline.shared.dto;

import java.io.Serializable;

public class LigneCommandeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idLigne;
    private int idProduit;
    private String nomProduit;
    private int quantite;
    private double prixUnitaire;
    private double sousTotal;

    // ───── Constructeur complet ─────
    public LigneCommandeDTO(int idLigne, int idProduit, String nomProduit,
                            int quantite, double prixUnitaire) {
        this.idLigne = idLigne;
        this.idProduit = idProduit;
        this.nomProduit = nomProduit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.sousTotal = quantite * prixUnitaire;
    }

    // ───── Constructeur vide ─────
    public LigneCommandeDTO() {}

    // ───── Getters ─────
    public int getIdLigne() { return idLigne; }
    public int getIdProduit() { return idProduit; }
    public String getNomProduit() { return nomProduit; }
    public int getQuantite() { return quantite; }
    public double getPrixUnitaire() { return prixUnitaire; }
    public double getSousTotal() { return sousTotal; }

    // ───── Setters ─────
    public void setIdLigne(int idLigne) { this.idLigne = idLigne; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public void setSousTotal(double sousTotal) { this.sousTotal = sousTotal; }

    // ───── toString ─────
    @Override
    public String toString() {
        return "LigneCommandeDTO{" +
                "idLigne=" + idLigne +
                ", nomProduit='" + nomProduit + '\'' +
                ", quantite=" + quantite +
                ", prixUnitaire=" + prixUnitaire +
                ", sousTotal=" + sousTotal +
                '}';
    }
}