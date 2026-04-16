package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Map;
import java.security.SecureRandom;

/**
 * Gère toutes les opérations BDD liées aux utilisateurs.
 */
public class UserDAO {

    /**
     * Inscrit un nouvel utilisateur en transaction :
     * utilisateur → client → adresse
     */
    public static Map<String, Object> inscrire(Map<String, Object> data) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            String sqlUser = """
                INSERT INTO utilisateur (nom, prenom, email, password)
                VALUES (?, ?, ?, ?)
            """;
            String hash = BCrypt.hashpw((String) data.get("mdp"), BCrypt.gensalt());

            int idUtilisateur;
            try (PreparedStatement ps = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, (String) data.get("nom"));
                ps.setString(2, (String) data.get("prenom"));
                ps.setString(3, (String) data.get("email"));
                ps.setString(4, hash);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new SQLException("Échec récupération idUtilisateur");
                idUtilisateur = keys.getInt(1);
            }

            String sqlClient = """
                INSERT INTO client (idUtilisateur, telephone, statut_compte)
                VALUES (?, ?, 'non actif')
            """;
            try (PreparedStatement ps = conn.prepareStatement(sqlClient)) {
                ps.setInt(1, idUtilisateur);
                String tel = (String) data.getOrDefault("telephone", "");
                ps.setString(2, tel.isBlank() ? null : tel);
                ps.executeUpdate();
            }

            String rue   = (String) data.getOrDefault("rue", "");
            String ville = (String) data.getOrDefault("ville", "");
            String cp    = (String) data.getOrDefault("code_postal", "");
            String pays  = (String) data.getOrDefault("pays", "Maroc");

            if (!rue.isBlank() && !ville.isBlank() && !cp.isBlank()) {
                String sqlAdresse = """
                    INSERT INTO adresse (idUtilisateur, type_adresse, rue, ville, code_postal, pays)
                    VALUES (?, 'livraison', ?, ?, ?, ?)
                """;
                try (PreparedStatement ps = conn.prepareStatement(sqlAdresse)) {
                    ps.setInt(1, idUtilisateur);
                    ps.setString(2, rue);
                    ps.setString(3, ville);
                    ps.setString(4, cp);
                    ps.setString(5, pays.isBlank() ? "Maroc" : pays);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            Map<String, Object> succes = new java.util.HashMap<>();
            succes.put("statut", "OK");
            succes.put("message", "Inscription réussie !");
            succes.put("idUtilisateur", idUtilisateur);
            return succes;

        } catch (SQLIntegrityConstraintViolationException e) {
            rollback(conn);
            return Map.of("statut", "ERREUR", "message", "Cet email est déjà utilisé.");
        } catch (Exception e) {
            rollback(conn);
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Authentifie un utilisateur (Etape 1: Verification + OTP).
     * Retourne REQUIRES_2FA pour déclencher l'authentification à deux facteurs.
     */
    public static Map<String, Object> connexion(Map<String, Object> data) {
        String email    = (String) data.get("email");
        String mdp      = (String) data.get("mdp");
        String clientIp = (String) data.getOrDefault("clientIp", "unknown");

        // ── 0. Vérification liste noire IP ────────────────────────────────────
        if (SecurityBlacklistDAO.isIpBlacklisted(clientIp)) {
            long remaining = SecurityBlacklistDAO.getRemainingSeconds(clientIp);
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("statut", "ERREUR_BLOQUE");
            res.put("message", "Votre adresse IP est temporairement suspendue suite à trop de tentatives.");
            res.put("delaySeconds", remaining);
            res.put("showPasswordRecovery", true);
            return res;
        }

        // Requête unifiée pour récupérer l'utilisateur, l'état de son compte, et son rôle
        String sql = """
            SELECT u.*,
                   c.statut_compte,
                   CASE WHEN a.idAdmin IS NOT NULL THEN 'admin' ELSE 'client' END as role
            FROM utilisateur u
            LEFT JOIN client c ON u.idUtilisateur = c.idUtilisateur
            LEFT JOIN admin a ON u.idUtilisateur = a.idAdmin
            WHERE u.email = ?
        """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return Map.of("statut", "ERREUR", "message", "Email ou mot de passe incorrect.");
            }

            int       idUtilisateur = rs.getInt("idUtilisateur");
            boolean   accountLocked = rs.getBoolean("account_locked");
            Timestamp lockTime      = rs.getTimestamp("lock_time");
            int       failedAttempts= rs.getInt("failed_attempts");
            String    role          = rs.getString("role");
            String    statutCompte  = rs.getString("statut_compte");

            // ── 1. Compte temporairement verrouillé ? ─────────────────────────
            if (accountLocked && lockTime != null) {
                int  dureeMinutes    = calculerDureeBlocage(failedAttempts);
                long tempsEcoule     = System.currentTimeMillis() - lockTime.getTime();
                long tempsRestantSec = (long)(dureeMinutes * 60) - (tempsEcoule / 1000);

                if (tempsRestantSec > 0) {
                    Map<String, Object> res = new java.util.HashMap<>();
                    res.put("statut", "ERREUR_BLOQUE");
                    res.put("message", "Compte temporairement suspendu.");
                    res.put("delaySeconds", tempsRestantSec);
                    // Afficher la récupération de mdp dès 6 tentatives
                    if (failedAttempts >= 6) res.put("showPasswordRecovery", true);
                    return res;
                }
                // Délai expiré → on autorise une tentative (palier conservé)
            }

            // ── 2. Bloques admin / compte non actif ───────────────────────────
            if ("client".equals(role)) {
                if ("bloque".equals(statutCompte)) {
                    return Map.of("statut", "ERREUR", "message",
                            "Votre compte a été bloqué par un administrateur.");
                } else if ("non actif".equals(statutCompte)) {
                    return Map.of("statut", "EN_ATTENTE", "message",
                            "Confirmez votre email avant de vous connecter.");
                }
            }

            // ── 3. Vérification du mot de passe ──────────────────────────────
            if (!BCrypt.checkpw(mdp, rs.getString("password"))) {
                failedAttempts++;

                if (failedAttempts % 3 == 0) {
                    // Palier atteint → verrouillage compte
                    bloquerCompte(conn, idUtilisateur, failedAttempts);
                    int dureeMinutes = calculerDureeBlocage(failedAttempts);

                    // À partir de 6 échecs : ajouter l'IP en liste noire
                    boolean ajouterBlacklist = failedAttempts >= 6 && !"unknown".equals(clientIp);
                    if (ajouterBlacklist) {
                        SecurityBlacklistDAO.addIp(clientIp, email,
                                failedAttempts + " échecs de connexion", dureeMinutes);
                    }

                    Map<String, Object> res = new java.util.HashMap<>();
                    res.put("statut", "ERREUR_BLOQUE");
                    res.put("message", "Compte bloqué suite à " + failedAttempts + " échecs.");
                    res.put("delaySeconds", (long) dureeMinutes * 60);
                    if (failedAttempts >= 6) res.put("showPasswordRecovery", true);
                    return res;

                } else {
                    incrementerTentatives(conn, idUtilisateur, failedAttempts);
                    int restantes = 3 - (failedAttempts % 3);
                    return Map.of("statut", "ERREUR",
                            "message", "Mot de passe incorrect. Tentatives restantes : " + restantes);
                }
            }

            // ── 4. Succès : réinitialisation + OTP ───────────────────────────
            reinitialiserTentatives(conn, idUtilisateur);
            SecurityBlacklistDAO.unlockIp(clientIp); // débloquer l'IP si nécessaire

            String otpCode = genererOTP();
            sauvegarderOTP(conn, idUtilisateur, otpCode);

            try {
                com.chrionline.server.service.EmailService.envoyerOTP2FA(email, otpCode);
            } catch (Exception e) {
                return Map.of("statut", "ERREUR",
                        "message", "Erreur lors de l'envoi de l'email OTP. Veuillez réessayer.");
            }

            return Map.of("statut", "REQUIRES_2FA",
                    "message", "Un code à 6 chiffres vous a été envoyé par email.");

        } catch (Exception e) {
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        }
    }

    /**
     * ÉTAPE 2 : Vérification du code OTP pour finaliser la connexion
     */
    public static Map<String, Object> verifierOTP(String email, String otpCodeSaisi) {
        String sql = """
            SELECT u.*, 
                   CASE WHEN a.idAdmin IS NOT NULL THEN 'admin' ELSE 'client' END as role
            FROM utilisateur u
            LEFT JOIN admin a ON u.idUtilisateur = a.idAdmin
            WHERE u.email = ?
        """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String dbOtpCode = rs.getString("otp_code");
                Timestamp otpExpiry = rs.getTimestamp("otp_expiry");

                // Vérifier si le code correspond et n'est pas expiré
                if (dbOtpCode != null && dbOtpCode.equals(otpCodeSaisi)) {
                    if (otpExpiry != null && otpExpiry.getTime() > System.currentTimeMillis()) {
                        
                        // OTP Valide : on nettoie les colonnes OTP pour des raisons de sécurité
                        nettoyerOTP(conn, rs.getInt("idUtilisateur"));

                        // On construit l'objet de connexion finale
                        Map<String, Object> innerData = new java.util.HashMap<>();
                        innerData.put("userId", rs.getInt("idUtilisateur"));
                        innerData.put("nom", rs.getString("nom"));
                        innerData.put("prenom", rs.getString("prenom"));
                        innerData.put("email", rs.getString("email"));
                        innerData.put("role", rs.getString("role"));

                        return Map.of(
                            "statut", "OK", 
                            "message", "Authentification validée !", 
                            "data", innerData
                        );
                    } else {
                        return Map.of("statut", "ERREUR", "message", "Le code OTP a expiré (limite de 5 minutes).");
                    }
                }
            }
            return Map.of("statut", "ERREUR", "message", "Code OTP invalide.");

        } catch (Exception e) {
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        }
    }

    private static String genererOTP() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // Génère entre 100000 et 999999
        return String.valueOf(otp);
    }

    private static void sauvegarderOTP(Connection conn, int idUtilisateur, String otp) throws Exception {
        Timestamp expiry = new Timestamp(System.currentTimeMillis() + (5 * 60 * 1000));
        String sql = "UPDATE utilisateur SET otp_code = ?, otp_expiry = ? WHERE idUtilisateur = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, otp);
            ps.setTimestamp(2, expiry);
            ps.setInt(3, idUtilisateur);
            ps.executeUpdate();
        }
    }

    private static void nettoyerOTP(Connection conn, int idUtilisateur) throws Exception {
        String sql = "UPDATE utilisateur SET otp_code = NULL, otp_expiry = NULL WHERE idUtilisateur = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.executeUpdate();
        }
    }

    private static int calculerDureeBlocage(int failedAttempts) {
        if (failedAttempts >= 12) return 1440; // 24 heures
        if (failedAttempts >= 9)  return 60;   // 1 heure
        if (failedAttempts >= 6)  return 15;   // 15 minutes
        if (failedAttempts >= 3)  return 1;    // 1 minute
        return 0;
    }

    private static void incrementerTentatives(Connection conn, int idUtilisateur, int failedAttempts) throws Exception {
        String sql = "UPDATE utilisateur SET failed_attempts = ? WHERE idUtilisateur = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, failedAttempts);
            ps.setInt(2, idUtilisateur);
            ps.executeUpdate();
        }
    }

    private static void bloquerCompte(Connection conn, int idUtilisateur, int failedAttempts) throws Exception {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String sql = "UPDATE utilisateur SET failed_attempts = ?, account_locked = true, lock_time = ? WHERE idUtilisateur = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, failedAttempts);
            ps.setTimestamp(2, now);
            ps.setInt(3, idUtilisateur);
            ps.executeUpdate();
        }
    }

    private static void reinitialiserTentatives(Connection conn, int idUtilisateur) throws Exception {
        String sql = "UPDATE utilisateur SET failed_attempts = 0, account_locked = false, lock_time = NULL WHERE idUtilisateur = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.executeUpdate();
        }
    }

    public static Map<String, Object> activerCompte(int idUtilisateur) {
        String sql = "UPDATE client SET statut_compte = 'actif' WHERE idUtilisateur = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.executeUpdate();
            return Map.of("statut", "OK", "message", "Compte confirmé ! Vous pouvez vous connecter.");
        } catch (Exception e) {
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        }
    }

    public static Map<String, Object> majMotDePasse(int idUtilisateur, String nouveauMdp) {
        String hash = BCrypt.hashpw(nouveauMdp, BCrypt.gensalt());
        // Réinitialiser le mdp et déverrouiller le compte
        String sql  = "UPDATE utilisateur SET password = ?, failed_attempts = 0, account_locked = false, lock_time = NULL WHERE idUtilisateur = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, idUtilisateur);
            ps.executeUpdate();
            
            // Débloquer l'IP concernée en liste noire
            SecurityBlacklistDAO.unlockIpByUserId(idUtilisateur);
            
            return Map.of("statut", "OK", "message", "Mot de passe réinitialisé avec succès !");
        } catch (Exception e) {
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        }
    }

    public static int findIdByEmail(String email) {
        String sql = "SELECT idUtilisateur FROM utilisateur WHERE email = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("idUtilisateur") : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static Map<String, Object> getInfosProfil(int userId) {
        String sql = """
            SELECT u.nom, u.prenom, u.email, c.telephone, a.rue, a.ville, a.code_postal, a.pays
            FROM utilisateur u
            LEFT JOIN client c ON u.idUtilisateur = c.idUtilisateur
            LEFT JOIN adresse a ON u.idUtilisateur = a.idUtilisateur AND a.type_adresse = 'livraison'
            WHERE u.idUtilisateur = ?
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("nom", rs.getString("nom"));
                data.put("prenom", rs.getString("prenom"));
                data.put("email", rs.getString("email"));
                data.put("telephone", rs.getString("telephone"));
                data.put("rue", rs.getString("rue"));
                data.put("ville", rs.getString("ville"));
                data.put("code_postal", rs.getString("code_postal"));
                data.put("pays", rs.getString("pays"));
                return Map.of("statut", "OK", "data", data);
            }
            return Map.of("statut", "ERREUR", "message", "Utilisateur non trouvé.");
        } catch (Exception e) {
            return Map.of("statut", "ERREUR", "message", "Erreur : " + e.getMessage());
        }
    }

    public static Map<String, Object> majProfil(int userId, Map<String, Object> data) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1. Utilisateur
            String sqlU = "UPDATE utilisateur SET nom = ?, prenom = ?, email = ? WHERE idUtilisateur = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlU)) {
                ps.setString(1, (String) data.get("nom"));
                ps.setString(2, (String) data.get("prenom"));
                ps.setString(3, (String) data.get("email"));
                ps.setInt(4, userId);
                ps.executeUpdate();
            }

            // 2. Client (téléphone)
            String sqlC = "UPDATE client SET telephone = ? WHERE idUtilisateur = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlC)) {
                String tel = (String) data.getOrDefault("telephone", "");
                ps.setString(1, tel.isBlank() ? null : tel);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }

            // 3. Adresse (UPSERT / simple update si existe)
            String sqlA = "UPDATE adresse SET rue = ?, ville = ?, code_postal = ?, pays = ? WHERE idUtilisateur = ? AND type_adresse = 'livraison'";
            try (PreparedStatement ps = conn.prepareStatement(sqlA)) {
                ps.setString(1, (String) data.get("rue"));
                ps.setString(2, (String) data.get("ville"));
                ps.setString(3, (String) data.get("code_postal"));
                ps.setString(4, (String) data.get("pays"));
                ps.setInt(5, userId);
                int rows = ps.executeUpdate();
                
                // Si pas d'adresse, on l'insère
                if (rows == 0 && data.get("rue") != null && !((String)data.get("rue")).isBlank()) {
                    String sqlIns = "INSERT INTO adresse (idUtilisateur, type_adresse, rue, ville, code_postal, pays) VALUES (?, 'livraison', ?, ?, ?, ?)";
                    try (PreparedStatement psi = conn.prepareStatement(sqlIns)) {
                        psi.setInt(1, userId);
                        psi.setString(2, (String) data.get("rue"));
                        psi.setString(3, (String) data.get("ville"));
                        psi.setString(4, (String) data.get("code_postal"));
                        psi.setString(5, (String) data.get("pays"));
                        psi.executeUpdate();
                    }
                }
            }

            conn.commit();
            return Map.of("statut", "OK", "message", "Profil mis à jour avec succès !");
        } catch (Exception e) {
            rollback(conn);
            return Map.of("statut", "ERREUR", "message", "Échec mise à jour : " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        }
    }

    public static java.util.List<Map<String, Object>> listerClients() {
        java.util.List<Map<String, Object>> clients = new java.util.ArrayList<>();
        String sql = """
            SELECT u.idUtilisateur, u.nom, u.prenom, u.email, c.statut_compte 
            FROM utilisateur u 
            JOIN client c ON u.idUtilisateur = c.idUtilisateur
            ORDER BY u.idUtilisateur DESC
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> cli = new java.util.HashMap<>();
                cli.put("idUtilisateur", rs.getInt("idUtilisateur"));
                cli.put("nom", rs.getString("nom"));
                cli.put("prenom", rs.getString("prenom"));
                cli.put("email", rs.getString("email"));
                cli.put("statut_compte", rs.getString("statut_compte"));
                clients.add(cli);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clients;
    }

    public static Map<String, Object> changerStatutCompte(int idUtilisateur, String nouveauStatut) {
        String sql = "UPDATE client SET statut_compte = ? WHERE idUtilisateur = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut); // 'actif' ou 'non actif'
            ps.setInt(2, idUtilisateur);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                return Map.of("statut", "OK", "message", "Statut mis à jour avec succès.");
            } else {
                return Map.of("statut", "ERREUR", "message", "Client introuvable.");
            }
        } catch (Exception e) {
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        }
    }

    public static java.util.List<String> getAllEmails() {
        java.util.List<String> emails = new java.util.ArrayList<>();
        String sql = "SELECT email FROM utilisateur";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                emails.add(rs.getString("email"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return emails;
    }

    public static java.util.List<String> getAllAdminEmails() {
        java.util.List<String> emails = new java.util.ArrayList<>();
        String sql = """
            SELECT u.email 
            FROM utilisateur u 
            JOIN admin a ON a.idAdmin = u.idUtilisateur
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                emails.add(rs.getString("email"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return emails;
    }

    /**
     * Email et rôle pour synchroniser le {@link com.chrionline.server.core.ClientHandler} après validation de session.
     *
     * @return {@code [email, role]} ou {@code null} si introuvable
     */
    public static String[] getEmailAndRoleById(int userId) {
        String sql = """
            SELECT u.email,
                   CASE WHEN a.idAdmin IS NOT NULL THEN 'admin' ELSE 'client' END AS role
            FROM utilisateur u
            LEFT JOIN admin a ON a.idAdmin = u.idUtilisateur
            WHERE u.idUtilisateur = ?
            """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[] { rs.getString("email"), rs.getString("role") };
                }
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] getEmailAndRoleById : " + e.getMessage());
        }
        return null;
    }

    private static void rollback(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) {}
        }
    }
}