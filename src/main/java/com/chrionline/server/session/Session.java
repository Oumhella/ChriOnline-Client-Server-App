package com.chrionline.server.session;

/**
 * Session serveur authentifiée (inactivité max 10 minutes).
 */
public class Session {

    private static final long TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes

    private final int userId;
    private volatile long lastActivity;

    public Session(int userId) {
        this.userId = userId;
        this.lastActivity = System.currentTimeMillis();
    }

    public int getUserId() {
        return userId;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - lastActivity > TIMEOUT_MS;
    }

    public void refresh() {
        lastActivity = System.currentTimeMillis();
    }
}
