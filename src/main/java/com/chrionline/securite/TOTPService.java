package com.chrionline.securite;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * Service TOTP (Time-based One-Time Password) - RFC 6238.
 * Compatible avec Microsoft Authenticator, Google Authenticator, Authy, etc.
 *
 * Implémentation en Java pur (HMAC-SHA1), aucune dépendance externe requise.
 *
 * Le code TOTP est généré à partir d'un secret partagé et du temps actuel :
 *   1. Le temps est divisé en périodes de 30 secondes
 *   2. Un HMAC-SHA1 est calculé sur le compteur de période
 *   3. Le résultat est tronqué en un code à 6 chiffres
 */
public class TOTPService {

    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final String ISSUER = "ChriOnline";
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    // ─── Génération du Secret ────────────────────────────────────────────────

    /**
     * Génère un secret aléatoire encodé en Base32 (20 octets = 32 caractères Base32).
     * Ce secret est partagé entre le serveur et l'application Authenticator.
     */
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 160 bits (recommandé par RFC 4226)
        random.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    /**
     * Génère l'URI otpauth:// à encoder dans un QR Code.
     * Format : otpauth://totp/Issuer:email?secret=XXX&issuer=Issuer&algorithm=SHA1&digits=6&period=30
     *
     * @param secret le secret en Base32
     * @param email  l'email de l'administrateur (sert de label dans l'app)
     * @return l'URI complète pour le QR Code
     */
    public static String generateOtpAuthUri(String secret, String email) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
            ISSUER, email, secret, ISSUER, CODE_DIGITS, TIME_STEP_SECONDS
        );
    }

    // ─── Vérification du Code ────────────────────────────────────────────────

    /**
     * Vérifie un code TOTP soumis par l'utilisateur.
     * Accepte le code de la période actuelle, de la période précédente et de la suivante
     * pour tolérer un léger décalage d'horloge (±30 secondes).
     *
     * @param secret  le secret Base32 stocké en BDD
     * @param code    le code à 6 chiffres soumis par l'utilisateur
     * @return true si le code est valide
     */
    public static boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }

        byte[] secretBytes = decodeBase32(secret);
        long currentTimeStep = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

        // Vérifier la période actuelle ± 1 (tolérance de 30 secondes)
        for (int i = -1; i <= 1; i++) {
            String generatedCode = generateCode(secretBytes, currentTimeStep + i);
            if (generatedCode.equals(code)) {
                return true;
            }
        }
        return false;
    }

    // ─── Algorithme TOTP (RFC 6238 / RFC 4226) ──────────────────────────────

    /**
     * Génère un code TOTP pour un pas de temps donné.
     * 
     * Algorithme :
     * 1. Convertir le compteur de temps en 8 octets (big-endian)
     * 2. Calculer HMAC-SHA1(secret, compteur)
     * 3. Extraire 4 octets dynamiques (dynamic truncation)
     * 4. Convertir en nombre et prendre les N derniers chiffres
     */
    private static String generateCode(byte[] secret, long timeStep) {
        try {
            // 1. Compteur en 8 octets big-endian
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

            // 2. HMAC-SHA1
            Mac hmac = Mac.getInstance("HmacSHA1");
            hmac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = hmac.doFinal(timeBytes);

            // 3. Dynamic Truncation (RFC 4226, Section 5.4)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                       | ((hash[offset + 1] & 0xFF) << 16)
                       | ((hash[offset + 2] & 0xFF) << 8)
                       | (hash[offset + 3] & 0xFF);

            // 4. Modulo pour obtenir N chiffres
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);

            // Padding avec des zéros à gauche (ex: 7 → "000007")
            return String.format("%0" + CODE_DIGITS + "d", otp);

        } catch (Exception e) {
            throw new RuntimeException("Erreur de génération TOTP", e);
        }
    }

    // ─── Encodage/Décodage Base32 (RFC 4648) ────────────────────────────────

    /**
     * Encode un tableau d'octets en Base32 (alphabet standard RFC 4648).
     */
    private static String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                result.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return result.toString();
    }

    /**
     * Décode une chaîne Base32 en tableau d'octets.
     */
    private static byte[] decodeBase32(String base32) {
        base32 = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int outputLength = base32.length() * 5 / 8;
        byte[] result = new byte[outputLength];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (char c : base32.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                result[index++] = (byte) (buffer >> bitsLeft);
            }
        }
        return result;
    }
}
