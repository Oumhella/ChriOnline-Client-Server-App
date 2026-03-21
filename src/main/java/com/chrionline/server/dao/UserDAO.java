package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Map;

/**
 * Gère toutes les opérations BDD liées à l'inscription.
 * Insère en cascade : utilisateur → client → adresse
 */
public class UserDAO {

    /**
     * Inscrit un nouvel utilisateur en 3 étapes dans une transaction :
     * 1. INSERT utilisateur
     * 2. INSERT client (FK idUtilisateur)
     * 3. INSERT adresse (FK id_client) si adresse fournie
     *
     * @return Map avec "statut" OK/ERREUR et "message"
     */
    public static Map<String, Object> inscrire(Map<String, Object> data) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // ← TRANSACTION

            // ── Étape 1 : INSERT utilisateur ──────────────────
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
                System.out.println("[DAO] utilisateur créé — id=" + idUtilisateur);
            }

            // ── Étape 2 : INSERT client ────────────────────────
            String sqlClient = """
                INSERT INTO client (idUtilisateur, telephone, statut_compte)
                VALUES (?, ?, 'en_attente')
            """;

            int idClient;
            try (PreparedStatement ps = conn.prepareStatement(sqlClient, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idUtilisateur);

                String tel = (String) data.getOrDefault("telephone", "");
                ps.setString(2, tel.isBlank() ? null : tel);

                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new SQLException("Échec récupération id_client");
                idClient = keys.getInt(1);
                System.out.println("[DAO] client créé — id=" + idClient);
            }

            // ── Étape 3 : INSERT adresse  ─────────
            String rue  = (String) data.getOrDefault("rue", "");
            String ville = (String) data.getOrDefault("ville", "");
            String cp   = (String) data.getOrDefault("code_postal", "");
            String pays = (String) data.getOrDefault("pays", "Maroc");

            if (!rue.isBlank() && !ville.isBlank() && !cp.isBlank()) {
                String sqlAdresse = """
                    INSERT INTO adresse (id_client, type_adresse, rue, ville, code_postal, pays)
                    VALUES (?, 'livraison', ?, ?, ?, ?)
                """;
                try (PreparedStatement ps = conn.prepareStatement(sqlAdresse)) {
                    ps.setInt(1, idClient);
                    ps.setString(2, rue);
                    ps.setString(3, ville);
                    ps.setString(4, cp);
                    ps.setString(5, pays.isBlank() ? "Maroc" : pays);
                    ps.executeUpdate();
                    System.out.println("[DAO] adresse créée pour client id=" + idClient);
                }
            }

            // ── Tout OK → COMMIT ──────────────────────────────
            conn.commit();
            System.out.println("[DAO] ✓ Inscription complète pour " + data.get("email"));
            
            Map<String, Object> succes = new java.util.HashMap<>();
            succes.put("statut", "OK");
            succes.put("message", "Inscription réussie !");
            succes.put("idUtilisateur", idUtilisateur);
            return succes;

        } catch (SQLIntegrityConstraintViolationException e) {
            rollback(conn);
            System.err.println("[DAO] Email déjà utilisé : " + e.getMessage());
            return Map.of("statut", "ERREUR", "message", "Cet email est déjà utilisé.");

        } catch (Exception e) {
            rollback(conn);
            System.err.println("[DAO] Erreur inscription : " + e.getMessage());
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());

        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Authentifie un utilisateur par email et mot de passe.
     * @return Map avec "statut", "message" et éventuellement "data" (userId, nom, prenom)
     */
    public static Map<String, Object> connexion(Map<String, Object> data) {
        String email = (String) data.get("email");
        String mdp   = (String) data.get("mdp");

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            String sql = """
                SELECT u.*, c.statut_compte
                FROM utilisateur u
                JOIN client c ON c.idUtilisateur = u.idUtilisateur
                WHERE u.email = ?
            """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    if (BCrypt.checkpw(mdp, rs.getString("password"))) {
                        String statut = rs.getString("statut_compte");
                        if ("en_attente".equals(statut)) {
                            return Map.of("statut", "EN_ATTENTE", "message", "Confirmez votre email avant de vous connecter.");
                        }

                        System.out.println("[DAO] ✓ Connexion réussie pour " + email);
                        return Map.of(
                            "statut", "OK",
                            "message", "Bienvenue, " + rs.getString("prenom") + " !",
                            "data", Map.of(
                                "userId", rs.getInt("idUtilisateur"),
                                "nom",    rs.getString("nom"),
                                "prenom", rs.getString("prenom"),
                                "email",  rs.getString("email")
                            )
                        );
                    } else {
                        return Map.of("statut", "ERREUR", "message", "Email ou mot de passe incorrect.");
                    }
                } else {
                    return Map.of("statut", "ERREUR", "message", "Email ou mot de passe incorrect.");
                }
            }
        } catch (Exception e) {
            System.err.println("[DAO] Erreur connexion : " + e.getMessage());
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        }
    }

    private static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
                System.err.println("[DAO] Transaction annulée (rollback).");
            } catch (SQLException e) {
                System.err.println("[DAO] Erreur rollback : " + e.getMessage());
            }
        }
    }

    /**
     * Active le compte après validation du token email.
     */
    public static Map<String, Object> activerCompte(int idUtilisateur) {
        String sql = "UPDATE client SET statut_compte = 'actif' WHERE idUtilisateur = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.executeUpdate();
            System.out.println("[DAO] Compte activé — idUtilisateur=" + idUtilisateur);
            return Map.of("statut", "OK", "message", "Compte confirmé ! Vous pouvez vous connecter.");
        } catch (Exception e) {
            System.err.println("[DAO] Erreur activation : " + e.getMessage());
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        }
    }

    /**
     * Met à jour le mot de passe avec un nouveau hash BCrypt.
     */
    public static Map<String, Object> majMotDePasse(int idUtilisateur, String nouveauMdp) {
        String hash = BCrypt.hashpw(nouveauMdp, BCrypt.gensalt());
        String sql  = "UPDATE utilisateur SET password = ? WHERE idUtilisateur = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, idUtilisateur);
            ps.executeUpdate();
            System.out.println("[DAO] Mot de passe mis à jour — idUtilisateur=" + idUtilisateur);
            return Map.of("statut", "OK", "message", "Mot de passe réinitialisé avec succès !");
        } catch (Exception e) {
            System.err.println("[DAO] Erreur maj mdp : " + e.getMessage());
            return Map.of("statut", "ERREUR", "message", "Erreur serveur : " + e.getMessage());
        }
    }

    /**
     * Retrouve l'idUtilisateur à partir d'un email (pour le flux reset mdp).
     */
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
}