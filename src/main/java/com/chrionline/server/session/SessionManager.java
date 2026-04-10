package com.chrionline.server.session;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestion centralisée des sessions TCP (identifiants aléatoires, stockage thread-safe).
 */
public final class SessionManager {

    private static final int SESSION_ID_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, Session> SESSIONS = new ConcurrentHashMap<>();

    public enum LastValidationFailure {
        NONE,
        EMPTY,
        UNKNOWN_ID,
        EXPIRED
    }

    private static final ThreadLocal<LastValidationFailure> LAST_FAILURE =
            ThreadLocal.withInitial(() -> LastValidationFailure.NONE);

    private SessionManager() {}

    public static String createSession(int userId) {
        String id = generateSessionId();
        SESSIONS.put(id, new Session(userId));
        return id;
    }

    private static String generateSessionId() {
        byte[] buf = new byte[SESSION_ID_BYTES];
        RANDOM.nextBytes(buf);
        return Base64.getEncoder().encodeToString(buf);
    }

    /**
     * @return la session valide (rafraîchie) ou {@code null} si absente / expirée / invalide
     */
    public static Session validateSession(String sessionId) {
        LAST_FAILURE.set(LastValidationFailure.NONE);
        if (sessionId == null || sessionId.isBlank()) {
            LAST_FAILURE.set(LastValidationFailure.EMPTY);
            return null;
        }
        Session s = SESSIONS.get(sessionId);
        if (s == null) {
            LAST_FAILURE.set(LastValidationFailure.UNKNOWN_ID);
            return null;
        }
        if (s.isExpired()) {
            SESSIONS.remove(sessionId);
            LAST_FAILURE.set(LastValidationFailure.EXPIRED);
            return null;
        }
        s.refresh();
        return s;
    }

    public static LastValidationFailure getLastValidationFailure() {
        return LAST_FAILURE.get();
    }

    /**
     * Invalide l'ancien identifiant et en crée un nouveau pour le même utilisateur.
     *
     * @return le nouvel id, ou {@code null} si l'ancien était absent ou expiré
     */
    public static String regenerateSession(String oldSessionId) {
        if (oldSessionId == null || oldSessionId.isBlank()) {
            return null;
        }
        Session old = SESSIONS.remove(oldSessionId);
        if (old == null || old.isExpired()) {
            return null;
        }
        return createSession(old.getUserId());
    }

    public static void invalidateSession(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            SESSIONS.remove(sessionId);
        }
    }
}
