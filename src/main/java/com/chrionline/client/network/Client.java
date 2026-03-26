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

    // Attributs UDP pour les notifications
    private DatagramSocket udpSocket;
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
     * Établit la connexion TCP avec le serveur et lance l'écouteur de notifications UDP.
     */
    public void connecter() throws IOException {
        if (socket == null || socket.isClosed()) {
            // Connexion TCP principale
            this.socket = new Socket(host, port);

            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[CLIENT] Connexion TCP établie sur le port " + port);

            // Démarrage de l'écouteur UDP
            Thread udpThread = new Thread(this::ecouterNotificationsUDP);
            udpThread.setDaemon(true);
            udpThread.start();
        }
    }

    /**
     * Envoie une requête au serveur.
     */
    public void envoyerRequete(Object requete) throws IOException {
        if (out != null) {
            out.writeObject(requete);
            out.flush();
        }
    }

    /**
     * Envoie une requête Map et attend la réponse Map associée.
     * Utile pour les appels simples synchrone (comme Profil ou Commandes).
     */
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> envoyerRequeteAttendreReponse(java.util.Map<String, Object> requete) {
        try {
            envoyerRequete((Object) requete);
            return (java.util.Map<String, Object>) lireReponse();
        } catch (Exception e) {
            java.util.Map<String, Object> err = new java.util.HashMap<>();
            err.put("statut", "ERREUR");
            err.put("message", "Erreur réseau : " + e.getMessage());
            return err;
        }
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
    public Object lireReponse() throws IOException, ClassNotFoundException {
        if (in != null) {
            return in.readObject();
        }
        return null;
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
            } catch (java.net.BindException e) {
                // Repli sur un port libre aléatoire
                udpSocket = new DatagramSocket(0);
                actualUdpPort = udpSocket.getLocalPort();
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

                // Alerte JavaFX
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Mise à jour de commande");
                    alert.setHeaderText("Notification reçue");
                    alert.setContentText(notification);
                    alert.showAndWait();
                });
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