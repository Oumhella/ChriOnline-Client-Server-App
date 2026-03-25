package com.chrionline.shared.models;
import java.io.Serializable;
import java.time.LocalDateTime;

public class Panier implements Serializable{
    private int id_panier ;
    private float montant_total;
    private String statut ;
    private String session_id ;
    private LocalDateTime date_creation;
    private LocalDateTime date_modification;

    public Panier(int id_panier, float montant_total, String statut, String session_id, LocalDateTime date_creation, LocalDateTime date_modification) {
        this.id_panier = id_panier;
        this.montant_total = montant_total;
        this.statut = statut;
        this.session_id = session_id;
        this.date_creation = date_creation;
        this.date_modification = date_modification;
    }

    public int getId_panier() {
        return id_panier;
    }

    public void setId_panier(int id_panier) {
        this.id_panier = id_panier;
    }

    public float getMontant_total() {
        return montant_total;
    }

    public void setMontant_total(float montant_total) {
        this.montant_total = montant_total;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    public LocalDateTime getDate_creation() {
        return date_creation;
    }

    public void setDate_creation(LocalDateTime date_creation) {
        this.date_creation = date_creation;
    }

    public LocalDateTime getDate_modification() {
        return date_modification;
    }

    public void setDate_modification(LocalDateTime date_modification) {
        this.date_modification = date_modification;
    }
}
