package com.chrionline.server.core;

import com.chrionline.server.utils.AppLogger;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.chrionline.server.dao.NotificationDAO;
import com.chrionline.server.dao.UserDAO;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Classe principale du serveur ChriOnline.
 * Gère les connexions TCP multi-clients et les notifications UDP.
 */

public class Server {

    // Attributs
    private int port;
    private ServerSocket serverSocket;
    private List<ClientHandler> clientConnectes;
    private ExecutorService threadPool;
    private ConnectionSecurityManager securityManager;

    // Port UDP séparé pour les notifications (port TCP + 1 par convention)
    private static final int UDP_PORT = 9091;

    // Constructeur

    public Server(int port) {
        this.port = port;
        this.clientConnectes = new ArrayList<>();
        this.threadPool = Executors.newFixedThreadPool(50); // Limite de 50 threads concurrents
        this.securityManager = new ConnectionSecurityManager();
    }

    // ─── Méthodes principales ─────────────────────────────────────────────────

    /**
     * Démarre le serveur : ouvre le ServerSocket TCP (SSL) et commence à accepter
     * les connexions.
     */
    public void demarrer() {
        try {
            // Chargement de la configuration SSL
            java.util.Properties props = new java.util.Properties();
            try (java.io.InputStream in = getClass().getClassLoader().getResourceAsStream("server.properties")) {
                if (in != null)
                    props.load(in);
            }

            String ksName = props.getProperty("server.ssl.keystore", "keystore.jks");
            String ksPass = props.getProperty("server.ssl.password", "password123");

            char[] password = ksPass.toCharArray();
            java.security.KeyStore ks = java.security.KeyStore.getInstance("JKS");
            try (java.io.InputStream ksIn = getClass().getClassLoader().getResourceAsStream(ksName)) {
                if (ksIn == null)
                    throw new IOException("Keystore introuvable : " + ksName);
                ks.load(ksIn, password);
            }

            javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            javax.net.ssl.SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            // Utilisation du backlog de 10000 pour simuler la tolérance au flood avant
            // rejet OS
            serverSocket = ssf.createServerSocket(port, 10000);

            AppLogger.info("[SERVER-SSL] Démarré sur le port " + port + " avec TLS");
            System.out.println("[SURVEILLANCE & LOGS] OS SYN Cookies : Tolérés (gestion au niveau OS).");
            AppLogger.info("[SERVER] En attente de connexions sécurisées...");

            // Lancer le thread UDP pour les notifications en parallèle
            Thread udpThread = new Thread(this::ecouterUDP);
            udpThread.setDaemon(true);
            udpThread.start();

            // Boucle principale d'acceptation des clients TCP
            while (!serverSocket.isClosed()) {
                accepterConnexion();
            }

        } catch (Exception e) {
            AppLogger.error("[SERVER] Erreur au démarrage SSL : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Arrête proprement le serveur et ferme toutes les connexions actives.
     */
    public void arreter() {
        try {
            AppLogger.info("[SERVER] Arrêt en cours...");

            // Déconnecter tous les clients
            for (ClientHandler handler : clientConnectes) {
                handler.fermerConnexion();
            }
            clientConnectes.clear();

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdownNow();
            }

            AppLogger.info("[SERVER] Arrêté avec succès.");
        } catch (IOException e) {
            AppLogger.error("[SERVER] Erreur lors de l'arrêt : " + e.getMessage());
        }
    }

    /**
     * Accepte une nouvelle connexion client TCP et lui attribue un ClientHandler
     * dans un thread dédié.
     */
    public void accepterConnexion() {
        try {
            Socket socketClient = serverSocket.accept();
            String clientIp = socketClient.getInetAddress().getHostAddress();

            // 1. Vérification de sécurité (Protection DoS / SYN Flood) via
            // ConnectionSecurityManager
            if (!securityManager.isAllowed(clientIp)) {
                AppLogger.warn("[SERVER] Connexion rejetée (Bloqué/BLACKLIST) : " + clientIp);
                socketClient.close();
                return;
            }

            AppLogger.info("[SERVER] Nouveau client connecté : " + clientIp);

            // 1.bis. Réduire le temps d'attente (soTimeout) pour libérer les ressources si
            // inactif - AUGMENTE A 5 MINUTES (300000ms) POUR LA PAGE DE CONNEXION
            socketClient.setSoTimeout(300000);

            ClientHandler handler = new ClientHandler(socketClient, this);
            clientConnectes.add(handler);

            // 2. Utilisation du ThreadPool pour la gestion des threads clients
            threadPool.execute(handler);

        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                AppLogger.error("[SERVER] Erreur lors de l'acceptation d'une connexion : " + e.getMessage());
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
        AppLogger.info("[SERVER] Client déconnecté. Clients actifs : " + clientConnectes.size());
    }

    /**
     *
     * Diffuse une notification UDP à une adresse/port donnés.
     *
     * @param message       le message de notification
     * @param adresseClient l'adresse IP du client destinataire
     * @param portClient    le port UDP du client destinataire
     */
    public void diffuserNotification(String message, InetAddress adresseClient, int portClient) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, adresseClient, portClient);
            udpSocket.send(packet);
            AppLogger.info("[UDP] Notification envoyée à "
                    + adresseClient.getHostAddress() + ":" + portClient + " → " + message);
        } catch (IOException e) {
            AppLogger.error("[UDP] Erreur d'envoi de notification : " + e.getMessage());
        }

    }

