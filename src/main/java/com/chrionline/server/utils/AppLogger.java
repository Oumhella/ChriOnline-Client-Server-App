package com.chrionline.server.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Journalisation centralisée pour les événements généraux de l'application.
 * Redirige les logs vers logs/app.log (via la configuration Log4j2).
 */
public final class AppLogger {

    private static final Logger LOG = LogManager.getRootLogger();

    private AppLogger() {}

    public static void info(String message) {
        LOG.info(message);
    }

    public static void warn(String message) {
        LOG.warn(message);
    }

    public static void error(String message) {
        LOG.error(message);
    }

    public static void error(String message, Throwable t) {
        LOG.error(message, t);
    }

    /**
     * Log spécifique pour le suivi d'activité (Audit).
     */
    public static void audit(String action, String details) {
        LOG.info("[AUDIT] Action: {} | Details: {}", action, details);
    }
}
