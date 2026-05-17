package com.chrionline.securite;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Moteur de cryptographie asymétrique RSA pour sécuriser les données de paiement.
 *
 * Zéro-Knowledge : 
 * - Le client utilise exclusivement la clé publique du serveur pour chiffrer la carte.
 * - Seul le serveur possède la clé privée (stockée de manière sécurisée ou générée
 *   dynamiquement en mémoire) et peut déchiffrer les données.
 * - Aucune clé symétrique ou secrète n'est stockée côté client.
 */
public class PaymentCrypto {

    private static final String ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    // Clés RSA (la clé privée reste au niveau du serveur, la clé publique est partagée)
    private static PrivateKey serverPrivateKey;
    private static PublicKey serverPublicKey;

    static {
        // Côté serveur : on essaie d'initialiser la paire de clés RSA
        try {
            if (isServerEnvironment()) {
                // Tenter de charger les clés RSA depuis Vault
                java.util.Map<String, String> config = VaultServerService.getServerConfig();
                String privKeyBase64 = config.get("payment_rsa_private");
                String pubKeyBase64 = config.get("payment_rsa_public");

                if (privKeyBase64 != null && !privKeyBase64.isEmpty() && pubKeyBase64 != null && !pubKeyBase64.isEmpty()) {
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    
                    byte[] privBytes = Base64.getDecoder().decode(privKeyBase64);
                    serverPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
                    
                    byte[] pubBytes = Base64.getDecoder().decode(pubKeyBase64);
                    serverPublicKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
                    
                    System.out.println("[PaymentCrypto-RSA] Paire de clés chargée depuis Vault.");
                } else {
                    // Fallback : générer dynamiquement une paire de clés RSA de 2048 bits
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                    kpg.initialize(2048);
                    KeyPair kp = kpg.generateKeyPair();
                    serverPrivateKey = kp.getPrivate();
                    serverPublicKey = kp.getPublic();
                    System.out.println("[PaymentCrypto-RSA] Paire de clés RSA générée dynamiquement (Vault indisponible).");
                }
            } else {
                System.out.println("[PaymentCrypto-RSA] Environnement Client : Clé publique en attente du serveur.");
            }
        } catch (Exception e) {
            System.err.println("[PaymentCrypto-RSA] Erreur d'initialisation : " + e.getMessage());
            // Fallback de sécurité : générer une paire temporaire pour que l'application ne plante pas
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                serverPrivateKey = kp.getPrivate();
                serverPublicKey = kp.getPublic();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Permet au client de définir la clé publique du serveur reçue par le réseau.
     */
    public static void setServerPublicKey(String pubKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(pubKeyBase64);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            serverPublicKey = kf.generatePublic(new X509EncodedKeySpec(keyBytes));
            System.out.println("[PaymentCrypto-RSA] Clé publique du serveur enregistrée avec succès.");
        } catch (Exception e) {
            System.err.println("[PaymentCrypto-RSA] Échec de l'enregistrement de la clé publique : " + e.getMessage());
        }
    }

    /**
     * Récupère la clé publique sous forme Base64 pour l'envoyer aux clients.
     */
    public static String getServerPublicKeyBase64() {
        if (serverPublicKey == null) return null;
        return Base64.getEncoder().encodeToString(serverPublicKey.getEncoded());
    }

    public static boolean hasPublicKey() {
        return serverPublicKey != null;
    }

    /**
     * Chiffre les données sensibles (carte bancaire) à l'aide de la clé publique RSA.
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        if (serverPublicKey == null) {
            System.err.println("[PaymentCrypto-RSA] Erreur : Clé publique manquante. Chiffrement impossible.");
            return plaintext;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            System.err.println("[PaymentCrypto-RSA] Échec du chiffrement RSA : " + e.getMessage());
            return plaintext;
        }
    }

    /**
     * Chiffre les données sensibles (carte bancaire) sous forme de char[] et nettoie impérativement la mémoire.
     */
    public static String encrypt(char[] plaintext) {
        if (plaintext == null || plaintext.length == 0) return "";
        if (serverPublicKey == null) {
            System.err.println("[PaymentCrypto-RSA] Erreur : Clé publique manquante. Chiffrement impossible.");
            return "";
        }
        try {
            // Conversion char[] -> byte[] temporaire
            java.nio.CharBuffer charBuffer = java.nio.CharBuffer.wrap(plaintext);
            java.nio.ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            // Chiffrement RSA
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            byte[] ciphertext = cipher.doFinal(bytes);

            // Écrasement immédiat de la clé temporaire en mémoire
            java.util.Arrays.fill(bytes, (byte) 0);

            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            System.err.println("[PaymentCrypto-RSA] Échec du chiffrement RSA : " + e.getMessage());
            return "";
        } finally {
            // Nettoyage impératif et immédiat du tableau d'entrée
            java.util.Arrays.fill(plaintext, '0');
        }
    }

    /**
     * Déchiffre les données sensibles à l'aide de la clé privée RSA (Serveur uniquement).
     */
    public static String decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null || ciphertextBase64.isEmpty()) return ciphertextBase64;
        if (serverPrivateKey == null) {
            System.err.println("[PaymentCrypto-RSA] Erreur : Clé privée manquante. Déchiffrement impossible.");
            return ciphertextBase64;
        }
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(ciphertextBase64);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
            byte[] plaintextBytes = cipher.doFinal(cipherBytes);
            return new String(plaintextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[PaymentCrypto-RSA] Échec du déchiffrement RSA : " + e.getMessage());
            return ciphertextBase64;
        }
    }

    /**
     * Vérifie si l'environnement actuel est le serveur en vérifiant la présence de la classe du serveur.
     */
    private static boolean isServerEnvironment() {
        try {
            Class.forName("com.chrionline.server.core.Server");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isEncrypted(String text) {
        if (text == null || text.length() < 24) return false;
        try {
            Base64.getDecoder().decode(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
