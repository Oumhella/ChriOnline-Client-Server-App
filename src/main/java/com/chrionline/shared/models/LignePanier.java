

package com.chrionline.shared.models;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Représente une ligne dans le panier.
 * Correspond à la table `ligne_panier` enrichie des infos produit.
 */
public class LignePanier implements Serializable {

    private int        idPanier;
    private int        idProductFormats;   // FK vers product_formats
    private int        quantite;
    private BigDecimal prix;

    // Infos dénormalisées pour l'affichage (non persistées, chargées par JOIN)
    private String     nomProduit;
    private String     descriptionVariant; // ex: "Taille M – Rouge"
    private String     imageUrl;

    public LignePanier() {}

    public LignePanier(int idPanier, int idProductFormats, int quantite, BigDecimal prixUnitaire) {
        this.idPanier          = idPanier;
        this.idProductFormats  = idProductFormats;
        this.quantite          = quantite;
        this.prix     = prixUnitaire;
    }

    // ── Calcul sous-total ──────────────────────────────────────────────────
    public BigDecimal getSousTotal() {
        if (prix == null) return BigDecimal.ZERO;
        return prix.multiply(BigDecimal.valueOf(quantite));
    }

    // ── Getters / Setters ──────────────────────────────────────────────────
    public int        getIdPanier()              { return idPanier; }
    public void       setIdPanier(int v)          { this.idPanier = v; }

    public int        getIdProductFormats()      { return idProductFormats; }
    public void       setIdProductFormats(int v)  { this.idProductFormats = v; }

    public int        getQuantite()              { return quantite; }
    public void       setQuantite(int v)          { this.quantite = v; }

    public BigDecimal getPrix()          { return prix; }
    public void       setPrix(BigDecimal v) { this.prix = v; }

    public String     getNomProduit()            { return nomProduit; }
    public void       setNomProduit(String v)     { this.nomProduit = v; }

    public String     getDescriptionVariant()    { return descriptionVariant; }
    public void       setDescriptionVariant(String v) { this.descriptionVariant = v; }

    public String     getImageUrl()              { return imageUrl; }
    public void       setImageUrl(String v)       { this.imageUrl = v; }

    @Override
    public String toString() {
        return "LignePanier{format=" + idProductFormats + ", qte=" + quantite + ", prix=" + prix+ "}";
    }
}
