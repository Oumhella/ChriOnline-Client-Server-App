package com.chrionline.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.io.InputStream;

/**
 * Service de gestion des JSON Web Tokens (JWT).
 */
public class JwtService {

    private static String SECRET_KEY_STR;
    private static Key SIGNING_KEY;
    private static final long EXPIRATION_TIME = 864_000_000; // 10 jours

    static {
        try (InputStream in = JwtService.class.getClassLoader().getResourceAsStream("server.properties")) {
            Properties props = new Properties();
            if (in != null) props.load(in);
            SECRET_KEY_STR = props.getProperty("security.jwt.secret", "default_secret_key_at_least_32_chars_long_12345");
            
            // S'assurer que la clé est assez longue pour HMAC
            byte[] keyBytes = SECRET_KEY_STR.getBytes();
            if (keyBytes.length < 32) {
                SIGNING_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            } else {
                SIGNING_KEY = Keys.hmacShaKeyFor(keyBytes);
            }
        } catch (Exception e) {
            SIGNING_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    /**
     * Génère un token pour un utilisateur.
     */
    public static String generateToken(String email, String role, int userId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SIGNING_KEY)
                .compact();
    }

    /**
     * Vérifie et décode un token.
     */
    public static Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) SIGNING_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
