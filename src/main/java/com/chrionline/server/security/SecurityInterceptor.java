package com.chrionline.server.security;

import java.util.Map;
import java.util.Properties;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Intercepteur de sécurité pour valider les requêtes TCP.
 * Remplace les "Filters" de Spring Boot dans notre architecture Custom TCP.
 */
public class SecurityInterceptor {

    private static String FIREWALL_TOKEN;
    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
        "^(10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.).*"
    );

    static {
        try (InputStream in = SecurityInterceptor.class.getClassLoader().getResourceAsStream("server.properties")) {
            Properties props = new Properties();
            if (in != null) props.load(in);
            FIREWALL_TOKEN = props.getProperty("security.firewall.token", "UNSET_TOKEN");
        } catch (Exception e) {
            FIREWALL_TOKEN = "ERROR_TOKEN";
        }
    }

    /**
     * Valide une requête entrante selon plusieurs critères de sécurité.
     * @return null si OK, sinon un message d'erreur.
     */
    public static String validateRequest(String commande, Map<String, Object> req, String socketIp) {
        
        // 0. Vérification Liste Noire (Blacklist)
        if (SecurityLogger.isBlacklisted(socketIp)) {
            SecurityLogger.rawSecurityAlert("REJET_BLACKLIST", socketIp, "Tentative d'accès depuis IP bannie");
            return "ACCÈS BLOQUÉ : Votre adresse IP a été bannie par l'administrateur.";
        }

        // 1. Détection IP Spoofing
        String claimedIp = (String) req.getOrDefault("claimedIp", socketIp);
        if (isPrivateIP(claimedIp) && !isPrivateIP(socketIp)) {
            SecurityLogger.ipSpoofingAttempt(claimedIp, socketIp);
            return "ALERTE SÉCURITÉ : Tentative d'IP Spoofing détectée.";
        }

        // 2. Vérification Token Pare-feu (pour les IPs internes)
        if (isPrivateIP(socketIp)) {
            String token = (String) req.getOrDefault("firewallToken", "");
            if (!FIREWALL_TOKEN.equals(token)) {
                SecurityLogger.rawSecurityAlert("INVALID_FIREWALL_TOKEN", socketIp, "Token manquant ou incorrect");
                return "ACCÈS REFUSÉ : Token de pare-feu requis pour les accès internes.";
            }
        }

        // 3. Rate Limiting
        boolean isSensitive = isSensitiveCommand(commande);
        boolean allowed = isSensitive ? 
                RateLimiterService.allowSensitiveRequest(socketIp) : 
                RateLimiterService.allowGeneralRequest(socketIp);
        
        if (!allowed) {
            SecurityLogger.rawSecurityAlert("RATE_LIMIT_EXCEEDED", socketIp, "Commande: " + commande);
            return "TROP DE REQUÊTES : Veuillez patienter avant de réessayer.";
        }

        // 4. Vérification JWT pour commandes sensibles (sauf CONNEXION/INSCRIPTION)
        if (isSensitive && !"CONNEXION".equals(commande) && !"INSCRIPTION".equals(commande)) {
            String jwt = (String) req.getOrDefault("jwt", "");
            try {
                JwtService.validateToken(jwt);
            } catch (Exception e) {
                SecurityLogger.rawSecurityAlert("INVALID_JWT", socketIp, "Erreur validation JWT: " + e.getMessage());
                return "ACCÈS NON AUTORISÉ : Session invalide ou expirée.";
            }
        }

        return null; // Tout est OK
    }

    private static boolean isPrivateIP(String ip) {
        if (ip == null) return false;
        return "127.0.0.1".equals(ip) || "localhost".equals(ip) || PRIVATE_IP_PATTERN.matcher(ip).matches();
    }

    private static boolean isSensitiveCommand(String cmd) {
        return cmd.equals("CONNEXION") || cmd.equals("INSCRIPTION") || 
               cmd.equals("PANIER_VALIDER") || cmd.startsWith("ADMIN_") ||
               cmd.equals("UPDATE_PROFIL") || cmd.equals("REINITIALISER_MDP");
    }
}
