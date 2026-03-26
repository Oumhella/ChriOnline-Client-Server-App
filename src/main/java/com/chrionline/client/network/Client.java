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
    private int selectedUdpPort = -1; // Port dynamique choisi à l'exécution

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

    /**
     * Établit la connexion TCP avec le serveur et lance l'écouteur de notifications UDP.
     */
    public void connecter() throws IOException {
        if (socket == null || socket.isClosed()) {
            // Connexion TCP principale pour les opérations (Auth, Panier, Commande)
            this.socket = new Socket(host, port);

            // Initialisation des flux d'objets pour l'envoi/réception de modèles (Produit, User)
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush(); // Crucial pour envoyer l'en-tête du flux immédiatement
            this.in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[CLIENT] Connexion TCP établie sur le port " + port);

            // Démarrage de l'écouteur UDP en arrière-plan (Thread Daemon)
            Thread udpThread = new Thread(this::ecouterNotificationsUDP);
            udpThread.setDaemon(true);
            udpThread.start();
        }
    }

    /**
     * Envoie une requête au serveur (ex: Authentification, Ajout au panier).
     */
    public synchronized void envoyerRequete(Object requete) throws IOException {
        if (out != null) {
            out.writeObject(requete);
            out.flush();
            out.reset(); // Crucial pour éviter d'envoyer d'anciennes versions d'objets modifiés
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
     * Écoute les paquets UDP envoyés par la méthode diffuserNotification du serveur.
     */
    private void ecouterNotificationsUDP() {
        try {
            // Utiliser port 0 pour laisser l'OS choisir un port libre (évite conflit sur localhost)
            udpSocket = new DatagramSocket(0);
            this.selectedUdpPort = udpSocket.getLocalPort();
            byte[] buffer = new byte[1024];
            System.out.println("[UDP] Écoute des notifications sur le port " + selectedUdpPort);

            while (!udpSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String notification = new String(packet.getData(), 0, packet.getLength());

                System.out.println("[NOTIFICATION REÇUE] " + notification);

                // Transmettre la notification à l'UI si un listener est défini
                if (notificationListener != null) {
                    javafx.application.Platform.runLater(() -> {
                        notificationListener.accept(notification);
                    });
                }
            }
        } catch (java.net.SocketException e) {
            // Socket fermée proprement à la déconnexion, pas une erreur
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