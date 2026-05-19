package com.chrionline.server.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.*;
import com.chrionline.database.DatabaseConnection;

/**
 * Journalisation centralisée des événements de sécurité.
 * Module IDS/IPS intégré : détection d'attaques par seuil, OTP suspects,
 * activité admin anormale, et actions correctives automatiques.
 */
public final class SecurityLogger {

    private static final Logger LOG = LogManager.getRootLogger();

    // Suivi des tentatives par IP : IP -> Liste de timestamps (ms)
    private static final Map<String, List<Long>> eventHistory = new ConcurrentHashMap<>();

    // Historique global des événements récents pour le dashboard
    private static final List<com.chrionline.shared.models.SecurityEvent> recentEvents = java.util.Collections
            .synchronizedList(new java.util.ArrayList<>());
    private static final int MAX_RECENT_EVENTS = 100;

    // Liste noire des IPs bloquées
    private static final java.util.Set<String> blacklistedIPs = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // IDS : 3 événements critiques en 60 secondes déclenchent une alerte
    private static final int THRESHOLD_COUNT = 3;
    private static final long THRESHOLD_WINDOW_MS = 60_000;


    // IDS Cas 3 : Suivi des lectures massives admin (email -> timestamps)
    private static final Map<String, List<Long>> adminReadHistory = new ConcurrentHashMap<>();
    private static final int ADMIN_MASSIVE_READ_THRESHOLD = 5;

    static {
        // Initialisation de la persistance au chargement de la classe
        ensureTableExists();
        loadBlacklistFromDB();
    }

    private SecurityLogger() {
    }

    /**
     * Entrée générique pour logger un événement et vérifier le seuil de sécurité.
     */
    public static void logSecurityEvent(String type, String email, String ip, String context) {
        String logEntry = String.format("[%s] email=%s ip=%s context=%s timestamp=%s",
                type, email, ip, context, Instant.now());

        // Chiffrement sélectif pour les événements de sécurité/sensibles
        String finalLogToStore = logEntry;
        if (type.contains("FAILED") || type.contains("REFUSE") || type.contains("SPOOF") 
                || type.contains("BLOQUE") || type.contains("ALERT") || type.contains("SUCCESS")) {
            
            // Appel de Vault Transit
            String ciphertext = com.chrionline.securite.VaultServerService.transitEncrypt(logEntry);
            
            // Si le chiffrement a réussi, on préfixe pour identifier le log chiffré
            if (ciphertext != null && ciphertext.startsWith("vault:")) {
                finalLogToStore = "[SECURE_ENCRYPTED] " + ciphertext;
            }
        }

        // S'assurer que l'IP n'est pas "inconnue"
        String displayIp = (ip == null || ip.isEmpty()) ? "127.0.0.1" : ip;

        // ── IDS : Alimenter le flux d'événements récents pour le Dashboard (en clair dans la RAM admin) ──
        addRecentEvent(type, displayIp, email + " | " + context);

        if (type.contains("FAILED") || type.contains("REFUSE") || type.contains("SPOOF") || type.contains("BLOQUE") || type.contains("ALERT")) {
            LOG.warn(finalLogToStore);
            checkThreshold(displayIp, type);
        } else {
            LOG.info(finalLogToStore);
        }
    }

    /**
     * Ajoute un événement dans la liste mémoire consultable par le dashboard admin.
     */
    private static void addRecentEvent(String type, String ip, String context) {
        recentEvents.add(0, new com.chrionline.shared.models.SecurityEvent(type, ip, context));
        if (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.remove(recentEvents.size() - 1);
        }
    }

    /**
     * Retourne les événements récents pour l'interface de supervision admin.
     */
    public static List<com.chrionline.shared.models.SecurityEvent> getRecentEvents() {
        return new ArrayList<>(recentEvents);
    }

    private static void checkThreshold(String ip, String type) {
        if (ip == null || "localhost".equals(ip) || "127.0.0.1".equals(ip))
            return;

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
        addRecentEvent("IDS_ALERT_BRUTE_FORCE", ip, "Seuil dépassé : " + count + " événements '" + type + "' en 60s");

        // ── IPS : Blocage automatique temporaire (15 min) ──
        if (!blacklistedIPs.contains(ip)) {
            com.chrionline.server.dao.SecurityBlacklistDAO.addIp(ip, "IDS", "IDS Auto-Ban : " + count + " alertes '" + type + "'", 15);
            blacklistedIPs.add(ip);
            LOG.error("[IPS] IP {} bloquée automatiquement pour 15 minutes suite à l'alerte IDS.", ip);
            addRecentEvent("IPS_AUTO_BAN", ip, "Blocage automatique 15min (" + type + ")");
        }
    }

    // --- Wrappers pour la compatibilité existante (Log simple uniquement) ---

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

    public static void ipSpoofingAttempt(String claimedIp, String socketIp) {
        logSecurityEvent("IP_SPOOF_ATTEMPT", "SYSTEM", socketIp, "Claimed IP: " + claimedIp);
    }

    public static void changementMotDePasse(int userId) {
        logSecurityEvent("MAJ_MDP", "ID:" + userId, "serveur", "Mot de passe réinitialisé");
    }

    public static void rawSecurityAlert(String type, String ip, String context) {
        logSecurityEvent(type, "GUEST", ip, context);
    }

