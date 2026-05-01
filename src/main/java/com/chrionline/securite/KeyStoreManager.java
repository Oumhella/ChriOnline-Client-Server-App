package com.chrionline.securite;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

/**
 * Gestionnaire de KeyStore pour l'authentification admin RSA.
 *
 * Pourquoi keytool ?
 * Générer un certificat X.509 valide en Java pur (sans BouncyCastle) nécessite
 * des classes internes (sun.security.x509) qui cassent sur certains JDK.
 * keytool est inclus dans TOUT JDK/JRE et génère un keystore PKCS12 standard,
 * garanti lisible par le KeyStore Java.
 */
public class KeyStoreManager {

    /**
     * Charge une clé privée depuis un KeyStore PKCS12 (Mot de passe géré par Vault).
     */
    public static PrivateKey getPrivateKey(String keyStorePath, String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        char[] password = null;
        try {
            password = VaultKeystoreService.getKeystorePassword();
            try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                ks.load(fis, password);
            }
            return (PrivateKey) ks.getKey(alias, password);
        } finally {
            // Nettoyage en mémoire du mot de passe (s'il s'agissait d'un tableau alloué par nous)
            if (password != null) {
                java.util.Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Charge la clé publique depuis le certificat d'un KeyStore PKCS12.
     */
    public static PublicKey getPublicKey(String keyStorePath, String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        char[] password = null;
        try {
            password = VaultKeystoreService.getKeystorePassword();
            try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                ks.load(fis, password);
            }
            Certificate cert = ks.getCertificate(alias);
            if (cert == null) throw new Exception("Aucun certificat pour l'alias : " + alias);
            return cert.getPublicKey();
        } finally {
            if (password != null) {
                java.util.Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Crée un KeyStore PKCS12 avec une paire de clés RSA auto-signée via keytool.
     * Le mot de passe est récupéré depuis Vault.
     */
    public static void createKeyStore(String path, String alias, java.security.KeyPair ignoredKeyPair) throws Exception {
        File f = new File(path);
        if (f.exists()) f.delete();

        char[] password = null;
        try {
            password = VaultKeystoreService.getKeystorePassword();
            String passStr = new String(password);

            String javaHome = System.getProperty("java.home");
            String keytool  = javaHome + File.separator + "bin" + File.separator + "keytool";

            String[] cmd = {
                keytool,
                "-genkeypair",
                "-alias",     alias,
                "-keyalg",    "RSA",
                "-keysize",   "2048",
                "-validity",  "3650",
                "-dname",     "CN=Admin,O=ChriOnline,C=FR",
                "-keystore",  path,
                "-storetype", "PKCS12",
                "-storepass", passStr,
                "-keypass",   passStr,
                "-noprompt"
            };

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            String output = new String(proc.getInputStream().readAllBytes());
            int exitCode  = proc.waitFor();

            if (exitCode != 0) {
                throw new Exception("keytool a échoué (exit=" + exitCode + ") : " + output);
            }
            System.out.println("[KeyStoreManager] KeyStore créé avec succès via mot de passe Vault : " + path);
        } finally {
            if (password != null) {
                java.util.Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Extrait la clé publique depuis un KeyStore existant (mot de passe Vault).
     */
    public static java.security.PublicKey extractPublicKey(String path, String alias) throws Exception {
        return getPublicKey(path, alias);
    }
}
