package com.chrionline.server.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Journalisation de sécurité dédiée (Security Audit Logging).
 *
 * Trace tous les événements critiques de sécurité dans un fichier séparé :
 * - Connexions TLS échouées
 * - Tentatives de login échouées
 * - Accès admin refusés
 * - Tokens JWT invalides ou expirés
 * - Tentatives de chiffrement/déchiffrement
 * - Événements mTLS (certificat client manquant ou invalide)
 *
 * Format : [timestamp] [SECURITY] [event_type] IP=x.x.x.x | User=xxx | Details=xxx
 */
public final class SecurityAuditLogger {

    private static final Logger AUDIT_LOG = LogManager.getLogger("SecurityAudit");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SecurityAuditLogger() {}

    // ─── ÉVÉNEMENTS DE CONNEXION ─────────────────────────────────────────────

    /**
     * Connexion TLS réussie.
     */
    public static void tlsConnectionSuccess(String clientIp, String tlsVersion) {
        log("TLS_CONNECT_OK", clientIp, "-", "Version=" + tlsVersion);
    }

    /**
     * Connexion TLS échouée (handshake raté, certificat invalide...).
     */
    public static void tlsConnectionFailed(String clientIp, String reason) {
        logWarn("TLS_CONNECT_FAIL", clientIp, "-", "Raison=" + reason);
    }

    /**
     * Connexion mTLS échouée (certificat client manquant ou invalide).
     */
    public static void mtlsAuthFailed(String clientIp, String reason) {
        logWarn("MTLS_AUTH_FAIL", clientIp, "-", "Raison=" + reason);
    }

    /**
     * Connexion mTLS réussie (admin authentifié par certificat).
     */
    public static void mtlsAuthSuccess(String clientIp, String certCN) {
        log("MTLS_AUTH_OK", clientIp, certCN, "Certificat client accepté");
    }

    // ─── ÉVÉNEMENTS D'AUTHENTIFICATION ───────────────────────────────────────

    /**
     * Tentative de login échouée.
     */
    public static void loginFailed(String email, String clientIp, String reason) {
        logWarn("LOGIN_FAIL", clientIp, email, "Raison=" + reason);
    }

    /**
     * Login réussi.
     */
    public static void loginSuccess(String email, String clientIp, String role) {
        log("LOGIN_OK", clientIp, email, "Role=" + role);
    }

    /**
     * Token JWT invalide ou expiré.
     */
    public static void invalidToken(String clientIp, String reason) {
        logWarn("INVALID_TOKEN", clientIp, "-", "Raison=" + reason);
    }

    /**
     * Session expirée.
     */
    public static void sessionExpired(String clientIp, String userId) {
        logWarn("SESSION_EXPIRED", clientIp, userId, "Session expirée ou invalidée");
    }

    // ─── ÉVÉNEMENTS D'AUTORISATION ───────────────────────────────────────────

    /**
     * Accès refusé à une ressource protégée.
     */
    public static void accessDenied(String command, String clientIp, String userId, String role) {
        logWarn("ACCESS_DENIED", clientIp, userId,
                "Commande=" + command + " | RoleActuel=" + role + " | RoleRequis=admin");
    }

    /**
     * Tentative d'IP Spoofing détectée.
     */
    public static void ipSpoofingDetected(String realIp, String claimedIp) {
        logError("IP_SPOOFING", realIp, "-",
                "IP revendiquée=" + claimedIp + " ≠ IP réelle=" + realIp);
    }

    // ─── ÉVÉNEMENTS DE CHIFFREMENT ───────────────────────────────────────────

    /**
     * Chiffrement de données de paiement.
     */
    public static void paymentEncrypted(String clientIp, String userId) {
        log("PAYMENT_ENCRYPT", clientIp, userId, "Données de paiement chiffrées (AES-256/GCM)");
    }

    /**
     * Déchiffrement de données de paiement.
     */
    public static void paymentDecrypted(String clientIp, String userId) {
        log("PAYMENT_DECRYPT", clientIp, userId, "Données de paiement déchiffrées");
    }

    /**
     * Erreur de chiffrement/déchiffrement.
     */
    public static void cryptoError(String operation, String clientIp, String error) {
        logError("CRYPTO_ERROR", clientIp, "-", "Operation=" + operation + " | Erreur=" + error);
    }

    // ─── ÉVÉNEMENTS RÉSEAU ───────────────────────────────────────────────────

    /**
     * Notification UDP envoyée avec HMAC.
     */
    public static void udpNotificationSent(String destIp, int destPort) {
        log("UDP_HMAC_SENT", destIp, "-", "Port=" + destPort);
    }

    /**
     * Vérification HMAC UDP échouée (possible injection de fausses notifications).
     */
    public static void udpHmacFailed(String sourceIp) {
        logWarn("UDP_HMAC_FAIL", sourceIp, "-", "Signature HMAC invalide — notification rejetée");
    }

    // ─── ÉVÉNEMENTS DE ROTATION ──────────────────────────────────────────────

    /**
     * Rotation de certificat SSL réussie.
     */
    public static void certRotationSuccess(String newExpiry) {
        log("CERT_ROTATION_OK", "SERVER", "-", "Nouveau certificat valide jusqu'à " + newExpiry);
    }

    /**
     * Rotation de certificat SSL échouée.
     */
    public static void certRotationFailed(String reason) {
        logError("CERT_ROTATION_FAIL", "SERVER", "-", "Raison=" + reason);
    }

    // ─── MÉTHODES INTERNES ───────────────────────────────────────────────────

    private static void log(String eventType, String ip, String user, String details) {
        AUDIT_LOG.info("[SECURITY] [{}] IP={} | User={} | {}",
                eventType, ip, user, details);
    }

    private static void logWarn(String eventType, String ip, String user, String details) {
        AUDIT_LOG.warn("[SECURITY] [{}] IP={} | User={} | {}",
                eventType, ip, user, details);
    }

    private static void logError(String eventType, String ip, String user, String details) {
        AUDIT_LOG.error("[SECURITY] [{}] IP={} | User={} | {}",
                eventType, ip, user, details);
    }
}
