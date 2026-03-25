package com.chrionline.shared.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public class Produit implements Serializable {

    private int idProduit;
    private int idCategorie;
    private String nom;
    private String description;
    private Timestamp dateAjout;


    private Categorie categorie;
    private List<ProductFormat> formats;

    public Produit() {
    }

    public Produit(int idProduit, int idCategorie, String nom, String description, Timestamp dateAjout, Categorie categorie, List<ProductFormat> formats) {
        this.idProduit = idProduit;
        this.idCategorie = idCategorie;
        this.nom = nom;
        this.description = description;
        this.dateAjout = dateAjout;
        this.categorie = categorie;
        this.formats = formats;
    }

    public int getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(int idProduit) {
        this.idProduit = idProduit;
    }

    public int getIdCategorie() {
        return idCategorie;
    }

    public void setIdCategorie(int idCategorie) {
        this.idCategorie = idCategorie;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(Timestamp dateAjout) {
        this.dateAjout = dateAjout;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public List<ProductFormat> getFormats() {
        return formats;
    }

    public void setFormats(List<ProductFormat> formats) {
        this.formats = formats;
    }
}
