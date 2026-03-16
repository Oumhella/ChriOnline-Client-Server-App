package com.chrionline.shared.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class Produit implements Serializable {
    private int idProduit;
    private int idCategorie;
    private String nom;
    private String description;
    private BigDecimal prix;
    private int stock;
    private int stockAlerte;
    private String imageUrl;
    private Timestamp dateAjout;

    public Produit() {}

    public Produit(int idProduit, int idCategorie, String nom, String description, BigDecimal prix, int stock, int stockAlerte, String imageUrl, Timestamp dateAjout) {
        this.idProduit = idProduit;
        this.idCategorie = idCategorie;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.stock = stock;
        this.stockAlerte = stockAlerte;
        this.imageUrl = imageUrl;
        this.dateAjout = dateAjout;
    }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getStockAlerte() { return stockAlerte; }
    public void setStockAlerte(int stockAlerte) { this.stockAlerte = stockAlerte; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Timestamp getDateAjout() { return dateAjout; }
    public void setDateAjout(Timestamp dateAjout) { this.dateAjout = dateAjout; }

    @Override
    public String toString() {
        return "Produit{" +
                "idProduit=" + idProduit +
                ", nom='" + nom + '\'' +
                ", prix=" + prix +
                '}';
    }
}
