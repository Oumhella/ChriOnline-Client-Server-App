package com.chrionline.shared.models;

import com.chrionline.shared.enums.StatutCommande;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Commande {

    private String idCommande;
    private int idUtilisateur;
    private double montantTotal;
    private StatutCommande statut;
    private LocalDateTime dateCommande;
    private List<LigneCommande> lignes = new ArrayList<>();

    // ───── Constructeur complet ─────
    public Commande(String idCommande, int idUtilisateur, double montantTotal,
                    StatutCommande statut, LocalDateTime dateCommande) {
        this.idCommande = idCommande;
        this.idUtilisateur = idUtilisateur;
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.dateCommande = dateCommande;
        this.lignes = new ArrayList<>();
    }

    // ───── Constructeur vide ─────
    public Commande() {
        this.lignes = new ArrayList<>();
    }

    // ───── Getters ─────
    public String getIdCommande() { return idCommande; }
    public int getIdUtilisateur() { return idUtilisateur; }
    public double getMontantTotal() { return montantTotal; }
    public StatutCommande getStatut() { return statut; }
    public LocalDateTime getDateCommande() { return dateCommande; }
    public List<LigneCommande> getLignes() { return lignes; }

    // ───── Setters ─────
    public void setIdCommande(String idCommande) { this.idCommande = idCommande; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }
    public void setLignes(List<LigneCommande> lignes) { this.lignes = lignes; }

    // ───── toString ─────
    @Override
    public String toString() {
        return "Commande{" +
                "idCommande='" + idCommande + '\'' +
                ", idUtilisateur=" + idUtilisateur +
                ", montantTotal=" + montantTotal +
                ", statut=" + statut +
                ", dateCommande=" + dateCommande +
                ", lignes=" + lignes.size() +
                '}';
    }
}