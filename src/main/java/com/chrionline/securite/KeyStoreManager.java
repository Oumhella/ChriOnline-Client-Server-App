package com.chrionline.securite;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

public class KeyStoreManager {

    /**
     * Charge une clé privée à partir d'un KeyStore (JKS ou PKCS12).
     */
    public static PrivateKey getPrivateKey(String keyStorePath, String keyStorePassword, String alias, String keyPassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            keyStore.load(fis, keyStorePassword.toCharArray());
        }
        return (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray());
    }

    /**
     * Charge une clé publique à partir du certificat contenu dans un KeyStore.
     */
    public static PublicKey getPublicKey(String keyStorePath, String keyStorePassword, String alias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            keyStore.load(fis, keyStorePassword.toCharArray());
        }
        Certificate cert = keyStore.getCertificate(alias);
        if (cert == null) {
            throw new Exception("Aucun certificat trouvé pour l'alias : " + alias);
        }
        return cert.getPublicKey();
    }
}
