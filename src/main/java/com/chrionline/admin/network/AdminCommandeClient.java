package com.chrionline.admin.network;

import com.chrionline.shared.dto.CommandeDTO;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client réseau admin — connexion sécurisée via mTLS (Mutual TLS).
 *
 * [MEMBRE 2] Sécurité renforcée :
 * - TrustStore : vérifie le certificat serveur via vault-ca.pem
 * - KeyStore  : présente le certificat admin (admin.jks) au serveur
 * - Port dédié : 9445 avec setNeedClientAuth(true) côté serveur
 */
public class AdminCommandeClient {

    private final String SERVER_IP   = "127.0.0.1";
    private final int    SERVER_PORT = 9445; // [MEMBRE 2] Port mTLS dédié aux admins

    // Chemins des fichiers de sécurité
    private static final String TRUSTSTORE_PATH = "src/main/resources/vault-ca.pem";
    private static final String KEYSTORE_PATH   = "admin.jks";
    private static final String KEYSTORE_PASS   = "admin123";

    /**
     * [MEMBRE 2] Crée une SSLSocketFactory avec authentification mutuelle (mTLS) :
     * - TrustManager : charge vault-ca.pem pour vérifier le serveur
     * - KeyManager   : charge admin.jks pour présenter le certificat client
     */
    private SSLSocketFactory getSSLSocketFactory() throws Exception {
        // 1. Charger le TrustStore avec le CA de Vault
        java.io.InputStream caInputStream = getClass().getResourceAsStream("/vault-ca.pem");
        if (caInputStream == null) {
            java.io.File caFile = new java.io.File(TRUSTSTORE_PATH);
            if (caFile.exists()) {
                caInputStream = new java.io.FileInputStream(caFile);
            } else {
                throw new Exception("vault-ca.pem introuvable. Impossible de vérifier le serveur.");
            }
        }

        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        java.security.cert.Certificate caCert = cf.generateCertificate(caInputStream);
        caInputStream.close();

        java.security.KeyStore trustStore = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("vault-ca", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 2. Charger le KeyStore admin (certificat client pour mTLS)
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("JKS");
        java.io.File ksFile = new java.io.File(KEYSTORE_PATH);
        if (!ksFile.exists()) {
            System.err.println("[ADMIN-mTLS] ATTENTION: " + KEYSTORE_PATH + " introuvable.");
            System.err.println("[ADMIN-mTLS] Le serveur refusera la connexion (mTLS requis sur le port " + SERVER_PORT + ").");
            throw new Exception("Fichier KeyStore admin introuvable (" + KEYSTORE_PATH + "). " +
                    "Générez-le avec : keytool -genkeypair -alias admin -keyalg RSA -keysize 2048 " +
                    "-keystore admin.jks -storepass admin123");
        }

        try (java.io.FileInputStream fis = new java.io.FileInputStream(ksFile)) {
            keyStore.load(fis, KEYSTORE_PASS.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASS.toCharArray());

        // 3. Créer le SSLContext avec les deux managers
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());

        System.out.println("[ADMIN-mTLS] SSLContext initialisé (TrustStore=vault-ca.pem, KeyStore=admin.jks)");
        return sc.getSocketFactory();
    }

    // ───── GET_ALL_ORDERS ─────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public List<CommandeDTO> fetchAllCommandes() {
        try {
            SSLSocketFactory ssf = getSSLSocketFactory();
            try (SSLSocket socket = (SSLSocket) ssf.createSocket(SERVER_IP, SERVER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

                socket.startHandshake();

                Map<String, Object> request = new HashMap<>();
                request.put("commande", "GET_ALL_ORDERS");
                out.writeObject(request);
                out.flush();

                Map<String, Object> response = (Map<String, Object>) in.readObject();
                if ("OK".equals(response.get("statut"))) {
                    return (List<CommandeDTO>) response.get("commandes");
                }
            }
        } catch (Exception e) {
            System.err.println("[ADMIN CLIENT] Erreur GET_ALL_ORDERS : " + e.getMessage());
        }
        return List.of();
    }

    // ───── UPDATE_ORDER_STATUS ────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public boolean updateStatus(String idCommande, String nouveauStatut) {
        try {
            SSLSocketFactory ssf = getSSLSocketFactory();
            try (SSLSocket socket = (SSLSocket) ssf.createSocket(SERVER_IP, SERVER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

                socket.startHandshake();

                Map<String, Object> request = new HashMap<>();
                request.put("commande", "UPDATE_ORDER_STATUS");
                request.put("idCommande", idCommande);
                request.put("statut", nouveauStatut);
                out.writeObject(request);
                out.flush();

                Map<String, Object> response = (Map<String, Object>) in.readObject();
                return "OK".equals(response.get("statut"));
            }
        } catch (Exception e) {
            System.err.println("[ADMIN CLIENT] Erreur UPDATE_ORDER_STATUS : " + e.getMessage());
        }
        return false;
    }

    // ───── GET_ORDER_DETAILS ──────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public CommandeDTO fetchOrderDetails(String idCommande) {
        try {
            SSLSocketFactory ssf = getSSLSocketFactory();
            try (SSLSocket socket = (SSLSocket) ssf.createSocket(SERVER_IP, SERVER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

                socket.startHandshake();

                Map<String, Object> request = new HashMap<>();
                request.put("commande", "GET_ORDER_DETAILS");
                request.put("idCommande", idCommande);
                out.writeObject(request);
                out.flush();

                Map<String, Object> response = (Map<String, Object>) in.readObject();
                if ("OK".equals(response.get("statut"))) {
                    return (CommandeDTO) response.get("commande");
                }
            }
        } catch (Exception e) {
            System.err.println("[ADMIN CLIENT] Erreur GET_ORDER_DETAILS : " + e.getMessage());
        }
        return null;
    }
}