package com.chrionline.client.session;

import java.util.Map;

/**
 * Singleton pour stocker les informations de l'utilisateur connecté côté client.
 */
public class SessionManager {

    private static SessionManager instance;
    private int userId = -1;
    private String nom;
    private String prenom;
    private String email;
    private String role;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setUser(Map<String, Object> data) {
        if (data != null) {
            this.userId = (Integer) data.getOrDefault("userId", -1);
            this.nom = (String) data.get("nom");
            this.prenom = (String) data.get("prenom");
            this.email = (String) data.get("email");
            this.role = (String) data.get("role");
        }
    }

    public void clear() {
        this.userId = -1;
        this.nom = null;
        this.prenom = null;
        this.email = null;
        this.role = null;
    }

    public boolean isLogged() {
        return userId != -1;
    }

    public int getUserId() { return userId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
