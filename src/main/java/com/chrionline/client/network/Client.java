package com.chrionline.client.network;

import java.io.*;
import java.net.*;

/**
 * Gestionnaire du réseau côté client pour l'application ChriOnline.
 * Implémente le pattern Singleton pour partager la connexion entre les contrôleurs JavaFX.
 */
public class Client {
    // Attributs TCP
    private static Client instance;
    private String host;
    private int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String jwtToken; // Stockage du token de session

    // Attributs UDP pour les notifications
    private DatagramSocket udpSocket;
    private int selectedUdpPort = -1; // Port dynamique choisi à l'exécution
    private int actualUdpPort = 9092;
    private static final int CLIENT_UDP_PORT = 9092;

    // Constructeur privé pour le pattern Singleton
    private Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Retourne l'instance unique du client (Singleton).
     */
    public static synchronized Client getInstance(String host, int port) {
        if (instance == null) {
            instance = new Client(host, port);
        }
        return instance;
    }

    public static synchronized Client getInstance() {
        return instance;
    }

    /**
     * Établit la connexion TCP sécurisée (SSL/TLS) avec le serveur.
     */
    public void connecter() throws IOException {
        if (socket == null || socket.isClosed()) {
            try {
                // Pour cette démonstration, on fait confiance à tous les certificats (auto-signés)
                java.util.Properties props = new java.util.Properties();
                try (java.io.InputStream in = getClass().getClassLoader().getResourceAsStream("server.properties")) {
                    if (in != null) props.load(in);
                }

                javax.net.ssl.SSLSocketFactory ssf = (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
                this.socket = ssf.createSocket(host, port);
                
                // Forcer le handshake
                ((javax.net.ssl.SSLSocket)socket).startHandshake();

                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.out.flush();
                this.in = new ObjectInputStream(socket.getInputStream());

                System.out.println("[CLIENT-SSL] Connexion sécurisée établie.");

                // Démarrage de l'écouteur UDP
                Thread udpThread = new Thread(this::ecouterNotificationsUDP);
                udpThread.setDaemon(true);
                udpThread.start();
            } catch (Exception e) {
                throw new IOException("Erreur SSL : " + e.getMessage(), e);
            }
        }
    }

    /**
     * Envoie une requête au serveur.
     */
    public synchronized void envoyerRequete(Object requete) throws IOException {
        if (out != null) {
            out.writeObject(requete);
            out.flush();
            out.reset(); // Crucial pour éviter d'envoyer d'anciennes versions d'objets modifiés
        }
    }

    /**
     * Envoie une requête Map et attend la réponse Map associée.
     * Utile pour les appels simples synchrone (comme Profil ou Commandes).
     */
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> envoyerRequeteAttendreReponse(java.util.Map<String, Object> requete) {
        try {
            // Injection automatique des headers de sécurité
            injectSecurityHeaders(requete);
            
            envoyerRequete((Object) requete);
            java.util.Map<String, Object> reponse = (java.util.Map<String, Object>) lireReponse();
            
            // Si c'est une connexion réussie, on sauvegarde le JWT
            if (reponse != null && "OK".equals(reponse.get("statut")) && reponse.containsKey("jwt")) {
                this.jwtToken = (String) reponse.get("jwt");
                System.out.println("[CLIENT] JWT de session mis à jour.");
            }
            
            return reponse;
        } catch (Exception e) {
            java.util.Map<String, Object> err = new java.util.HashMap<>();
            err.put("statut", "ERREUR");
            err.put("message", "Erreur réseau : " + e.getMessage());
            return err;
        }
    }

    private void injectSecurityHeaders(java.util.Map<String, Object> req) {
        if (jwtToken != null) {
            req.put("jwt", jwtToken);
        }
        // Token pare-feu pour les accès "internes" simulés
        req.put("firewallToken", "CHRI-FW-2026-SECRET-X91");
        // IP revendiquée (pour test IP Spoofing)
        // req.put("claimedIp", "192.168.1.100"); 
    }

    /**
     * Enregistre le port UDP actuel auprès du serveur.
     */
    public void enregistrerUDP() {
        try {
            java.util.Map<String, Object> req = new java.util.HashMap<>();
            req.put("commande", "UDP_REGISTER");
            req.put("port", actualUdpPort);
            envoyerRequete((Object) req);
            System.out.println("[CLIENT] Port UDP " + actualUdpPort + " enregistré sur le serveur.");
        } catch (java.io.IOException e) {
            System.err.println("[CLIENT] Erreur lors de l'enregistrement UDP : " + e.getMessage());
        }
    }

    /**
     * Reçoit une réponse du serveur.
     */
    public synchronized Object lireReponse() throws IOException, ClassNotFoundException {
        if (in != null) {
            return in.readObject();
        }
        return null;
    }

    public int getUdpPort() {
        return selectedUdpPort;
    }

    // Ecouteur pour la couche UI
    private java.util.function.Consumer<String> notificationListener;

    public void setNotificationListener(java.util.function.Consumer<String> listener) {
        this.notificationListener = listener;
    }

    /**
     * Écoute les paquets UDP envoyés par le serveur.
     */
    private void ecouterNotificationsUDP() {
        try {
            try {
                // Tentative sur le port par défaut
                udpSocket = new DatagramSocket(CLIENT_UDP_PORT);
                actualUdpPort = CLIENT_UDP_PORT;
                this.selectedUdpPort = actualUdpPort;
            } catch (java.net.BindException e) {
                // Repli sur un port libre aléatoire
                udpSocket = new DatagramSocket(0);
                actualUdpPort = udpSocket.getLocalPort();
                this.selectedUdpPort = actualUdpPort;
                System.out.println("[UDP] Port " + CLIENT_UDP_PORT + " occupé, repli sur le port " + actualUdpPort);
            }

            byte[] buffer = new byte[1024];
            System.out.println("[UDP] Écoute des notifications sur le port " + actualUdpPort);

            while (!udpSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String notification = new String(packet.getData(), 0, packet.getLength());

                System.out.println("[NOTIFICATION REÇUE] " + notification);

                // Transmettre la notification à l'UI
                if (notificationListener != null) {
                    javafx.application.Platform.runLater(() -> {
                        notificationListener.accept(notification);
                    });
                }

            }
        } catch (java.net.SocketException e) {
            System.out.println("[UDP] Socket fermée.");
        } catch (java.io.IOException e) {
            System.err.println("[UDP] Erreur : " + e.getMessage());
        }
    }

    /**
     * Ferme proprement les sockets et les flux.
     */
    public void deconnecter() throws IOException {
        if (socket != null) socket.close();
        if (udpSocket != null) udpSocket.close();
        System.out.println("[CLIENT] Déconnecté.");
    }
}