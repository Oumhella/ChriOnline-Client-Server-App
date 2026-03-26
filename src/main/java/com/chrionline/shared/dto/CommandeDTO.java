package com.chrionline.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour le récapitulatif d'une commande passée.
 * Contient les infos de la commande et du client.
 */
public class CommandeDTO implements Serializable {

    private String reference;
    private LocalDateTime dateCommande;
    private BigDecimal montantTotal;
    private String status;
    private List<LignePanierDTO> lignes;

    // Infos Client
    private String clientNom;
    private String clientPrenom;
    private String clientEmail;
    private String clientTelephone;
    private String clientAdresse;

    public CommandeDTO() {}

    // Getters and Setters
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<LignePanierDTO> getLignes() { return lignes; }
    public void setLignes(List<LignePanierDTO> lignes) { this.lignes = lignes; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientPrenom() { return clientPrenom; }
    public void setClientPrenom(String clientPrenom) { this.clientPrenom = clientPrenom; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public String getClientAdresse() { return clientAdresse; }
    public void setClientAdresse(String clientAdresse) { this.clientAdresse = clientAdresse; }
}
