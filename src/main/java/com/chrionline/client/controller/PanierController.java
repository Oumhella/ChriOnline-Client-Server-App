
package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.PanierDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur client pour toutes les opérations du panier.
 * Communique avec le serveur via TCP et retourne des PanierDTO.
 *
 * Utilisation depuis la vue :
 *   PanierController ctrl = new PanierController(idUtilisateur);
 *   PanierDTO panier = ctrl.getPanier();
 *   PanierDTO panier = ctrl.ajouterProduit(idFormat, quantite);
 */
public class PanierController {

    private final int    idUtilisateur;
    private final Client client;

    public PanierController(int idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
        this.client        = Client.getInstance("localhost", 12345);
    }

    // ─── Récupérer le panier ──────────────────────────────────────────────

    public PanierDTO getPanier() {
        Map<String, Object> req = new HashMap<>();
        req.put("commande",      "PANIER_GET");
        req.put("idUtilisateur", idUtilisateur);
        return envoyerEtLire(req);
    }

    // ─── Ajouter un produit ───────────────────────────────────────────────

    public PanierDTO ajouterProduit(int idProductFormats, int quantite) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande",         "PANIER_AJOUTER");
        req.put("idUtilisateur",    idUtilisateur);
        req.put("idProductFormats", idProductFormats);
        req.put("quantite",         quantite);
        return envoyerEtLire(req);
    }

    // ─── Modifier la quantité ─────────────────────────────────────────────

    public PanierDTO modifierQuantite(int idProductFormats, int nouvelleQte) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande",         "PANIER_MODIFIER_QTE");
        req.put("idUtilisateur",    idUtilisateur);
        req.put("idProductFormats", idProductFormats);
        req.put("quantite",         nouvelleQte);
        return envoyerEtLire(req);
    }

    // ─── Retirer un produit ───────────────────────────────────────────────

    public PanierDTO retirerProduit(int idProductFormats) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande",         "PANIER_RETIRER");
        req.put("idUtilisateur",    idUtilisateur);
        req.put("idProductFormats", idProductFormats);
        return envoyerEtLire(req);
    }

    // ─── Vider le panier ──────────────────────────────────────────────────

    public PanierDTO viderPanier() {
        Map<String, Object> req = new HashMap<>();
        req.put("commande",      "PANIER_VIDER");
        req.put("idUtilisateur", idUtilisateur);
        return envoyerEtLire(req);
    }

    // ─── Valider → passer commande ────────────────────────────────────────

    /**
     * Valide le panier et crée une commande.
     * @return un objet CommandeDTO contenant le récapitulatif ou null si erreur.
     */
    public CommandeDTO validerPanier() {
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("commande",      "PANIER_VALIDER");
            req.put("idUtilisateur", idUtilisateur);

            client.connecter();
            client.envoyerRequete(req);

            @SuppressWarnings("unchecked")
            Map<String, Object> rep = (Map<String, Object>) client.lireReponse();

            if ("OK".equals(rep.get("statut"))) {
                return (CommandeDTO) rep.get("recap");
            } else {
                System.err.println("[PanierController] Erreur validation : " + rep.get("message"));
                return null;
            }
        } catch (Exception e) {
            System.err.println("[PanierController] Erreur réseau : " + e.getMessage());
            return null;
        }
    }

    /**
     * Confirmation de commande / paiement avec 2FA simulé côté serveur.
     * <ul>
     *   <li>1er appel avec {@code payment2faCode == null} : le serveur génère un code (réponse {@code REQUIRES_PAYMENT_2FA}).</li>
     *   <li>2e appel avec le code saisi : finalise la commande si le code est valide.</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> confirmerCommandeEtape(String methodePaiement, String nomCarte, char[] numeroCarte,
                                                      String payment2faCode) {
        try {
            // [ZKP RSA] Récupérer la clé publique du serveur si non définie
            if (!com.chrionline.securite.PaymentCrypto.hasPublicKey()) {
                try {
                    Map<String, Object> keyReq = new HashMap<>();
                    keyReq.put("commande", "PAYMENT_GET_PUBLIC_KEY");
                    client.connecter();
                    client.envoyerRequete(keyReq);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> keyRep = (Map<String, Object>) client.lireReponse();
                    if (keyRep != null && "OK".equals(keyRep.get("statut"))) {
                        String pubKeyBase64 = (String) keyRep.get("publicKey");
                        com.chrionline.securite.PaymentCrypto.setServerPublicKey(pubKeyBase64);
                    }
                } catch (Exception ex) {
                    System.err.println("[PanierController] Échec récupération clé publique de paiement : " + ex.getMessage());
                }
            }

            Map<String, Object> req = new HashMap<>();
            req.put("commande", "COMMANDE_CONFIRMER");
            req.put("idUtilisateur", idUtilisateur);
            req.put("methodePaiement", methodePaiement);
            req.put("nomCarte", nomCarte != null ? nomCarte : "");

            // [ZKP RSA & Memory Cleaning] Chiffrement RSA et écrasement immédiat de la RAM
            if (numeroCarte != null && numeroCarte.length > 0) {
                String numeroChiffre = com.chrionline.securite.PaymentCrypto.encrypt(numeroCarte);
                req.put("numeroCarteChiffre", numeroChiffre);
                System.out.println("[PAIEMENT] Numéro de carte chiffré asymétriquement avec RSA-256 (OAEP) avant envoi.");
            } else {
                req.put("numeroCarte", "");
            }

            if (payment2faCode != null && !payment2faCode.isBlank()) {
                req.put("payment2faCode", payment2faCode.trim());
            }

            client.connecter();
            client.envoyerRequete(req);

            Map<String, Object> rep = (Map<String, Object>) client.lireReponse();
            return rep != null ? rep : Map.of("statut", "ERREUR", "message", "Réponse serveur vide.");
        } catch (Exception e) {
            System.err.println("[PanierController] Erreur réseau : " + e.getMessage());
            Map<String, Object> err = new HashMap<>();
            err.put("statut", "ERREUR");
            err.put("message", e.getMessage());
            return err;
        }
    }

    /**
     * @deprecated Préférer {@link #confirmerCommandeEtape(String, String, String, String)} (flux 2 étapes).
     */
    @Deprecated
    public CommandeDTO confirmerCommande(String methodePaiement, String nomCarte, String numeroCarte) {
        char[] cardChars = numeroCarte != null ? numeroCarte.toCharArray() : new char[0];
        Map<String, Object> rep = confirmerCommandeEtape(methodePaiement, nomCarte, cardChars, null);
        if ("OK".equals(rep.get("statut"))) {
            return (CommandeDTO) rep.get("commandeResult");
        }
        System.err.println("[PanierController] Erreur confirmation : " + rep.get("message"));
        return null;
    }

    // ─── Helper réseau ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private PanierDTO envoyerEtLire(Map<String, Object> req) {
        try {
            client.connecter();
            client.envoyerRequete(req);
            Map<String, Object> rep = (Map<String, Object>) client.lireReponse();

            if ("OK".equals(rep.get("statut"))) {
                return (PanierDTO) rep.get("panier");
            } else {
                System.err.println("[PanierController] " + rep.get("message"));
                return null;
            }
        } catch (Exception e) {
            System.err.println("[PanierController] Erreur réseau : " + e.getMessage());
            return null;
        }
    }
}