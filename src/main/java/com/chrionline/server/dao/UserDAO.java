package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import com.chrionline.server.security.SecurityLogger;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Map;

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
     * Authentifie un utilisateur.
     * Retourne le rôle dans "data" pour permettre la redirection côté client.
     */
    public static Map<String, Object> connexion(Map<String, Object> data) {
        String email    = (String) data.get("email");
        String mdp      = (String) data.get("mdp");
        String clientIp = (String) data.getOrDefault("clientIp", "inconnue");

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {

            // ── Admin : on vérifie la présence de l'id dans la table `admin` ──
            String sqlAdmin = """
                SELECT u.idUtilisateur, u.nom, u.prenom, u.email, u.password
                FROM utilisateur u
                JOIN admin a ON a.idAdmin = u.idUtilisateur
                WHERE u.email = ?
            """;
            try (PreparedStatement ps = conn.prepareStatement(sqlAdmin)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (BCrypt.checkpw(mdp, rs.getString("password"))) {
                        int id = rs.getInt("idUtilisateur");
                        SecurityLogger.loginSucces(email, "admin", id, clientIp);
                        Map<String, Object> innerData = new java.util.HashMap<>();
                        innerData.put("userId",  id);
                        innerData.put("nom",     rs.getString("nom"));
                        innerData.put("prenom",  rs.getString("prenom"));
                        innerData.put("email",   rs.getString("email"));
                        innerData.put("role",    "admin");

                        Map<String, Object> rep = new java.util.HashMap<>();
                        rep.put("statut",  "OK");
                        rep.put("message", "Bienvenue, " + rs.getString("prenom") + " !");
                        rep.put("data", innerData);
                        return rep;
                    } else {
                        // Email admin reconnu mais mot de passe incorrect
                        SecurityLogger.loginEchec(email, clientIp);
                    }
                }
            }

            // ── Client : on vérifie la présence de l'id dans la table `client` ──
            String sqlClient = """
                SELECT u.idUtilisateur, u.nom, u.prenom, u.email, u.password, c.statut_compte
                FROM utilisateur u
                JOIN client c ON c.idUtilisateur = u.idUtilisateur
                WHERE u.email = ?
            """;
            try (PreparedStatement ps = conn.prepareStatement(sqlClient)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (BCrypt.checkpw(mdp, rs.getString("password"))) {
                        String statut = rs.getString("statut_compte");
                        if ("bloque".equals(statut)) {
                            SecurityLogger.compteBloque(email, clientIp);
                            Map<String, Object> rep = new java.util.HashMap<>();
                            rep.put("statut",  "ERREUR");
                            rep.put("message", "Votre compte a été bloqué par un administrateur.");
                            return rep;
                        } else if ("non actif".equals(statut)) {
                            SecurityLogger.compteNonActif(email, clientIp);
                            Map<String, Object> rep = new java.util.HashMap<>();
                            rep.put("statut",  "EN_ATTENTE");
                            rep.put("message", "Confirmez votre email avant de vous connecter.");
                            return rep;
                        }
                        int id = rs.getInt("idUtilisateur");
                        SecurityLogger.loginSucces(email, "client", id, clientIp);
                        Map<String, Object> innerData = new java.util.HashMap<>();
                        innerData.put("userId",  id);
                        innerData.put("nom",     rs.getString("nom"));
                        innerData.put("prenom",  rs.getString("prenom"));
                        innerData.put("email",   rs.getString("email"));
                        innerData.put("role",    "client");

                        Map<String, Object> rep = new java.util.HashMap<>();
                        rep.put("statut",  "OK");
                        rep.put("message", "Bienvenue, " + rs.getString("prenom") + " !");
                        rep.put("data", innerData);
                        return rep;
                    } else {
                        // Mot de passe incorrect pour un email client reconnu
                        SecurityLogger.loginEchec(email, clientIp);
                    }
                }
            }

            // Email introuvable dans les deux tables
            SecurityLogger.loginEchec(email, clientIp);
            Map<String, Object> rep = new java.util.HashMap<>();
            rep.put("statut",  "ERREUR");
            rep.put("message", "Email ou mot de passe incorrect.");
            return rep;

        } catch (Exception e) {
            SecurityLogger.erreurServeur("connexion", e.getMessage());
            Map<String, Object> rep = new java.util.HashMap<>();
            rep.put("statut",  "ERREUR");
            rep.put("message", "Erreur serveur : " + e.getMessage());
            return rep;
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
        String sql  = "UPDATE utilisateur SET password = ? WHERE idUtilisateur = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, idUtilisateur);
            ps.executeUpdate();
            SecurityLogger.changementMotDePasse(idUtilisateur);
            return Map.of("statut", "OK", "message", "Mot de passe réinitialisé avec succès !");
        } catch (Exception e) {
            SecurityLogger.erreurServeur("majMotDePasse userId=" + idUtilisateur, e.getMessage());
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
        return changerStatutCompte(-1, idUtilisateur, nouveauStatut);
    }

    public static Map<String, Object> changerStatutCompte(int adminId, int idUtilisateur, String nouveauStatut) {
        String sql = "UPDATE client SET statut_compte = ? WHERE idUtilisateur = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, idUtilisateur);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                SecurityLogger.changementStatutCompte(adminId, idUtilisateur, nouveauStatut);
                return Map.of("statut", "OK", "message", "Statut mis à jour avec succès.");
            } else {
                return Map.of("statut", "ERREUR", "message", "Client introuvable.");
            }
        } catch (Exception e) {
            SecurityLogger.erreurServeur("changerStatutCompte userId=" + idUtilisateur, e.getMessage());
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

    private static void rollback(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) {}
        }
    }
}