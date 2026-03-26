package com.chrionline.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class LignePanierDTO implements Serializable {

    private int        id_product_formats;
    private BigDecimal prix;
    private int        stock;
    private int        quantite;
    private String     image_url;
    private String     nomProduit;
    private String     descriptionVariant;
    private BigDecimal total;

    public LignePanierDTO() {}

    public LignePanierDTO(int id_product_formats, BigDecimal prix, int stock,
                          int quantite, String image_url,
                          String nomProduit, String descriptionVariant) {
        this.id_product_formats = id_product_formats;
        this.prix               = prix;
        this.stock              = stock;
        this.quantite           = quantite;
        this.image_url          = image_url;
        this.nomProduit         = nomProduit;
        this.descriptionVariant = descriptionVariant;
        // ✅ Calcul correct du total
        this.total = (prix != null)
                ? prix.multiply(BigDecimal.valueOf(quantite))
                : BigDecimal.ZERO;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public int        getId_product_formats()                    { return id_product_formats; }
    public void       setId_product_formats(int v)               { this.id_product_formats = v; }

    public BigDecimal getPrix()                                  { return prix; }
    public void       setPrix(BigDecimal v)                      { this.prix = v; }

    public int        getStock()                                 { return stock; }
    public void       setStock(int v)                            { this.stock = v; }

    public int        getQuantite()                              { return quantite; }
    public void       setQuantite(int v)                         { this.quantite = v; }

    public String     getImage_url()                             { return image_url; }
    public void       setImage_url(String v)                     { this.image_url = v; }

    public String     getNomProduit()                            { return nomProduit; }
    public void       setNomProduit(String v)                    { this.nomProduit = v; }

    public String     getDescriptionVariant()                    { return descriptionVariant; }
    public void       setDescriptionVariant(String v)            { this.descriptionVariant = v; }

    public BigDecimal getTotal()                                 { return total; }
    public void       setTotal(BigDecimal v)                     { this.total = v; }
}