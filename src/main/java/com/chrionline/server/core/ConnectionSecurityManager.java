package com.chrionline.server.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère la sécurité des connexions entrantes.
 * Détecte les attaques de type SYN Flood en comptant le nombre de connexions
 * par adresse IP dans une fenêtre de temps réduite.
 */
public class ConnectionSecurityManager {
    
    // Limites de sécurité selon cahier des charges
    private static final int MAX_CONNEXIONS_PAR_MINUTE = 100; 
    private static final long TIME_WINDOW_MS = 60000; // 1 minute
    private static final long BAN_DURATION_MS = 5 * 60 * 1000; // Block pendant 5 minutes

    // Historiques
    private final Map<String, ConnectionAttempt> attempts = new ConcurrentHashMap<>();
    private final Map<String, Long> blacklistedIps = new ConcurrentHashMap<>();

    private static class ConnectionAttempt {
        long firstAttemptTime;
        int count;

        ConnectionAttempt(long time) {
            this.firstAttemptTime = time;
            this.count = 1;
        }
    }

    /**
     * Vérifie si l'adresse IP est autorisée à se connecter.
     * @param ip L'adresse IP du client
     * @return true si autorisé, false si bloqué (Déni de service détecté)
     */
    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();

        // 1. Vérifier si l'IP est déjà sur liste noire
        if (blacklistedIps.containsKey(ip)) {
            long banTime = blacklistedIps.get(ip);
            if (now - banTime > BAN_DURATION_MS) {
                // Temps de bannissement écoulé, on lui redonne sa chance
                blacklistedIps.remove(ip);
                attempts.remove(ip);
                System.out.println("[SECURITY] Dé-bannissement de l'IP : " + ip);
            } else {
                return false; // Toujours bloqué
            }
        }

        // 2. Compter la nouvelle connexion
        attempts.compute(ip, (key, attempt) -> {
            if (attempt == null) {
                return new ConnectionAttempt(now);
            }
            if (now - attempt.firstAttemptTime > TIME_WINDOW_MS) {
                // Nouvelle fenêtre de temps
                return new ConnectionAttempt(now);
            }
            attempt.count++;
            return attempt;
        });

        // 3. Vérifier si on dépasse la limite autorisée (100 connexions/min)
        if (attempts.get(ip).count > MAX_CONNEXIONS_PAR_MINUTE) {
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println("[SURVEILLANCE & LOGS] Attaque DoS/SYN Flood détectée !");
            System.err.println("[SURVEILLANCE & LOGS] Limite de " + MAX_CONNEXIONS_PAR_MINUTE + " requêtes/min dépassée.");
            System.err.println("[SURVEILLANCE & LOGS] IP mise sous liste noire : " + ip);
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            blacklistedIps.put(ip, now);
            return false;
        }

        return true;
    }
}
