package com.chrionline.server.session;

import com.chrionline.server.dao.SecurityBlacklistDAO;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

/**
 * Gestionnaire central des sessions TCP (RAM uniquement).
 *
 * Fonctionnalités :
 *  - Stockage RAM (ConcurrentHashMap) SANS BD
 *  - Limite de 3 sessions simultanées par utilisateur
 *  - Vérification d'IP à chaque requête protégée
 *  - Adaptabilité de la durée selon fonctionnalité
 *  - Protection brute-force sur sessionId (10 échecs / min → rejet IP)
 *  - Nettoyage automatique RAM toutes les 5 min
 *  - Régénération après chaque transaction (ID roulant)
 *  - Silencieux (Pas de logs sensibles)
 */
public final class SessionManager {

    private static final int  SESSION_ID_BYTES      = 32;
    private static final int  MAX_SESSIONS_PER_USER = 3;
    private static final int  MAX_BRUTE_ATTEMPTS    = 10;
    private static final long BRUTE_WINDOW_MS       = 60_000L;

    private static final ConcurrentHashMap<String, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<String>> USER_SESSIONS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, long[]> BRUTE_TRACKER = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService CLEANER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "session-cleaner");
                t.setDaemon(true);
                return t;
            });

    static {
        CLEANER.scheduleAtFixedRate(SessionManager::runScheduledCleanup, 5, 5, TimeUnit.MINUTES);
    }

    public enum LastValidationFailure {
        NONE, EMPTY, UNKNOWN_ID, EXPIRED, ABSOLUTE_EXPIRED, IP_MISMATCH, BRUTE_FORCE
    }

    private static final ThreadLocal<LastValidationFailure> LAST_FAILURE =
            ThreadLocal.withInitial(() -> LastValidationFailure.NONE);

    private SessionManager() {}

    public static String createSession(int userId, String clientIp) {
        enforceSessionLimit(userId);
        String id  = generateSessionId();
        Session s  = new Session(id, userId, clientIp);
        SESSIONS.put(id, s);
        USER_SESSIONS.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(id);
        return id;
    }

    private static void enforceSessionLimit(int userId) {
        CopyOnWriteArrayList<String> sids = USER_SESSIONS.get(userId);
        if (sids == null) return;
        sids.removeIf(sid -> {
            Session s = SESSIONS.get(sid);
            if (s == null || s.isExpired()) {
                SESSIONS.remove(sid);
                return true;
            }
            return false;
        });
        while (sids.size() >= MAX_SESSIONS_PER_USER) {
            String oldest = sids.isEmpty() ? null : sids.get(0);
            if (oldest == null) break;
            sids.remove(oldest);
            SESSIONS.remove(oldest);
        }
    }

    public static Session validateSession(String sessionId, String clientIp, long dynamicTimeoutMs) {
        LAST_FAILURE.set(LastValidationFailure.NONE);
        if (isBruteForce(clientIp)) {
            LAST_FAILURE.set(LastValidationFailure.BRUTE_FORCE);
            return null;
        }
        if (sessionId == null || sessionId.isBlank()) {
            LAST_FAILURE.set(LastValidationFailure.EMPTY);
            return null;
        }
        Session s = SESSIONS.get(sessionId);
        if (s == null) {
            LAST_FAILURE.set(LastValidationFailure.UNKNOWN_ID);
            recordBruteAttempt(clientIp);
            return null;
        }
        if (s.isAbsolutelyExpired()) {
            evict(sessionId, s.getUserId());
            LAST_FAILURE.set(LastValidationFailure.ABSOLUTE_EXPIRED);
            return null;
        }
        if (s.isInactivityExpired(dynamicTimeoutMs)) {
            evict(sessionId, s.getUserId());
            LAST_FAILURE.set(LastValidationFailure.EXPIRED);
            return null;
        }
        if (s.getClientIp() != null && !"unknown".equals(s.getClientIp()) && !s.getClientIp().equals(clientIp)) {
            LAST_FAILURE.set(LastValidationFailure.IP_MISMATCH);
            return null;
        }
        s.refresh();
        return s;
    }

    public static Session validateSession(String sessionId, String clientIp) {
        return validateSession(sessionId, clientIp, 15L * 60 * 1000); // 15 min defaut
    }

    public static Session validateSession(String sessionId) {
        return validateSession(sessionId, "unknown");
    }

    public static String regenerateSession(String oldSessionId, String clientIp) {
        if (oldSessionId == null || oldSessionId.isBlank()) return null;
        Session old = SESSIONS.remove(oldSessionId);
        if (old == null || old.isExpired()) return null;
        removeFromUserMap(old.getUserId(), oldSessionId);
        return createSession(old.getUserId(), clientIp);
    }

    public static void invalidateSession(String sessionId) {
        Session s = SESSIONS.remove(sessionId);
        if (s != null) removeFromUserMap(s.getUserId(), sessionId);
    }

    public static void invalidateAllSessionsForUser(int userId) {
        CopyOnWriteArrayList<String> sids = USER_SESSIONS.remove(userId);
        if (sids != null) {
            for (String sid : sids) SESSIONS.remove(sid);
        }
    }

    private static void runScheduledCleanup() {
        for (Map.Entry<String, Session> e : SESSIONS.entrySet()) {
            if (e.getValue().isExpired()) {
                SESSIONS.remove(e.getKey());
                removeFromUserMap(e.getValue().getUserId(), e.getKey());
            }
        }
        long now = System.currentTimeMillis();
        BRUTE_TRACKER.entrySet().removeIf(e -> now - e.getValue()[1] > BRUTE_WINDOW_MS);
        SecurityBlacklistDAO.cleanupExpired();
    }

    private static void recordBruteAttempt(String ip) {
        if (ip == null || "unknown".equals(ip)) return;
        long now = System.currentTimeMillis();
        BRUTE_TRACKER.compute(ip, (k, v) -> {
            if (v == null || now - v[1] > BRUTE_WINDOW_MS) return new long[]{1, now};
            v[0]++;
            return v;
        });
    }

    private static boolean isBruteForce(String ip) {
        if (ip == null || "unknown".equals(ip)) return false;
        long[] data = BRUTE_TRACKER.get(ip);
        if (data == null) return false;
        if (System.currentTimeMillis() - data[1] > BRUTE_WINDOW_MS) {
            BRUTE_TRACKER.remove(ip);
            return false;
        }
        return data[0] >= MAX_BRUTE_ATTEMPTS;
    }

    private static void evict(String sessionId, int userId) {
        SESSIONS.remove(sessionId);
        removeFromUserMap(userId, sessionId);
    }

    private static void removeFromUserMap(int userId, String sessionId) {
        CopyOnWriteArrayList<String> list = USER_SESSIONS.get(userId);
        if (list != null) list.remove(sessionId);
    }

    private static String generateSessionId() {
        SecureRandom random = new SecureRandom();
        byte[] buf = new byte[SESSION_ID_BYTES];
        random.nextBytes(buf);
        return Base64.getEncoder().encodeToString(buf);
    }

    public static LastValidationFailure getLastValidationFailure() {
        return LAST_FAILURE.get();
    }
}
