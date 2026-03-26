package com.chrionline.shared.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


public class Panier implements Serializable {

    private int          idPanier;
    private int          idUtilisateur;
    private BigDecimal   montantTotal;
    private String       statut;          // 'actif' | 'valide' | 'abandonne'
    private Timestamp    dateCreation;
    private Timestamp    dateModification;
    private List<LignePanier> lignes;

    public Panier() {
        this.lignes       = new ArrayList<>();
        this.montantTotal = BigDecimal.ZERO;
        this.statut       = "actif";
    }

    // ── Recalcul du total à partir des lignes ─────────────────────────────
    public void recalculerTotal() {
        this.montantTotal = lignes.stream()
                .map(l -> l.getPrix().multiply(BigDecimal.valueOf(l.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────
    public int          getIdPanier()           { return idPanier; }
    public void         setIdPanier(int v)       { this.idPanier = v; }

    public int          getIdUtilisateur()       { return idUtilisateur; }
    public void         setIdUtilisateur(int v)  { this.idUtilisateur = v; }

    public BigDecimal   getMontantTotal()        { return montantTotal; }
    public void         setMontantTotal(BigDecimal v) { this.montantTotal = v; }

    public String       getStatut()              { return statut; }
    public void         setStatut(String v)       { this.statut = v; }

    public Timestamp    getDateCreation()        { return dateCreation; }
    public void         setDateCreation(Timestamp v) { this.dateCreation = v; }

    public Timestamp    getDateModification()    { return dateModification; }
    public void         setDateModification(Timestamp v) { this.dateModification = v; }

    public List<LignePanier> getLignes()         { return lignes; }
    public void         setLignes(List<LignePanier> v) { this.lignes = v; }

    @Override
    public String toString() {
        return "Panier{id=" + idPanier + ", total=" + montantTotal + ", lignes=" + lignes.size() + "}";
    }
}