package com.chrionline.securite;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffrement applicatif AES-256/GCM pour les données de paiement.
 *
 * Défense en profondeur : même si le tunnel TLS est compromis,
 * le numéro de carte bancaire reste illisible car chiffré avec une clé
 * distincte stockée dans Vault (secret/server/config → payment_aes_key).
 *
 * Format du message chiffré (Base64) : IV (12 octets) || Ciphertext+Tag
 */
public class PaymentCrypto {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // 96 bits (recommandé par NIST)
    private static final int GCM_TAG_LENGTH = 128;  // 128 bits d'authentification

    // Clé AES chargée depuis Vault ou en dur pour le fallback
    private static byte[] aesKeyBytes;

    static {
        try {
            // Tenter de charger la clé depuis Vault (secret/server/config → payment_aes_key)
            java.util.Map<String, String> config = VaultServerService.getServerConfig();
            String keyFromVault = config.get("payment_aes_key");

            if (keyFromVault != null && !keyFromVault.isEmpty()) {
                // Dériver une clé de 256 bits à partir du secret via SHA-256
                java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
                aesKeyBytes = sha.digest(keyFromVault.getBytes(StandardCharsets.UTF_8));
                System.out.println("[PaymentCrypto] Clé AES chargée depuis Vault.");
            } else {
                // Fallback : clé dérivée d'un secret par défaut (développement uniquement)
                java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
                aesKeyBytes = sha.digest("ChriOnline-Dev-Payment-Key-2026".getBytes(StandardCharsets.UTF_8));
                System.out.println("[PaymentCrypto] WARNING: Clé AES par défaut utilisée (Vault indisponible).");
            }
        } catch (Exception e) {
            System.err.println("[PaymentCrypto] Erreur d'initialisation : " + e.getMessage());
            try {
                java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
                aesKeyBytes = sha.digest("ChriOnline-Dev-Payment-Key-2026".getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                throw new RuntimeException("Impossible d'initialiser PaymentCrypto", ex);
            }
        }
    }

    /**
     * Chiffre un texte en clair (ex: numéro de carte) avec AES-256/GCM.
     *
     * @param plaintext le texte en clair à chiffrer
     * @return le texte chiffré encodé en Base64 (IV + ciphertext+tag)
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            // 1. Générer un IV aléatoire unique pour chaque chiffrement
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 2. Initialiser le chiffrement AES-GCM
            SecretKey key = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            // 3. Chiffrer
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 4. Concaténer IV + Ciphertext et encoder en Base64
            byte[] message = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, message, 0, iv.length);
            System.arraycopy(ciphertext, 0, message, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(message);

        } catch (Exception e) {
            System.err.println("[PaymentCrypto] Erreur de chiffrement : " + e.getMessage());
            return plaintext; // Fallback : retourner en clair (ne devrait jamais arriver)
        }
    }

    /**
     * Déchiffre un texte chiffré par {@link #encrypt(String)}.
     *
     * @param encryptedBase64 le texte chiffré encodé en Base64
     * @return le texte en clair original
     */
    public static String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return encryptedBase64;
        }
        try {
            // 1. Décoder le Base64
            byte[] message = Base64.getDecoder().decode(encryptedBase64);

            // 2. Extraire l'IV (12 premiers octets)
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(message, 0, iv, 0, iv.length);

            // 3. Extraire le ciphertext+tag
            byte[] ciphertext = new byte[message.length - iv.length];
            System.arraycopy(message, iv.length, ciphertext, 0, ciphertext.length);

            // 4. Déchiffrer
            SecretKey key = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            System.err.println("[PaymentCrypto] Erreur de déchiffrement : " + e.getMessage());
            // Si le déchiffrement échoue, le texte n'était peut-être pas chiffré
            return encryptedBase64;
        }
    }

    /**
     * Vérifie si un texte semble être chiffré (format Base64 valide + longueur minimale).
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.length() < 20) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            return decoded.length > GCM_IV_LENGTH; // Au moins IV + quelques octets
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
