package com.chrionline.server.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
  Journalisation centralisée des événements de sécurité.
  Tous les logs sont redirigés vers le logger "SECURITY" (→ logs/security.log).
 */
public final class SecurityLogger {

    private static final Logger LOG = LogManager.getRootLogger();

    private SecurityLogger() {}

    // Authentification

    /**
     * Connexion réussie.
     */
    public static void loginSucces(String email, String role, int userId, String ip) {
        LOG.info("[LOGIN_SUCCESS] email={} role={} userId={} ip={}", email, role, userId, ip);
    }

    /**
     * Email ou mot de passe incorrect (tentative ratée).
     */
    public static void loginEchec(String email, String ip) {
        LOG.warn("[LOGIN_FAILED]  email={} ip={}", email, ip);
    }

    /**
     * Tentative de connexion sur un compte explicitement bloqué par l'admin.
     */
    public static void compteBloque(String email, String ip) {
        LOG.warn("[COMPTE_BLOQUE] Connexion refusée – compte bloqué. email={} ip={}", email, ip);
    }

    /**
     * Tentative de connexion sur un compte non encore confirmé.
     */
    public static void compteNonActif(String email, String ip) {
        LOG.info("[COMPTE_INACTIF] Connexion refusée – email non confirmé. email={} ip={}", email, ip);
    }

    // ─── Changements de compte ────────────────────────────────────────────────

    /**
     * Modification du statut d'un compte client par un administrateur.
     */
    public static void changementStatutCompte(int adminId, int cibleId, String nouveauStatut) {
        LOG.warn("[STATUT_COMPTE] adminId={} a modifié le statut du compte userId={} → {}",
                adminId, cibleId, nouveauStatut);
    }

    /**
     * Réinitialisation du mot de passe via token.
     */
    public static void changementMotDePasse(int userId) {
        LOG.warn("[MAJ_MDP] Mot de passe réinitialisé pour userId={}", userId);
    }

    /**
     * Demande de réinitialisation du mot de passe (peut-être un attaquant).
     */
    public static void demandeResetMdp(String email, String ip) {
        LOG.info("[RESET_MDP_REQUEST] email={} ip={}", email, ip);
    }

    /**
     * Mise à jour du profil utilisateur.
     */
    public static void majProfil(int userId, String ip) {
        LOG.info("[MAJ_PROFIL] userId={} ip={}", userId, ip);
    }

    // ─── Accès non autorisés ──────────────────────────────────────────────────

    /**
     * Commande reçue d'un utilisateur sans le rôle requis.
     */
    public static void accesNonAutorise(String commande, int userId, String role, String ip) {
        LOG.warn("[ACCES_REFUSE]  commande={} userId={} role={} ip={}",
                commande, userId, role != null ? role : "INCONNU", ip);
    }

    /**
     * Erreur serveur dans un flux critique.
     */
    public static void erreurServeur(String contexte, String message) {
        LOG.error("[SERVER_ERROR]  contexte={} message={}", contexte, message);
    }
}