    /**
     * Envoie une notification UDP à tous les administrateurs connectés et la
     * sauvegarde en BDD.
     *
     * @param message le message de notification à envoyer aux admins
     * @param type    le type de notification
     */
    public void notifierAdmins(String message, String type) {
        for (ClientHandler handler : clientConnectes) {
            if ("admin".equals(handler.getRole())) {
                diffuserNotification(message, handler.getSocket().getInetAddress(), handler.getUdpPort());
                // Sauvegarde en BDD pour chaque admin
                NotificationDAO.save(handler.getUserId(), message, type);
            }
        }
    }

    /**
     * Envoie une notification UDP à TOUS les clients connectés et la sauvegarde en
     * BDD pour TOUS les utilisateurs.
     * 
     * @param message le message de notification
     * @param type    le type de notification
     */
    public void notifierTousLesClients(String message, String type) {
        // 1. Notifier les connectés via UDP
        for (ClientHandler handler : clientConnectes) {
            diffuserNotification(message, handler.getSocket().getInetAddress(), handler.getUdpPort());
        }

        // 2. Sauvegarder en BDD pour TOUS les utilisateurs inscrits (Newsletter/Alerte
        // globale)
        // Note: On pourrait aussi ne sauvegarder que pour les connectés,
        // mais une newsletter doit être visible par tous à leur prochaine connexion.
        new Thread(() -> {
            List<Map<String, Object>> users = UserDAO.listerClients();
            for (Map<String, Object> u : users) {
                int uid = (int) u.get("idUtilisateur");
                NotificationDAO.save(uid, message, type);
            }
        }).start();
    }

    /**
     * Envoie une notification UDP à un client spécifique et la sauvegarde en BDD.
     *
     * @param userId  l'ID unique de l'utilisateur à notifier
     * @param message le message de notification
     * @param type    le type de notification
     */
    public void notifierClient(int userId, String message, String type) {
        // Sauvegarde en BDD (pour que l'utilisateur la voie même s'il n'est pas
        // connecté à l'instant T)
        NotificationDAO.save(userId, message, type);

        // Notification UDP si connecté
        for (ClientHandler handler : clientConnectes) {
            if (handler.getUserId() == userId) {
                diffuserNotification(message, handler.getSocket().getInetAddress(), handler.getUdpPort());
            }
        }
    }

    // ─── Rate Limiting UDP (Protection Anti-Flood) ────────────────────────────
    private static final int MAX_UDP_PACKETS_PER_SECOND = 20;
    private static final long UDP_BLOCK_DURATION_MS = 10000; // 10 secondes
    private final Map<InetAddress, UdpRateData> udpRateLimits = new ConcurrentHashMap<>();

    private static class UdpRateData {
        long lastResetTime = System.currentTimeMillis();
        int packetCount = 0;
        boolean isBlocked = false;
        long blockUntil = 0;
    }

    /**
     * Vérifie si l'adresse IP est soumise à une limitation de taux UDP.
     */
    private boolean isUdpRateLimited(InetAddress address) {
        long now = System.currentTimeMillis();
        UdpRateData data = udpRateLimits.computeIfAbsent(address, k -> new UdpRateData());

        // Si l'IP est déjà bloquée
        if (data.isBlocked) {
            if (now > data.blockUntil) {
                // Débloquage après la pénalité
                data.isBlocked = false;
                data.packetCount = 0;
                data.lastResetTime = now;
            } else {
                return true; // Toujours bloqué
            }
        }

        // Remise à zéro chaque seconde
        if (now - data.lastResetTime > 1000) {
            data.packetCount = 0;
            data.lastResetTime = now;
        }

        data.packetCount++;

        // Bloquer l'IP en cas de flood (Dépassement du nombre max de requêtes)
        if (data.packetCount > MAX_UDP_PACKETS_PER_SECOND) {
            System.err.println("[SECURITY] Flood UDP détecté depuis " + address.getHostAddress() + ". IP bloquée pour 10 secondes.");
            data.isBlocked = true;
            data.blockUntil = now + UDP_BLOCK_DURATION_MS;
            return true;
        }

        return false;
    }

    // ─── Thread UDP (écoute des messages entrants UDP) ─────────────────────────

    /**
     * Écoute les messages UDP entrants.
     * Tourne dans un thread daemon.
     */
    private void ecouterUDP() {
        try (DatagramSocket udpSocket = new DatagramSocket(UDP_PORT)) {
            AppLogger.info("[UDP] En écoute sur le port " + UDP_PORT);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                InetAddress clientAddress = packet.getAddress();
                
                // Vérifier si l'adresse est limitée en taux (Protection Flood UDP)
                if (isUdpRateLimited(clientAddress)) {
                    // Les paquets sont ignorés silencieusement pour économiser des ressources 
                    continue; 
                }

                String messageRecu = new String(packet.getData(), 0, packet.getLength());
                AppLogger.info("[UDP] Message reçu de "
                        + clientAddress.getHostAddress() + " : " + messageRecu);
            }

        } catch (IOException e) {
            AppLogger.error("[UDP] Erreur socket UDP : " + e.getMessage());
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