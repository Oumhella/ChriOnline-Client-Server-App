package com.chrionline.server.service;

import com.chrionline.server.dao.PanierDAO;
import com.chrionline.shared.dto.LignePanierDTO;
import com.chrionline.shared.dto.PanierDTO;
import com.chrionline.shared.models.LignePanier;
import com.chrionline.shared.models.Panier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service panier : orchestre les opérations et convertit Model → DTO.
 * Appelé par ClientHandler, retourne toujours une Map statut/message/data.
 */
public class PanierService {



    public Map<String, Object> getPanier(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        if (idUtilisateur == -1)
            return erreur("idUtilisateur manquant.");

        try {
            Panier panier = PanierDAO.getPanierActif(idUtilisateur);
            return ok("Panier récupéré.", toDTO(panier));
        } catch (Exception e) {
            return erreur("Erreur récupération panier : " + e.getMessage());
        }
    }



    public Map<String, Object> ajouterProduit(Map<String, Object> req) {
        int idUtilisateur    = getInt(req, "idUtilisateur");
        int idProductFormats = getInt(req, "idProductFormats");
        int quantite         = getInt(req, "quantite");

        if (idUtilisateur == -1 || idProductFormats == -1)
            return erreur("Paramètres manquants.");
        if (quantite <= 0) quantite = 1;

        try {
            Panier panier = PanierDAO.ajouterProduit(idUtilisateur, idProductFormats, quantite);
            return ok("Produit ajouté au panier.", toDTO(panier));
        } catch (Exception e) {
            return erreur(e.getMessage());
        }
    }



    public Map<String, Object> modifierQuantite(Map<String, Object> req) {
        int idUtilisateur    = getInt(req, "idUtilisateur");
        int idProductFormats = getInt(req, "idProductFormats");
        int nouvelleQte      = getInt(req, "quantite");

        if (idUtilisateur == -1 || idProductFormats == -1)
            return erreur("Paramètres manquants.");

        try {
            Panier panier = PanierDAO.modifierQuantite(idUtilisateur, idProductFormats, nouvelleQte);
            return ok("Quantité mise à jour.", toDTO(panier));
        } catch (Exception e) {
            return erreur(e.getMessage());
        }
    }



    public Map<String, Object> retirerProduit(Map<String, Object> req) {
        int idUtilisateur    = getInt(req, "idUtilisateur");
        int idProductFormats = getInt(req, "idProductFormats");

        if (idUtilisateur == -1 || idProductFormats == -1)
            return erreur("Paramètres manquants.");

        try {
            Panier panier = PanierDAO.retirerProduit(idUtilisateur, idProductFormats);
            return ok("Produit retiré du panier.", toDTO(panier));
        } catch (Exception e) {
            return erreur(e.getMessage());
        }
    }

    public Map<String, Object> viderPanier(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        if (idUtilisateur == -1)
            return erreur("idUtilisateur manquant.");

        try {
            Panier panier = PanierDAO.viderPanier(idUtilisateur);
            return ok("Panier vidé.", toDTO(panier));
        } catch (Exception e) {
            return erreur(e.getMessage());
        }
    }



    public Map<String, Object> validerPanier(Map<String, Object> req) {
        int idUtilisateur = getInt(req, "idUtilisateur");
        if (idUtilisateur == -1)
            return erreur("idUtilisateur manquant.");

        try {
            String reference = PanierDAO.validerPanier(idUtilisateur);
            return Map.of(
                    "statut",    "OK",
                    "message",   "Commande créée avec succès !",
                    "reference", reference
            );
        } catch (Exception e) {
            return erreur(e.getMessage());
        }
    }


    private PanierDTO toDTO(Panier panier) {
        PanierDTO dto = new PanierDTO();
        dto.setIdPanier(panier.getIdPanier());
        dto.setMontantTotal(panier.getMontantTotal());
        dto.setStatut(panier.getStatut());

        List<LignePanierDTO> lignesDTO = new ArrayList<>();
        for (LignePanier ligne : panier.getLignes()) {
            LignePanierDTO l = new LignePanierDTO();
            l.setId_product_formats(ligne.getIdProductFormats());
            l.setQuantite(ligne.getQuantite());
            l.setPrix(ligne.getPrix());
            l.setTotal(ligne.getSousTotal());
            l.setNomProduit(ligne.getNomProduit());
            l.setDescriptionVariant(ligne.getDescriptionVariant());
            l.setImage_url(ligne.getImageUrl());
            lignesDTO.add(l);
        }
        dto.setLignes(lignesDTO);
        return dto;
    }


    private int getInt(Map<String, Object> req, String key) {
        Object val = req.get(key);
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof String)  {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private Map<String, Object> ok(String message, Object data) {
        return Map.of("statut", "OK", "message", message, "panier", data);
    }

    private Map<String, Object> erreur(String message) {
        return Map.of("statut", "ERREUR", "message", message);
    }
}