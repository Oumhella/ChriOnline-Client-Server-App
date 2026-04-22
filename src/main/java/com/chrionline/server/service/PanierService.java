package com.chrionline.server.service;

import com.chrionline.server.dao.PanierDAO;
import com.chrionline.server.dao.TokenDAO;
import com.chrionline.server.dao.UserDAO;
import com.chrionline.shared.models.Panier;
import com.chrionline.shared.models.LignePanier;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.PanierDTO;
import com.chrionline.shared.dto.LignePanierDTO;
import com.chrionline.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Service gérant le panier (CRUD) et la validation des commandes.
 */
public class PanierService {

    private final EmailService emailService = new EmailService();

    // ─── CRUD PANIER (Restauré) ─────────────────────────────────────────────

    public Map<String, Object> getPanier(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        if (idUtilisateur == -1) return erreur("Utilisateur non identifié.");
        try {
            Panier p = PanierDAO.getPanierActif(idUtilisateur);
            return ok(Map.of("panier", mapToDTO(p)));
        } catch (SQLException e) {
            e.printStackTrace();
            return erreur("Erreur BDD : " + e.getMessage());
        }
    }

    public Map<String, Object> ajouterProduit(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        int idFormat = getInt(req, "idProductFormats");
        int qte = getInt(req, "quantite");

        try {
            Panier p = PanierDAO.ajouterProduit(idUtilisateur, idFormat, qte);
            return ok(Map.of("panier", mapToDTO(p)));
        } catch (SQLException e) {
            e.printStackTrace();
            return erreur("Erreur ajout produit : " + e.getMessage());
        }
    }

    public Map<String, Object> modifierQuantite(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        int idFormat = getInt(req, "idProductFormats");
        int qte = getInt(req, "quantite");

        try {
            Panier p = PanierDAO.modifierQuantite(idUtilisateur, idFormat, qte);
            return ok(Map.of("panier", mapToDTO(p)));
        } catch (SQLException e) {
            e.printStackTrace();
            return erreur("Erreur modification quantite : " + e.getMessage());
        }
    }

    public Map<String, Object> retirerProduit(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        int idFormat = getInt(req, "idProductFormats");

        try {
            Panier p = PanierDAO.retirerProduit(idUtilisateur, idFormat);
            return ok(Map.of("panier", mapToDTO(p)));
        } catch (SQLException e) {
            e.printStackTrace();
            return erreur("Erreur retrait produit : " + e.getMessage());
        }
    }

    public Map<String, Object> viderPanier(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        try {
            Panier p = PanierDAO.viderPanier(idUtilisateur);
            return ok(Map.of("panier", mapToDTO(p)));
        } catch (SQLException e) {
            e.printStackTrace();
            return erreur("Erreur vidage panier : " + e.getMessage());
        }
    }

    // ─── VALIDATION & OTP (Nouveau) ───────────────────────────────────────────

    public Map<String, Object> validerPanier(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        if (idUtilisateur == -1) return erreur("Utilisateur non identifié.");
        try {
            CommandeDTO recap = PanierDAO.validerPanier(idUtilisateur);
            Map<String, Object> res = new HashMap<>();
            res.put("statut", "OK");
            res.put("message", "Panier validé. Veuillez confirmer avec le code OTP reçu par email.");
            res.put("recap", recap);
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return erreur("Erreur BDD : " + e.getMessage());
        }
    }

    public Map<String, Object> demanderOTPPayment(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        try {
            Panier p = PanierDAO.getPanierActif(idUtilisateur);
            Map<String, Object> profil = UserDAO.getInfosProfil(idUtilisateur);
            if (!"OK".equals(profil.get("statut"))) return profil;
            
            @SuppressWarnings("unchecked")
            String email = (String) ((Map<String, Object>) profil.get("data")).get("email");
            
            // Génération OTP unifiée via TokenDAO
            String otpCode = TokenDAO.genererToken(idUtilisateur, "paiement");

            emailService.envoyerOTPTransaction(email, otpCode, p.getMontantTotal().doubleValue());
            
            return ok(Map.of("message", "Code de sécurité envoyé à " + email));
        } catch (Exception e) {
            e.printStackTrace();
            return erreur("Erreur génération OTP : " + e.getMessage());
        }
    }

    public Map<String, Object> confirmerCommande(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        String otpCode = (String) req.get("otpCode");

        try {
            // Vérification et consommation atomique du token
            int verifiedUserId = TokenDAO.consommerToken(otpCode, "paiement");
            if (verifiedUserId == -1 || verifiedUserId != idUtilisateur) {
                return erreur("Code de sécurité invalide ou expiré.");
            }

            // Création réelle de la commande
            CommandeDTO res = PanierDAO.confirmerCommande(
                idUtilisateur, 
                (String) req.get("methodePaiement"),
                (String) req.get("nomCarte"),
                (String) req.get("numeroCarte")
            );
            
            if (res != null) {
                return ok(Map.of("message", "Commande réussie !", "commandeResult", res));
            } else {
                return erreur("Échec lors de la création de la commande.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return erreur("Erreur confirmation : " + e.getMessage());
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private PanierDTO mapToDTO(Panier p) {
        PanierDTO dto = new PanierDTO();
        dto.setIdPanier(p.getIdPanier());
        dto.setMontantTotal(p.getMontantTotal());
        dto.setStatut(p.getStatut());
        List<LignePanierDTO> lignesDTO = new ArrayList<>();
        if (p.getLignes() != null) {
            for (LignePanier l : p.getLignes()) {
                lignesDTO.add(new LignePanierDTO(
                    l.getIdProductFormats(), 
                    l.getPrix(), 
                    0, // Stock non nécessaire pour le DTO simple ici
                    l.getQuantite(), 
                    l.getImageUrl(), 
                    l.getNomProduit(), 
                    l.getDescriptionVariant()
                ));
            }
        }
        dto.setLignes(lignesDTO);
        return dto;
    }

    private int getInt(Map<String, Object> req, String key) {
        Object val = req.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (Exception e) {}
        }
        return -1;
    }

    private Map<String, Object> ok(Map<String, Object> data) {
        Map<String, Object> res = new HashMap<>(data);
        res.put("statut", "OK");
        return res;
    }

    private Map<String, Object> erreur(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("statut", "ERREUR");
        map.put("message", msg);
        return map;
    }
}