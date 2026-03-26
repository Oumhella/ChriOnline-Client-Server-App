package com.chrionline.shared.models;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

public class Wishlist implements Serializable {

    private int id;
    private int idUtilisateur;
    private String nom;
    private String description;
    private int nombreProduits;
    private Timestamp dateCreation;
    private Timestamp dateModification;

    private List<Produit> produits;

    public Wishlist() {
    }

    public Wishlist(int id, int idUtilisateur, String nom, String description, int nombreProduits, Timestamp dateCreation, Timestamp dateModification, List<Produit> produits) {
        this.id = id;
        this.idUtilisateur = idUtilisateur;
        this.nom = nom;
        this.description = description;
        this.nombreProduits = nombreProduits;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
        this.produits = produits;
    }
}
