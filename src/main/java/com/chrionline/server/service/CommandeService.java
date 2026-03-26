package com.chrionline.server.service;

import com.chrionline.server.dao.CommandeDAO;
import com.chrionline.server.dao.LigneCommandeDAO;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LigneCommandeDTO;
import com.chrionline.shared.enums.StatutCommande;
import com.chrionline.shared.models.Commande;
import com.chrionline.shared.models.LigneCommande;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommandeService {

    private CommandeDAO commandeDAO;
    private LigneCommandeDAO ligneCommandeDAO;

    public CommandeService(CommandeDAO commandeDAO, LigneCommandeDAO ligneCommandeDAO) {
        this.commandeDAO = commandeDAO;
        this.ligneCommandeDAO = ligneCommandeDAO;
    }

    // ───── Lister toutes les commandes ─────
    public List<CommandeDTO> getAllCommandes() throws SQLException {
        List<Commande> commandes = commandeDAO.findAll();
        List<CommandeDTO> dtos = new ArrayList<>();

        for (Commande c : commandes) {
            CommandeDTO dto = convertToDTO(c);
            dtos.add(dto);
        }
        return dtos;
    }

    // ───── Détail d'une commande ─────
    public CommandeDTO getCommandeById(String idCommande) throws SQLException {
        Commande c = commandeDAO.findById(idCommande);

        if (c == null) {
            throw new IllegalArgumentException("Commande introuvable : " + idCommande);
        }

        CommandeDTO dto = convertToDTO(c);

        // Ajouter les lignes de commande
        List<LigneCommande> lignes = ligneCommandeDAO.findByCommande(idCommande);
        for (LigneCommande ligne : lignes) {
            dto.getLignes().add(new LigneCommandeDTO(
                    ligne.getIdLigne(),
                    ligne.getIdProduit(),
                    ligne.getNomProduit(),
                    ligne.getQuantite(),
                    ligne.getPrixUnitaire()
            ));
        }
        return dto;
    }

    // ───── Mettre à jour le statut ─────
    public String updateStatut(String idCommande, String nouveauStatut) throws SQLException {

        // 1️⃣ Convertir le statut d'abord (fail-fast)
        StatutCommande statut;
        try {
            statut = StatutCommande.valueOf(nouveauStatut.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "ERREUR: Statut invalide → " + nouveauStatut;
        }

        // 2️⃣ Vérifier que la commande existe
        Commande commande = commandeDAO.findById(idCommande);
        if (commande == null) {
            return "ERREUR: Commande introuvable : " + idCommande;
        }

        // 3️⃣ Bloquer uniquement si la commande est déjà terminale
        StatutCommande statutActuel = commande.getStatut();
        if (statutActuel == StatutCommande.LIVREE) {
            return "ERREUR: Commande déjà livrée, aucune modification possible";
        }
        if (statutActuel == StatutCommande.ANNULEE) {
            return "ERREUR: Commande déjà annulée, aucune modification possible";
        }

        // 4️⃣ Si annulation → restaurer le stock
        if (statut == StatutCommande.ANNULEE) {
            ligneCommandeDAO.restaurerStock(idCommande);
        }

        // 5️⃣ Mettre à jour en BDD
        boolean ok = commandeDAO.updateStatus(idCommande, statut);
        if (ok) {
            System.out.println("[SERVICE] Statut commande " + idCommande + " mis à jour : " + statut.name());
            return "SUCCESS: Statut mis à jour → " + statut.name();
        } else {
            return "ERREUR: Mise à jour échouée (0 ligne affectée)";
        }
    }

    // ───── Conversion Commande → CommandeDTO ─────
    private CommandeDTO convertToDTO(Commande c) {
        CommandeDTO dto = new CommandeDTO();
        dto.setIdCommande(c.getIdCommande());
        dto.setReference(c.getReference());  // ← numéro de commande (ex: CMD-2026-00042)
        dto.setNomUtilisateur(c.getNomClient() != null ? c.getNomClient() : "Client #" + c.getIdUtilisateur());
        dto.setMontantTotal(c.getMontantTotal());
        dto.setStatut(c.getStatut() != null ? c.getStatut().name() : "EN_PREPARATION");
        dto.setDateCommande(c.getDateCommande() != null ? c.getDateCommande().toString() : "");
        return dto;
    }
}