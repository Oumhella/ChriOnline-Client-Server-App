package com.chrionline.server.service;

import com.chrionline.server.dao.UserDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2FA simulé pour le paiement : code 6 chiffres, valable 5 minutes, indexé par {@code idUtilisateur}.
 */
public final class PaymentTwoFactorService {

    private static final Logger logger = LogManager.getLogger(PaymentTwoFactorService.class);

    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<Integer, Entry> PENDING = new ConcurrentHashMap<>();

    private static final class Entry {
        final String code;
        final long expiresAt;

        Entry(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    private PaymentTwoFactorService() {}

    /**
     * Génère un nouveau code pour l'utilisateur (remplace tout code en attente).
     * Journalise le code et l'envoie par email ({@link EmailService#envoyerCodePaiement2FA}).
     *
     * @return le code généré
     */
    public static String generateAndStore(int userId) {
        int n = 100000 + RANDOM.nextInt(900000);
        String code = String.valueOf(n);
        PENDING.put(userId, new Entry(code, System.currentTimeMillis() + TTL_MS));

        logger.info("[PAYMENT_2FA] Code simulé pour utilisateur {} : {} (valide 5 min)", userId, code);

        String[] profil = UserDAO.getEmailAndRoleById(userId);
        if (profil != null && profil[0] != null && !profil[0].isBlank()) {
            final String destinataire = profil[0];
            final String codePourEmail = code;
            new Thread(() -> {
                try {
                    EmailService.envoyerCodePaiement2FA(destinataire, codePourEmail);
                } catch (Exception e) {
                    System.err.println("[PAYMENT_2FA] Erreur envoi email code paiement : " + e.getMessage());
                }
            }).start();
        }

        return code;
    }

    /**
     * Vérifie le code ; en cas de succès, l'entrée est supprimée (usage unique).
     */
    public static boolean verifyAndConsume(int userId, String code) {
        if (code == null) {
            return false;
        }
        String trimmed = code.trim();
        if (trimmed.length() != 6 || !trimmed.chars().allMatch(Character::isDigit)) {
            return false;
        }
        Entry e = PENDING.get(userId);
        if (e == null) {
            return false;
        }
        if (System.currentTimeMillis() > e.expiresAt) {
            PENDING.remove(userId, e);
            return false;
        }
        if (!e.code.equals(trimmed)) {
            return false;
        }
        PENDING.remove(userId);
        return true;
    }
}
