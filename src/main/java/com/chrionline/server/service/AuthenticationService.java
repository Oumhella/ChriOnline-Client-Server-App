package com.chrionline.server.service;

import com.chrionline.server.dao.TokenDAO;
import com.chrionline.server.dao.UserDAO;
import java.util.Map;
import java.util.HashMap;

/**
 * Service gérant l'authentification, l'inscription et la récupération de compte.
 */
public class AuthenticationService {

    // ─── Inscription ──────────────────────────────────────────────────────────

    public Map<String, Object> register(Map<String, Object> req) {
        // ─── Validation serveur des entrées (ne pas se fier au client) ────
        String email = (String) req.get("email");
        String nom = (String) req.get("nom");
        String prenom = (String) req.get("prenom");
        String mdp = (String) req.get("mdp");
        String telephone = (String) req.getOrDefault("telephone", "");

        if (!InputValidator.isValidEmail(email)) {
            return Map.of("statut", "ERREUR", "message", "Format d'email invalide.");
        }
        if (!InputValidator.isValidName(nom)) {
            return Map.of("statut", "ERREUR", "message", "Nom invalide (lettres, espaces et tirets uniquement, max 50 caractères).");
        }
        if (!InputValidator.isValidName(prenom)) {
            return Map.of("statut", "ERREUR", "message", "Prénom invalide (lettres, espaces et tirets uniquement, max 50 caractères).");
        }
        if (!InputValidator.isValidPassword(mdp)) {
            return Map.of("statut", "ERREUR", "message", "Le mot de passe doit contenir au moins 6 caractères.");
        }
        if (!InputValidator.isValidPhone(telephone)) {
            return Map.of("statut", "ERREUR", "message", "Numéro de téléphone invalide.");
        }

        // Assainir les champs texte avant insertion
        Map<String, Object> sanitizedReq = new HashMap<>(req);
        sanitizedReq.put("nom", InputValidator.sanitizeString(nom, 50));
        sanitizedReq.put("prenom", InputValidator.sanitizeString(prenom, 50));
        sanitizedReq.put("email", email.trim());

        Map<String, Object> result = UserDAO.inscrire(sanitizedReq);
        if (!"OK".equals(result.get("statut"))) return result;

        int idUtilisateur = (int) result.get("idUtilisateur");

        try {
            String token = TokenDAO.genererToken(idUtilisateur, "confirmation");
            EmailService.envoyerConfirmation(email, token);
            return Map.of(
                "statut",  "EN_ATTENTE",
                "message", "Inscription réussie ! Consultez votre boîte email pour confirmer votre compte."
            );
        } catch (Exception e) {
            System.err.println("[AUTH] Envoi email confirmation échoué : " + e.getMessage());
            return Map.of(
                "statut",  "EN_ATTENTE",
                "message", "Inscription enregistrée, mais l'email n'a pas pu être envoyé. Contactez le support."
            );
        }
    }

    // ─── Connexion ────────────────────────────────────────────────────────────

    public Map<String, Object> login(Map<String, Object> req) {
        // ─── Validation serveur des entrées ──────────────────────────────
        String email = (String) req.get("email");
        String mdp = (String) req.get("mdp");

        if (!InputValidator.isValidEmail(email)) {
            return Map.of("statut", "ERREUR", "message", "Format d'email invalide.");
        }
        if (mdp == null || mdp.isEmpty()) {
            return Map.of("statut", "ERREUR", "message", "Le mot de passe est requis.");
        }

        return UserDAO.connexion(req);
    }

    public Map<String, Object> verifierOTPConnexion(Map<String, Object> req) {
        String email = (String) req.get("email");
        String otp   = (String) req.get("otp");
        
        if (email == null || otp == null || otp.isBlank()) {
            return Map.of("statut", "ERREUR", "message", "L'email ou le code OTP est manquant.");
        }
        
        return UserDAO.verifierOTP(email, otp);
    }

    // ─── Confirmation email ───────────────────────────────────────────────────

    public Map<String, Object> confirmerEmail(Map<String, Object> req) {
        String token = (String) req.getOrDefault("token", "");
        if (token.isBlank()) return Map.of("statut", "ERREUR", "message", "Token manquant.");

        try {
            int idUtilisateur = TokenDAO.consommerToken(token, "confirmation");
            if (idUtilisateur == -1) {
                return Map.of("statut", "ERREUR", "message", "Code invalide ou expiré.");
            }
            return UserDAO.activerCompte(idUtilisateur);

        } catch (Exception e) {
            System.err.println("[AUTH] Erreur confirmation email : " + e.getMessage());
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        }
    }

    // ─── Mot de passe oublié ──────────────────────────────────────────────────

    public Map<String, Object> oublierMotDePasse(Map<String, Object> req) {
        String email = (String) req.getOrDefault("email", "");
        if (email.isBlank()) return Map.of("statut", "ERREUR", "message", "Email manquant.");

        // Toujours renvoyer OK même si l'email n'existe pas (anti-énumération)
        int idUtilisateur = UserDAO.findIdByEmail(email);
        if (idUtilisateur != -1) {
            try {
                String token = TokenDAO.genererToken(idUtilisateur, "reset_mdp");
                EmailService.envoyerReset(email, token);
            } catch (Exception e) {
                System.err.println("[AUTH] Erreur envoi reset mdp : " + e.getMessage());
            }
        }

        return Map.of(
            "statut",  "OK",
            "message", "Si cet email existe, vous recevrez un code de réinitialisation."
        );
    }

    // ─── Réinitialisation du mot de passe ─────────────────────────────────────

    public Map<String, Object> reinitialiserMotDePasse(Map<String, Object> req) {
        String token      = (String) req.getOrDefault("token", "");
        String nouveauMdp = (String) req.getOrDefault("nouveauMdp", ""); // Corrigé de nouveau_mdp -> nouveauMdp

        if (token.isBlank() || nouveauMdp.isBlank()) {
            return Map.of("statut", "ERREUR", "message", "Token ou nouveau mot de passe manquant.");
        }
        if (nouveauMdp.length() < 6) { // Ajusté 8 -> 6 pour cohérence avec inscription
            return Map.of("statut", "ERREUR", "message", "Le mot de passe doit contenir au moins 6 caractères.");
        }

        try {
            int idUtilisateur = TokenDAO.consommerToken(token, "reset_mdp");
            if (idUtilisateur == -1) {
                return Map.of("statut", "ERREUR", "message", "Code invalide ou expiré.");
            }
            return UserDAO.majMotDePasse(idUtilisateur, nouveauMdp);

        } catch (Exception e) {
            System.err.println("[AUTH] Erreur réinitialisation mdp : " + e.getMessage());
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        }
    }

    public Map<String, Object> getProfil(Map<String, Object> req) {
        int userId = (int) req.get("userId");
        return UserDAO.getInfosProfil(userId);
    }

    public Map<String, Object> updateProfil(Map<String, Object> req) {
        int userId = (int) req.get("userId");
        Map<String, Object> data = (Map<String, Object>) req.get("data");
        return UserDAO.majProfil(userId, data);
    }
}
