package com.chrionline.database;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DatabaseConnection {

    // ─── Configuration ────────────────────────────────────────────────────────
    private static final String HOST     = "localhost";
    private static final int    PORT     = 3306;
    private static final String DATABASE = "chrionline";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static final String URL = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8",
            HOST, PORT, DATABASE
    );
    // ──────────────────────────────────────────────────────────────────────────

    private static DatabaseConnection instance;
    private Connection connection;

    /** Constructeur privé : charge le driver et ouvre la connexion. */
    private DatabaseConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DB] Connexion à 'chrionline' établie avec succès.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Driver MySQL introuvable.");
            e.printStackTrace();
            throw new SQLException("Driver MySQL introuvable : " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DB] Échec de connexion à la base 'chrionline'. Vérifiez que MySQL est lancé et que la base existe.");
            e.printStackTrace();
            throw e;
        }
    }


    public static synchronized DatabaseConnection getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseConnection();
        } else {
            try {
                // isValid(2) envoie un ping au serveur MySQL — détecte une connexion zombie
                if (!instance.connection.isValid(2)) {
                    System.out.println("[DB] Connexion invalide, reconnexion...");
                    instance = new DatabaseConnection();
                }
            } catch (SQLException e) {
                instance = new DatabaseConnection();
            }
        }
        return instance;
    }


    public Connection getConnection() {
        // Retourner un proxy pour empêcher les blocs 'try (Connection c = ...)' de fermer le singleton !
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] { Connection.class },
            (proxy, method, args) -> {
                if ("close".equals(method.getName())) {
                    // On ignore complétement l'appel .close() pour protéger le Singleton !
                    return null;
                }
                try {
                    return method.invoke(connection, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getTargetException(); // Nécessaire pour ne pas masquer les vraies SQLException
                }
            }
        );
    }


    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connexion fermée.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erreur fermeture : " + e.getMessage());
        }
    }
}
