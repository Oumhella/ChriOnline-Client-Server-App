package com.chrionline.admin.network;

import com.chrionline.shared.dto.CommandeDTO;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client réseau admin — connexion sécurisée via SSL.
 */
public class AdminCommandeClient {

    private final String SERVER_IP  = "127.0.0.1";
    private final int    SERVER_PORT = 12345;

    private SSLSocketFactory getSSLSocketFactory() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
            }
        };
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
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