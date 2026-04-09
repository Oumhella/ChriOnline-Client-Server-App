package com.chrionline.server.service;

import com.chrionline.server.dao.UserDAO;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class AdminUserService {

    public static Map<String, Object> handleListerClients() {
        Map<String, Object> reponse = new HashMap<>();
        try {
            List<Map<String, Object>> clients = UserDAO.listerClients();
            reponse.put("statut", "OK");
            reponse.put("data", clients);
        } catch (Exception e) {
            e.printStackTrace();
            reponse.put("statut", "ERREUR");
            reponse.put("message", "Erreur serveur : " + e.getMessage());
        }
        return reponse;
    }

    public static Map<String, Object> handleChangerStatutClient(Map<String, Object> requete) {
        Map<String, Object> reponse = new HashMap<>();
        try {
            int idUtilisateur = (int) requete.get("idUtilisateur");
            String statut = (String) requete.get("statut");

            // ─── Validation du statut contre la liste blanche ────────────
            if (!InputValidator.isValidAccountStatut(statut)) {
                reponse.put("statut", "ERREUR");
                reponse.put("message", "Statut invalide : '" + statut + "'. Valeurs autorisées : actif, bloque.");
                System.out.println("[SECURITY] Tentative de changement de statut invalide : '" + statut 
                        + "' pour utilisateur " + idUtilisateur);
                return reponse;
            }

            Map<String, Object> daoRes = UserDAO.changerStatutCompte(idUtilisateur, statut);
            return daoRes;
        } catch (Exception e) {
            e.printStackTrace();
            reponse.put("statut", "ERREUR");
            reponse.put("message", "Erreur validation: " + e.getMessage());
        }
        return reponse;
    }
}