    public static void majProfil(int userId, String ip) {
        logSecurityEvent("MAJ_PROFIL", "ID:" + userId, ip, "Mise à jour profil");
    }

    public static void changementStatutCompte(int adminId, int cibleId, String nouveauStatut) {
        logSecurityEvent("STATUT_COMPTE", "adminId:" + adminId, "serveur",
                "cibleId=" + cibleId + " nouveauStatut=" + nouveauStatut);
    }

    public static void accesNonAutorise(String commande, int userId, String role, String ip) {
        logSecurityEvent("ACCES_REFUSE", "ID:" + userId, ip, "commande=" + commande + " role=" + role);
    }


    /**
     * Enregistre un accès admin à des données utilisateurs.
     * Si > 5 lectures en < 1 minute, lève une alerte de consultation massive.
     */
    public static void trackAdminDataAccess(String email, String ip) {
        long now = System.currentTimeMillis();
        List<Long> timestamps = adminReadHistory.computeIfAbsent(email, k -> new ArrayList<>());
        synchronized (timestamps) {
            timestamps.removeIf(t -> now - t > THRESHOLD_WINDOW_MS);
            timestamps.add(now);
            if (timestamps.size() >= ADMIN_MASSIVE_READ_THRESHOLD) {
                logSecurityEvent("IDS_ALERT_ADMIN_MASSIVE_READ", email, ip,
                        "Consultation massive : " + timestamps.size() + " accès données en 60s");
            }
        }
    }

    public static void blockIP(String ip) {
        blockIP(ip, "Bannissement manuel admin");
    }

    public static void blockIP(String ip, String reason) {
        blacklistedIPs.add(ip);
        persistBan(ip, reason);
        // Alerte très visible en console pour le monitoring en temps réel
        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.err.println("[AUTO-BAN] IP BLACKLISTÉE DÉFINITIVEMENT : " + ip);
        System.err.println("[AUTO-BAN] Raison : " + reason);
        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        LOG.error("[AUTO-BAN] IP {} mise sur liste noire persistante. Raison : {}", ip, reason);
    }

    private static void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS security_blacklist (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "ip_address VARCHAR(45) NOT NULL, " +
                "email VARCHAR(255), " +
                "raison VARCHAR(255), " +
                "date_ajout TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "expire_le TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "actif BOOLEAN NOT NULL DEFAULT TRUE, " +
                "offense_count INT DEFAULT 1)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // Ajouter dynamiquement la colonne offense_count si elle n'existe pas (migration)
            try {
                stmt.execute("ALTER TABLE security_blacklist ADD COLUMN offense_count INT DEFAULT 1");
            } catch (SQLException ignored) {
                // Déjà existante
            }
        } catch (SQLException e) {
            LOG.error("Erreur création table blacklist : {}", e.getMessage());
        }
    }

    private static void loadBlacklistFromDB() {
        String sql = "SELECT ip_address FROM security_blacklist WHERE actif = TRUE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                blacklistedIPs.add(rs.getString("ip_address"));
            }
            LOG.info("[SECURITY] {} IPs chargées depuis la blacklist persistante.", blacklistedIPs.size());
        } catch (SQLException e) {
            LOG.error("Erreur chargement blacklist : {}", e.getMessage());
        }
    }

    public static long getBanDurationDays(int offenseCount) {
        return switch (offenseCount) {
            case 1  -> 1;    // 24h — 1ère détection
            case 2  -> 7;    // 7 jours — récidive
            case 3  -> 30;   // 30 jours
            default -> 90;   // 90 jours — plafond, jamais permanent
        };
    }

    private static int getOffenseCount(String ip) {
        String sql = "SELECT COUNT(*) FROM security_blacklist WHERE ip_address = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) + 1; // Le prochain ban est le count + 1
                }
            }
        } catch (SQLException e) {
            LOG.error("Erreur comptage récidives pour IP {} : {}", ip, e.getMessage());
        }
        return 1;
    }

    private static void persistBan(String ip, String reason) {
        int offenseCount = getOffenseCount(ip);
        long days = getBanDurationDays(offenseCount);
        
        String sql = "INSERT INTO security_blacklist (ip_address, raison, expire_le, actif, offense_count) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? DAY), TRUE, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, reason);
            ps.setLong(3, days);
            ps.setInt(4, offenseCount);
            ps.executeUpdate();
            LOG.info("[SECURITY] IP {} bannie pour {} jours (Palier récidive : #{}). Raison : {}", 
                    ip, days, offenseCount, reason);
        } catch (SQLException e) {
            LOG.error("Erreur persistance ban IP {} : {}", ip, e.getMessage());
        }
    }

    public static boolean isBlacklisted(String ip) {
        return blacklistedIPs.contains(ip);
    }

    /** Retire une IP de la blacklist mémoire (le DAO gère la BDD). */
    public static void unblockIP(String ip) {
        blacklistedIPs.remove(ip);
        LOG.info("[IPS] IP {} retirée de la blacklist mémoire.", ip);
        addRecentEvent("IPS_UNBLOCK", ip, "IP débloquée manuellement par admin");
    }

    public static void erreurServeur(String contexte, String message) {
        LOG.error("[SERVER_ERROR] contexte={} message={}", contexte, message);
    }
}
