package com.chrionline.server.session;

/**
 * Session serveur TCP authentifiée.
 *
 * Règles d'expiration :
 *   - Inactivité  : 15 minutes sans requête
 *   - Absolue     : 24 heures depuis la création (même usage actif)
 */
public class Session {

    private static final long INACTIVITY_TIMEOUT_MS = 15L * 60 * 1000;       // 15 min
    private static final long ABSOLUTE_TIMEOUT_MS   = 24L * 60 * 60 * 1000;  // 24 h

    private final String sessionId;
    private final int    userId;
    private final String clientIp;
    private final long   createdAt;
    private volatile long lastActivity;

    public Session(String sessionId, int userId, String clientIp) {
        this.sessionId    = sessionId;
        this.userId       = userId;
        this.clientIp     = clientIp;
        this.createdAt    = System.currentTimeMillis();
        this.lastActivity = this.createdAt;
    }

    // ─── Expiration ────────────────────────────────────────────────────────────

    /** Inactivité dépasse le délai dynamique selon la fonctionnalité. */
    public boolean isInactivityExpired(long dynamicTimeoutMs) {
        return System.currentTimeMillis() - lastActivity > dynamicTimeoutMs;
    }

    /** Session > 24 h depuis création → expirée même si active. */
    public boolean isAbsolutelyExpired() {
        return System.currentTimeMillis() - createdAt > ABSOLUTE_TIMEOUT_MS;
    }

    /** Expirée pour l'une ou l'autre raison (utilise le timeout dynamique). */
    public boolean isExpired(long dynamicTimeoutMs) {
        return isInactivityExpired(dynamicTimeoutMs) || isAbsolutelyExpired();
    }

    /** Expirée avec timeout par défaut (nettoyage). */
    public boolean isExpired() {
        return isExpired(INACTIVITY_TIMEOUT_MS);
    }

    /** Rafraîchit le timer d'inactivité (appelé à chaque requête valide). */
    public void refresh() {
        this.lastActivity = System.currentTimeMillis();
    }

    // ─── Accesseurs ────────────────────────────────────────────────────────────

    public String getSessionId()   { return sessionId;    }
    public int    getUserId()      { return userId;        }
    public String getClientIp()    { return clientIp;      }
    public long   getCreatedAt()   { return createdAt;     }
    public long   getLastActivity(){ return lastActivity;  }
}
