package com.chrionline.server.security;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Intercepteur de sécurité minimaliste.
 * Implémente UNIQUEMENT la protection contre l'IP Spoofing et la Blacklist.
 * Aucune interférence avec la logique de connexion ou les tentatives de login.
 */
public class SecurityInterceptor {

    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
            "^(10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.).*");

    /**
     * Résultat d'une validation de requête.
     */
    public static class ValidationResult {
        private final String error;
        public ValidationResult(String error) { this.error = error; }
        public boolean isOk() { return error == null; }
        public String getError() { return error; }
    }

    /**
     * Valide une requête entrante selon des critères de réseau.
     */
    public static ValidationResult validateRequest(String commande, Map<String, Object> req, String socketIp) {
        
        // 1. Vérification Liste Noire (Blacklist)
        if (SecurityLogger.isBlacklisted(socketIp)) {
            SecurityLogger.rawSecurityAlert("REJET_BLACKLIST", socketIp, "Tentative d'accès depuis IP bannie");
            return new ValidationResult("ACCÈS BLOQUÉ : Votre adresse IP a été bannie par l'administrateur.");
        }

        // 2. Détection IP Spoofing
        // Protection contre une IP publique qui prétend être une IP privée interne.
        String claimedIp = (String) req.getOrDefault("claimedIp", socketIp);
        if (isPrivateIP(claimedIp) && !isPrivateIP(socketIp)) {
            SecurityLogger.ipSpoofingAttempt(claimedIp, socketIp);
            // AUTO-BAN : On bloque immédiatement l'IP de manière persistante
            SecurityLogger.blockIP(socketIp, "IP Spoofing détecté : prétend être " + claimedIp + " depuis " + socketIp);
            return new ValidationResult("ALERTE SÉCURITÉ : Tentative d'IP Spoofing détectée. Votre accès est définitivement bloqué.");
        }

        return new ValidationResult(null); // Tout est OK
    }

    private static boolean isPrivateIP(String ip) {
        if (ip == null) return false;
        return "127.0.0.1".equals(ip) || "localhost".equals(ip) || PRIVATE_IP_PATTERN.matcher(ip).matches();
    }
}
