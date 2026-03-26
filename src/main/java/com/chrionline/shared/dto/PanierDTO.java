package com.chrionline.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO du panier — objet léger transféré entre client et serveur via TCP.
 * Ne contient que ce dont la vue a besoin.
 */
public class PanierDTO implements Serializable {

    private int               idPanier;
    private BigDecimal        montantTotal;
    private String            statut;
    private List<LignePanierDTO> lignes;


    public PanierDTO() {
        this.lignes       = new ArrayList<>();
        this.montantTotal = BigDecimal.ZERO;
    }

    public int               getIdPanier()       { return idPanier; }
    public void              setIdPanier(int v)   { this.idPanier = v; }

    public BigDecimal        getMontantTotal()    { return montantTotal; }
    public void              setMontantTotal(BigDecimal v) { this.montantTotal = v; }

    public String            getStatut()          { return statut; }
    public void              setStatut(String v)   { this.statut = v; }

    public List<LignePanierDTO> getLignes()       { return lignes; }
    public void              setLignes(List<LignePanierDTO> v) { this.lignes = v; }

    /** Nombre total d'articles (somme des quantités). */
    public int getNombreArticles() {
        return lignes.stream().mapToInt(LignePanierDTO::getQuantite).sum();
    }
}