package com.chrionline.server.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Journalisation des événements de sécurité dans {@code security.log}.
 */
public final class SecurityLogger {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path LOG_FILE = Path.of("security.log");

    private SecurityLogger() {}

    private static void append(String line) {
        try {
            Files.writeString(LOG_FILE, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[SECURITY] Écriture security.log impossible : " + e.getMessage());
        }
    }

    private static String ts() {
        return LocalDateTime.now().format(DATE_FMT);
    }

    private static String nz(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? "-" : sessionId;
    }

    /**
     * Tentative avec session absente ou inconnue.
     */
    public static void logInvalidSession(String ip, String attemptedCommand, String sessionIdSent) {
        append(String.format("[SECURITY] %s | IP: %s | Action: INVALID_SESSION (%s) | SessionID: %s%n",
                ts(), ip, attemptedCommand, nz(sessionIdSent)));
    }

    /**
     * Session expirée (inactivité).
     */
    public static void logExpiredSession(String ip, String attemptedCommand, String sessionIdSent) {
        append(String.format("[SECURITY] %s | IP: %s | Action: SESSION_EXPIRED (%s) | SessionID: %s%n",
                ts(), ip, attemptedCommand, nz(sessionIdSent)));
    }

    public static void logLoginSuccess(String ip, String newSessionId) {
        append(String.format("[SECURITY] %s | IP: %s | Action: LOGIN_SUCCESS | SessionID: %s%n",
                ts(), ip, nz(newSessionId)));
    }

    public static void logLogout(String ip, String sessionId) {
        append(String.format("[SECURITY] %s | IP: %s | Action: LOGOUT | SessionID: %s%n",
                ts(), ip, nz(sessionId)));
    }
}
