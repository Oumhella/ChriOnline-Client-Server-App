package com.chrionline.client.session;

import java.util.Map;

/**
 * Singleton pour stocker les informations de l'utilisateur connecté côté client.
 */
public class SessionManager {

    private static SessionManager instance;
    private int userId = -1;
    /** Identifiant de session serveur (TCP), distinct du userId local. */
    private String serverSessionId;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private final java.util.List<String> notificationHistory = new java.util.ArrayList<>();
    private int unreadNotificationsCount = 0;

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
            Object sid = data.get("sessionId");
            if (sid instanceof String) {
                this.serverSessionId = (String) sid;
            }
        }
    }

    public void setServerSessionId(String sessionId) {
        this.serverSessionId = sessionId;
    }

    public String getServerSessionId() {
        return serverSessionId;
    }

    public void clear() {
        this.userId = -1;
        this.serverSessionId = null;
        this.nom = null;
        this.prenom = null;
        this.email = null;
        this.role = null;
        this.notificationHistory.clear();
        this.unreadNotificationsCount = 0;
    }

    /**
     * Réaction à {@code ERROR} / {@code SESSION_EXPIRED} renvoyé par le serveur.
     */
    public void handleServerResponseIfSessionExpired(java.util.Map<String, Object> rep) {
        if (rep != null
                && "ERROR".equals(rep.get("statut"))
                && "SESSION_EXPIRED".equals(rep.get("message"))) {
            clear();
        }
    }
    
    public void addNotification(String msg) {
        this.notificationHistory.add(0, msg);
        this.unreadNotificationsCount++;
    }
    
    public void resetUnreadCount() {
        this.unreadNotificationsCount = 0;
    }
    
    public int getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }
    
    public java.util.List<String> getNotificationHistory() {
        return notificationHistory;
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