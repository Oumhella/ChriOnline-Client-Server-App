package com.chrionline.server;

import com.chrionline.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;

public class SchemaUpdater {
    public static void main(String[] args) {
        System.out.println("Début de la mise à jour du schéma de base de données...");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Mise à jour de la table utilisateur (ajout de la colonne public_key)
            try {
                stmt.execute("ALTER TABLE utilisateur ADD COLUMN public_key TEXT");
                System.out.println("Colonne 'public_key' ajoutée à la table 'utilisateur'.");
            } catch (Exception e) {
                System.out.println("La colonne 'public_key' existe peut-être déjà ou erreur: " + e.getMessage());
            }

            // 2. Mise à jour de la table categorie (ajout de la colonne discount)
            try {
                stmt.execute("ALTER TABLE categorie ADD COLUMN discount DOUBLE DEFAULT 0");
                System.out.println("Colonne 'discount' ajoutée à la table 'categorie'.");
            } catch (Exception e) {
                System.out.println("La colonne 'discount' existe peut-être déjà ou erreur: " + e.getMessage());
            }

            // 2. Recréation de la table security_blacklist avec le bon schéma
            try {
                stmt.execute("DROP TABLE IF EXISTS security_blacklist");
                System.out.println("Ancienne table 'security_blacklist' supprimée.");
                
                String sql = "CREATE TABLE IF NOT EXISTS security_blacklist (" +
                             "id INT AUTO_INCREMENT PRIMARY KEY, " +
                             "ip_address VARCHAR(45) NOT NULL, " +
                             "email VARCHAR(255), " +
                             "raison VARCHAR(255), " +
                             "date_ajout TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                             "expire_le TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                             "actif BOOLEAN NOT NULL DEFAULT TRUE)";
                stmt.execute(sql);
                System.out.println("Nouvelle table 'security_blacklist' créée.");
            } catch (Exception e) {
                System.out.println("Erreur lors de la recréation de 'security_blacklist': " + e.getMessage());
            }

            // 4. Ajout de la colonne totp_secret pour l'authentification 2FA TOTP
            try {
                stmt.execute("ALTER TABLE utilisateur ADD COLUMN totp_secret VARCHAR(64)");
                System.out.println("Colonne 'totp_secret' ajoutée à la table 'utilisateur'.");
            } catch (Exception e) {
                System.out.println("La colonne 'totp_secret' existe peut-être déjà ou erreur: " + e.getMessage());
            }

            System.out.println("Mise à jour terminée !");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
