package com.chrionline.server.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe principale du serveur ChriOnline.
 * Gère les connexions TCP multi-clients et les notifications UDP.
 */
public class Server {

    // Attributs 
    private int port;
    private ServerSocket serverSocket;
    private List<ClientHandler> clientConnectes;

    // Port UDP séparé pour les notifications (port TCP + 1 par convention)
    private static final int UDP_PORT = 9091;

    //Constructeur 

    public Server(int port) {
        this.port = port;
        this.clientConnectes = new ArrayList<>();
    }

    // ─── Méthodes principales ─────────────────────────────────────────────────

    /**
     * Démarre le serveur : ouvre le ServerSocket TCP et commence à accepter les connexions.
     */
    public void demarrer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[SERVER] Démarré sur le port " + port);
            System.out.println("[SERVER] En attente de connexions clients...");

            // Lancer le thread UDP pour les notifications en parallèle
            Thread udpThread = new Thread(this::ecouterUDP);
            udpThread.setDaemon(true);
            udpThread.start();

            // Boucle principale d'acceptation des clients TCP
            while (!serverSocket.isClosed()) {
                accepterConnexion();
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Erreur au démarrage : " + e.getMessage());
        }
    }

    /**
     * Arrête proprement le serveur et ferme toutes les connexions actives.
     */
    public void arreter() {
        try {
            System.out.println("[SERVER] Arrêt en cours...");

            // Déconnecter tous les clients
            for (ClientHandler handler : clientConnectes) {
                handler.fermerConnexion();
            }
            clientConnectes.clear();

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            System.out.println("[SERVER] Arrêté avec succès.");
        } catch (IOException e) {
            System.err.println("[SERVER] Erreur lors de l'arrêt : " + e.getMessage());
        }
    }

    /**
     * Accepte une nouvelle connexion client TCP et lui attribue un ClientHandler dans un thread dédié.
     */
    public void accepterConnexion() {
        try {
            Socket socketClient = serverSocket.accept();
            System.out.println("[SERVER] Nouveau client connecté : "
                    + socketClient.getInetAddress().getHostAddress());

            ClientHandler handler = new ClientHandler(socketClient, this);
            clientConnectes.add(handler);

            Thread clientThread = new Thread(handler);
            clientThread.start();

        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                System.err.println("[SERVER] Erreur lors de l'acceptation d'une connexion : " + e.getMessage());
            }
        }
    }

    /**
     * Envoie une réponse à un client spécifique via son handler.
     *
     * @param handler le ClientHandler du destinataire
     * @param reponse le message à envoyer
     */
    public void envoyerReponse(ClientHandler handler, String reponse) {
        handler.envoyerMessage(reponse);
    }

    /**
     * Retire un client de la liste des clients connectés (appelé à la déconnexion).
     *
     * @param handler le ClientHandler à retirer
     */
    public void gererDeconnexion(ClientHandler handler) {
        clientConnectes.remove(handler);
        System.out.println("[SERVER] Client déconnecté. Clients actifs : " + clientConnectes.size());
    }

    /**
     * Diffuse une notification UDP à une adresse/port donnés.
     *
     * @param message        le message de notification
     * @param adresseClient  l'adresse IP du client destinataire
     * @param portClient     le port UDP du client destinataire
     */
    public void diffuserNotification(String message, InetAddress adresseClient, int portClient) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, adresseClient, portClient);
            udpSocket.send(packet);
            System.out.println("[UDP] Notification envoyée à "
                    + adresseClient.getHostAddress() + ":" + portClient + " → " + message);
        } catch (IOException e) {
            System.err.println("[UDP] Erreur d'envoi de notification : " + e.getMessage());
        }
    }

    // ─── Thread UDP (écoute des messages entrants UDP) ─────────────────────────

    /**
     * Écoute les messages UDP entrants.
     * Tourne dans un thread daemon.
     */
    private void ecouterUDP() {
        try (DatagramSocket udpSocket = new DatagramSocket(UDP_PORT)) {
            System.out.println("[UDP] En écoute sur le port " + UDP_PORT);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String messageRecu = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[UDP] Message reçu de "
                        + packet.getAddress().getHostAddress() + " : " + messageRecu);
            }

        } catch (IOException e) {
            System.err.println("[UDP] Erreur socket UDP : " + e.getMessage());
        }
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public int getPort() {
        return port;
    }

    public List<ClientHandler> getClientConnectes() {
        return clientConnectes;
    }
}