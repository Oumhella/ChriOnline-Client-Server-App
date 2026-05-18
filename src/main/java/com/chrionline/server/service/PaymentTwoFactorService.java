package com.chrionline.server.service;

import com.chrionline.securite.TOTPService;
import com.chrionline.server.dao.UserDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service 2FA TOTP (Google Authenticator) pour les paiements.
 * 
 * Gestion de l'enrôlement dynamique (si l'utilisateur n'a pas configuré TOTP)
 * et de la vérification cryptographique inviolable (±30s de décalage temporel).
 */
public final class PaymentTwoFactorService {

    private static final Logger logger = LogManager.getLogger(PaymentTwoFactorService.class);

    private static final long SETUP_TTL_MS = 10 * 60 * 1000L; // 10 minutes pour scanner le QR code
    private static final ConcurrentHashMap<Integer, SetupEntry> PENDING_SETUPS = new ConcurrentHashMap<>();

    private static final class SetupEntry {
        final String secret;
        final long expiresAt;

        SetupEntry(String secret, long expiresAt) {
            this.secret = secret;
            this.expiresAt = expiresAt;
        }
    }

    private PaymentTwoFactorService() {}

    /**
     * Initie le processus de validation 2FA pour le paiement.
     * Si l'utilisateur n'a pas de secret TOTP, génère un secret d'enrôlement temporaire.
     */
    public static Map<String, Object> initiateVerification(int userId) {
        String existingSecret = UserDAO.getTotpSecretById(userId);
        Map<String, Object> response = new HashMap<>();

        if (existingSecret != null && !existingSecret.isEmpty()) {
            // L'utilisateur a déjà configuré Microsoft Authenticator
            response.put("statut", "REQUIRES_PAYMENT_2FA");
            response.put("message", "Saisissez le code à 6 chiffres généré par votre application de sécurité (Microsoft Authenticator) pour valider le paiement.");
            return response;
        }

        // L'utilisateur n'a pas encore configuré Microsoft Authenticator : Enrôlement requis
        String tempSecret = TOTPService.generateSecret();
        PENDING_SETUPS.put(userId, new SetupEntry(tempSecret, System.currentTimeMillis() + SETUP_TTL_MS));

        String[] emailAndRole = UserDAO.getEmailAndRoleById(userId);
        String email = (emailAndRole != null && emailAndRole[0] != null) ? emailAndRole[0] : "utilisateur@chrionline.com";
        String otpauthUri = TOTPService.generateOtpAuthUri(tempSecret, email);

        response.put("statut", "REQUIRES_TOTP_SETUP");
        response.put("totpSecret", tempSecret);
        response.put("otpauthUri", otpauthUri);
        response.put("message", "Pour sécuriser vos transactions, veuillez lier votre compte à une application d'authentification (Microsoft Authenticator) :\n" +
                                "1. Scannez le QR Code ou saisissez la clé manuellement dans l'application.\n" +
                                "2. Entrez ensuite le code à 6 chiffres ci-dessous pour confirmer et valider le paiement.");
        
        logger.info("[PAYMENT_TOTP] Enrôlement initié pour l'utilisateur ID: {} (secret temporaire généré).", userId);
        return response;
    }

    /**
     * Vérifie le code TOTP soumis et l'associe définitivement au compte si enrôlement.
     */
    public static boolean verifyAndConsume(int userId, String code) {
        if (code == null || code.trim().length() != 6) {
            return false;
        }
        String cleanCode = code.trim();

        // 1. Cas de l'enrôlement en attente (Setup)
        SetupEntry setup = PENDING_SETUPS.get(userId);
        if (setup != null) {
            if (System.currentTimeMillis() > setup.expiresAt) {
                PENDING_SETUPS.remove(userId);
                return false;
            }
            if (TOTPService.verifyCode(setup.secret, cleanCode)) {
                // Validation et persistance immédiate du secret dans la BDD
                UserDAO.updateTotpSecretById(userId, setup.secret);
                PENDING_SETUPS.remove(userId);
                logger.info("[PAYMENT_TOTP] Nouvel enrôlement TOTP validé et stocké pour l'utilisateur ID: {}", userId);
                return true;
            }
            return false;
        }

        // 2. Cas classique (Secret déjà stocké en BDD)
        String secret = UserDAO.getTotpSecretById(userId);
        if (secret == null || secret.isEmpty()) {
            return false;
        }

        boolean valid = TOTPService.verifyCode(secret, cleanCode);
        if (valid) {
            logger.info("[PAYMENT_TOTP] Code TOTP validé avec succès pour l'utilisateur ID: {}", userId);
        } else {
            logger.warn("[PAYMENT_TOTP] Code TOTP invalide soumis pour l'utilisateur ID: {}", userId);
        }
        return valid;
    }
}
