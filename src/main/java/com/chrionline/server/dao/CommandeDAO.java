package com.chrionline.server.dao;

import com.chrionline.shared.enums.StatutCommande;
import com.chrionline.shared.models.Commande;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeDAO {

    private Connection connection;

    public CommandeDAO(Connection connection) {
        this.connection = connection;
    }

    // ───── Lister toutes les commandes ─────────────────────────────────────────
    public List<Commande> findAll() throws SQLException {
        List<Commande> commandes = new ArrayList<>();

        String sql =
            "SELECT c.id_commande, c.reference, c.idUtilisateur, c.statut, c.date_commande, " +
            "COALESCE(SUM(lc.quantite * lc.prix_unitaire), 0) AS montant_total, " +
            "CONCAT(u.prenom, ' ', u.nom) AS nom_client " +
            "FROM commande c " +
            "JOIN utilisateur u ON c.idUtilisateur = u.idUtilisateur " +
            "LEFT JOIN ligne_commande lc ON c.id_commande = lc.id_commande " +
            "GROUP BY c.id_commande, c.reference, c.idUtilisateur, c.statut, c.date_commande, u.nom, u.prenom " +
            "ORDER BY c.date_commande DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Commande c = new Commande();
                c.setIdCommande(rs.getString("id_commande"));
                c.setReference(rs.getString("reference"));
                c.setIdUtilisateur(rs.getInt("idUtilisateur"));
                c.setNomClient(rs.getString("nom_client"));
                c.setMontantTotal(rs.getDouble("montant_total"));

                String statutDB = rs.getString("statut");
                try {
                    c.setStatut(StatutCommande.valueOf(
                        statutDB != null ? statutDB.toUpperCase() : "EN_PREPARATION"));
                } catch (Exception e) {
                    c.setStatut(StatutCommande.EN_PREPARATION);
                }

                Timestamp ts = rs.getTimestamp("date_commande");
                if (ts != null) c.setDateCommande(ts.toLocalDateTime());
                commandes.add(c);
            }
        }
        return commandes;
    }

    // ───── Trouver une commande par ID ──────────────────────────────────────────
    public Commande findById(String idCommande) throws SQLException {
        String sql =
            "SELECT c.id_commande, c.reference, c.idUtilisateur, c.statut, c.date_commande, " +
            "COALESCE(SUM(lc.quantite * lc.prix_unitaire), 0) AS montant_total, " +
            "CONCAT(u.prenom, ' ', u.nom) AS nom_client " +
            "FROM commande c " +
            "JOIN utilisateur u ON c.idUtilisateur = u.idUtilisateur " +
            "LEFT JOIN ligne_commande lc ON c.id_commande = lc.id_commande " +
            "WHERE c.id_commande = ? " +
            "GROUP BY c.id_commande, c.reference, c.idUtilisateur, c.statut, c.date_commande, u.nom, u.prenom";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idCommande)); // id_commande est INT en BDD
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Commande c = new Commande();
                    c.setIdCommande(rs.getString("id_commande"));
                    c.setReference(rs.getString("reference"));
                    c.setIdUtilisateur(rs.getInt("idUtilisateur"));
                    c.setNomClient(rs.getString("nom_client"));
                    c.setMontantTotal(rs.getDouble("montant_total"));

                    String statutDB = rs.getString("statut");
                    try {
                        c.setStatut(StatutCommande.valueOf(
                            statutDB != null ? statutDB.toUpperCase() : "EN_PREPARATION"));
                    } catch (Exception e) {
                        c.setStatut(StatutCommande.EN_PREPARATION);
                    }

                    Timestamp ts = rs.getTimestamp("date_commande");
                    if (ts != null) c.setDateCommande(ts.toLocalDateTime());
                    return c;
                }
            }
        }
        return null;
    }

    // ───── Mettre à jour le statut ──────────────────────────────────────────────
    public boolean updateStatus(String idCommande, StatutCommande nouveauStatut)
            throws SQLException {
        String sql = "UPDATE commande SET statut = ? WHERE id_commande = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut.name().toLowerCase()); // stocké en minuscule en BDD
            ps.setInt(2, Integer.parseInt(idCommande));           // id_commande est INT
            return ps.executeUpdate() > 0;
        }
    }
}