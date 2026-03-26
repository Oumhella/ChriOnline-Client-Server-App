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

    // ───── Lister toutes les commandes ─────
    public List<Commande> findAll() throws SQLException {
        List<Commande> commandes = new ArrayList<>();

        // On fait une jointure pour calculer le montant total qui n'est pas dans la table commande
        // et on utilise les noms de tables EXACTS du script (commande, ligne_commande, utilisateur)
        String sql = "SELECT c.id_commande, c.idUtilisateur, c.statut, c.date_commande, " +
                     "COALESCE(SUM(lc.quantite * lc.prix_unitaire), 0) AS montant_total " +
                     "FROM commande c " +
                     "LEFT JOIN ligne_commande lc ON c.id_commande = lc.id_commande " +
                     "GROUP BY c.id_commande, c.idUtilisateur, c.statut, c.date_commande " +
                     "ORDER BY c.date_commande DESC";

        PreparedStatement ps = connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Commande c = new Commande();
            c.setIdCommande(rs.getString("id_commande"));
            c.setIdUtilisateur(rs.getInt("idUtilisateur"));
            c.setMontantTotal(rs.getDouble("montant_total"));
            
            String statutDB = rs.getString("statut");
            if(statutDB != null) {
                try {
                    c.setStatut(StatutCommande.valueOf(statutDB.toUpperCase()));
                } catch(Exception e) {
                    c.setStatut(StatutCommande.EN_PREPARATION);
                }
            } else {
                c.setStatut(StatutCommande.EN_PREPARATION);
            }
            c.setDateCommande(rs.getTimestamp("date_commande").toLocalDateTime());
            commandes.add(c);
        }
        return commandes;
    }

    // ───── Trouver une commande par ID ─────
    public Commande findById(String idCommande) throws SQLException {
        String sql = "SELECT c.id_commande, c.idUtilisateur, c.statut, c.date_commande, " +
                     "COALESCE(SUM(lc.quantite * lc.prix_unitaire), 0) AS montant_total " +
                     "FROM commande c " +
                     "LEFT JOIN ligne_commande lc ON c.id_commande = lc.id_commande " +
                     "WHERE c.id_commande = ? " +
                     "GROUP BY c.id_commande, c.idUtilisateur, c.statut, c.date_commande";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, idCommande);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Commande c = new Commande();
            c.setIdCommande(rs.getString("id_commande"));
            c.setIdUtilisateur(rs.getInt("idUtilisateur"));
            c.setMontantTotal(rs.getDouble("montant_total"));
            
            String statutDB = rs.getString("statut");
            if(statutDB != null) {
                try {
                    c.setStatut(StatutCommande.valueOf(statutDB.toUpperCase()));
                } catch(Exception e) {
                    c.setStatut(StatutCommande.EN_PREPARATION);
                }
            }
            c.setDateCommande(rs.getTimestamp("date_commande").toLocalDateTime());
            return c;
        }
        return null;
    }

    // ───── Mettre à jour le statut ─────
    public boolean updateStatus(String idCommande, StatutCommande nouveauStatut)
            throws SQLException {
        String sql = "UPDATE commande SET statut = ? WHERE id_commande = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, nouveauStatut.name().toLowerCase());
        ps.setString(2, idCommande);
        return ps.executeUpdate() > 0;
    }
}