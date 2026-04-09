package com.chrionline.server.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de limitation de débit (Rate Limiting) utilisant Bucket4j.
 * Fournit des "seaux" (buckets) de jetons par adresse IP.
 */
public class RateLimiterService {

    // Buckets pour les requêtes générales (ex: LISTE_PRODUITS)
    private static final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    
    // Buckets pour les requêtes sensibles (ex: CONNEXION, INSCRIPTION, CHECKOUT)
    private static final Map<String, Bucket> sensitiveBuckets = new ConcurrentHashMap<>();

    /**
     * Tente de consommer un jeton pour une requête générale.
     * Limite : 100 requêtes par 15 minutes.
     */
    public static boolean allowGeneralRequest(String ip) {
        Bucket bucket = generalBuckets.computeIfAbsent(ip, k -> {
            // Refill 100 tokens every 15 minutes
            Refill refill = Refill.intervally(100, Duration.ofMinutes(15));
            Bandwidth limit = Bandwidth.classic(100, refill);
            return Bucket.builder().addLimit(limit).build();
        });
        return bucket.tryConsume(1);
    }

    /**
     * Tente de consommer un jeton pour une requête sensible.
     * Limite : 10 requêtes par 15 minutes.
     */
    public static boolean allowSensitiveRequest(String ip) {
        Bucket bucket = sensitiveBuckets.computeIfAbsent(ip, k -> {
            // Refill 10 tokens every 15 minutes
            Refill refill = Refill.intervally(10, Duration.ofMinutes(15));
            Bandwidth limit = Bandwidth.classic(10, refill);
            return Bucket.builder().addLimit(limit).build();
        });
        return bucket.tryConsume(1);
    }
}
