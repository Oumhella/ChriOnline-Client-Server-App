package com.chrionline.shared.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CommandeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String idCommande;
    private String nomUtilisateur;
    private double montantTotal;
    private String statut;
    private String dateCommande;
    private List<LigneCommandeDTO> lignes = new ArrayList<>();

    // ───── Constructeur complet ─────
    public CommandeDTO(String idCommande, String nomUtilisateur, double montantTotal,
                       String statut, String dateCommande) {
        this.idCommande = idCommande;
        this.nomUtilisateur = nomUtilisateur;
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.dateCommande = dateCommande;
        this.lignes = new ArrayList<>();
    }

    // ───── Constructeur vide ─────
    public CommandeDTO() {
        this.lignes = new ArrayList<>();
    }

    // ───── Getters ─────
    public String getIdCommande() { return idCommande; }
    public String getNomUtilisateur() { return nomUtilisateur; }
    public double getMontantTotal() { return montantTotal; }
    public String getStatut() { return statut; }
    public String getDateCommande() { return dateCommande; }
    public List<LigneCommandeDTO> getLignes() { return lignes; }

    // ───── Setters ─────
    public void setIdCommande(String idCommande) { this.idCommande = idCommande; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setDateCommande(String dateCommande) { this.dateCommande = dateCommande; }
    public void setLignes(List<LigneCommandeDTO> lignes) { this.lignes = lignes; }

    // ───── toString ─────
    @Override
    public String toString() {
        return "CommandeDTO{" +
                "idCommande='" + idCommande + '\'' +
                ", nomUtilisateur='" + nomUtilisateur + '\'' +
                ", montantTotal=" + montantTotal +
                ", statut='" + statut + '\'' +
                ", dateCommande='" + dateCommande + '\'' +
                ", lignes=" + lignes.size() +
                '}';
    }
}