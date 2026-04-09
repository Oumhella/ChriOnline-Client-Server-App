package com.chrionline.server.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Journalisation centralisée des événements de sécurité.
 * Gère désormais la détection d'attaques par seuil (Threshold detection).
 */
public final class SecurityLogger {

    private static final Logger LOG = LogManager.getRootLogger();
    
    // Suivi des tentatives par IP : IP -> Liste de timestamps (ms)
    private static final Map<String, List<Long>> eventHistory = new ConcurrentHashMap<>();
    
    // Historique global des événements récents pour le dashboard
    private static final List<com.chrionline.shared.models.SecurityEvent> recentEvents = 
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    private static final int MAX_RECENT_EVENTS = 100;

    // Liste noire des IPs bloquées
    private static final java.util.Set<String> blacklistedIPs = java.util.concurrent.ConcurrentHashMap.newKeySet();
    
    // Seuil : 5 événements critiques en 60 secondes
    private static final int THRESHOLD_COUNT = 5;
    private static final long THRESHOLD_WINDOW_MS = 60_000;

    private SecurityLogger() {}

    /**
     * Entrée générique pour logger un événement et vérifier le seuil de sécurité.
     */
    public static void logSecurityEvent(String type, String email, String ip, String context) {
        String logEntry = String.format("[%s] email=%s ip=%s context=%s timestamp=%s", 
                type, email, ip, context, Instant.now());
        
        // Ajouter à l'historique pour le dashboard
        com.chrionline.shared.models.SecurityEvent event = new com.chrionline.shared.models.SecurityEvent(type, ip, context);
        recentEvents.add(0, event);
        if (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.remove(recentEvents.size() - 1);
        }

        if (type.contains("FAILED") || type.contains("REFUSE") || type.contains("SPOOF") || type.contains("BLOQUE")) {
            LOG.warn(logEntry);
            checkThreshold(ip, type);
        } else {
            LOG.info(logEntry);
        }
    }

    private static void checkThreshold(String ip, String type) {
        if (ip == null || "localhost".equals(ip) || "127.0.0.1".equals(ip)) return;

        long now = System.currentTimeMillis();
        List<Long> timestamps = eventHistory.computeIfAbsent(ip, k -> new ArrayList<>());

        synchronized (timestamps) {
            // Nettoyer les anciens timestamps hors de la fenêtre (60s)
            timestamps.removeIf(t -> now - t > THRESHOLD_WINDOW_MS);
            timestamps.add(now);

            if (timestamps.size() >= THRESHOLD_COUNT) {
                sendSecurityAlert(ip, type, timestamps.size());
            }
        }
    }

    private static void sendSecurityAlert(String ip, String type, int count) {
        LOG.error("!!! ALERTE SÉCURITÉ !!! IP {} a déclenché {} alertes de type '{}' en moins d'une minute.", 
                ip, count, type);
        // TODO: Envoyer un email ou webhook Slack ici
    }

    // --- Wrappers pour la compatibilité existante ---

    public static void loginSucces(String email, String role, int userId, String ip) {
        logSecurityEvent("LOGIN_SUCCESS", email, ip, "role=" + role + " userId=" + userId);
    }

    public static void loginEchec(String email, String ip) {
        logSecurityEvent("LOGIN_FAILED", email, ip, "Tentative de connexion");
    }

    public static void compteBloque(String email, String ip) {
        logSecurityEvent("COMPTE_BLOQUE", email, ip, "Accès à un compte bloqué");
    }

    public static void compteNonActif(String email, String ip) {
        logSecurityEvent("COMPTE_INACTIF", email, ip, "Accès à un compte non confirmé");
    }

    public static void changementStatutCompte(int adminId, int cibleId, String nouveauStatut) {
        LOG.warn("[STATUT_COMPTE] adminId={} a modifié le statut du compte userId={} → {}",
                adminId, cibleId, nouveauStatut);
    }

    public static void changementMotDePasse(int userId) {
        LOG.warn("[MAJ_MDP] Mot de passe réinitialisé pour userId={}", userId);
    }

    public static void demandeResetMdp(String email, String ip) {
        logSecurityEvent("RESET_MDP_REQUEST", email, ip, "Demande de reset");
    }

    public static void majProfil(int userId, String ip) {
        logSecurityEvent("MAJ_PROFIL", "ID:" + userId, ip, "Mise à jour profil");
    }

    public static void accesNonAutorise(String commande, int userId, String role, String ip) {
        logSecurityEvent("ACCES_REFUSE", "ID:" + userId, ip, "commande=" + commande + " role=" + role);
    }

    public static void ipSpoofingAttempt(String claimedIp, String socketIp) {
        logSecurityEvent("IP_SPOOF_ATTEMPT", "SYSTEM", socketIp, "Claimed IP: " + claimedIp);
    }

    public static void rawSecurityAlert(String type, String ip, String context) {
        logSecurityEvent(type, "GUEST", ip, context);
    }

    public static List<com.chrionline.shared.models.SecurityEvent> getRecentEvents() {
        return new ArrayList<>(recentEvents);
    }

    public static void blockIP(String ip) {
        blacklistedIPs.add(ip);
        LOG.error("IP Bloquée manuellement : {}", ip);
    }

    public static boolean isBlacklisted(String ip) {
        return blacklistedIPs.contains(ip);
    }

    public static void erreurServeur(String contexte, String message) {
        LOG.error("[SERVER_ERROR] contexte={} message={}", contexte, message);
    }
}
