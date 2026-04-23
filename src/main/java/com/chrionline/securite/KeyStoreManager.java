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
     * Charge une clé privée depuis un KeyStore PKCS12.
     */
    public static PrivateKey getPrivateKey(String keyStorePath, String password, String alias, String keyPassword) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            ks.load(fis, password.toCharArray());
        }
        return (PrivateKey) ks.getKey(alias, keyPassword.toCharArray());
    }

    /**
     * Charge la clé publique depuis le certificat d'un KeyStore PKCS12.
     */
    public static PublicKey getPublicKey(String keyStorePath, String password, String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            ks.load(fis, password.toCharArray());
        }
        Certificate cert = ks.getCertificate(alias);
        if (cert == null) throw new Exception("Aucun certificat pour l'alias : " + alias);
        return cert.getPublicKey();
    }

    /**
     * Crée un KeyStore PKCS12 avec une paire de clés RSA auto-signée via keytool.
     * keytool génère un vrai certificat X.509 valide, ce qui évite les erreurs
     * "Empty input" ou "Could not parse certificate" des implémentations maison.
     *
     * @param path     Chemin du fichier keystore à créer (ex: "admin.jks")
     * @param password Mot de passe qui protège le keystore ET la clé
     * @param alias    Alias de la paire de clés dans le keystore
     * @throws Exception si keytool échoue ou n'est pas trouvé
     */
    public static void createKeyStore(String path, String password, String alias,
                                      java.security.KeyPair ignoredKeyPair) throws Exception {
        // Supprimer l'ancien fichier s'il existe
        File f = new File(path);
        if (f.exists()) f.delete();

        // Construire la commande keytool
        // keytool est dans le même répertoire que java
        String javaHome = System.getProperty("java.home");
        String keytool  = javaHome + File.separator + "bin" + File.separator + "keytool";

        String[] cmd = {
            keytool,
            "-genkeypair",
            "-alias",     alias,
            "-keyalg",    "RSA",
            "-keysize",   "2048",
            "-validity",  "3650",           // 10 ans
            "-dname",     "CN=Admin,O=ChriOnline,C=FR",
            "-keystore",  path,
            "-storetype", "PKCS12",
            "-storepass", password,
            "-keypass",   password,
            "-noprompt"
        };

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        // Lire la sortie pour le diagnostic
        String output = new String(proc.getInputStream().readAllBytes());
        int exitCode  = proc.waitFor();

        if (exitCode != 0) {
            throw new Exception("keytool a échoué (exit=" + exitCode + ") : " + output);
        }

        System.out.println("[KeyStoreManager] KeyStore créé avec succès : " + path);
    }

    /**
     * Extrait la clé publique depuis un KeyStore existant.
     * Utilisé après createKeyStore pour obtenir la clé à envoyer au serveur.
     */
    public static java.security.PublicKey extractPublicKey(String path, String password, String alias) throws Exception {
        return getPublicKey(path, password, alias);
    }
}
