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
            AppLogger.info("[VAULT-PKI] Récupération dynamique des certificats SSL...");

            // 1. Récupérer les données depuis Vault
            java.util.Map<String, String> certData = com.chrionline.securite.VaultServerService.generateServerCertificate();
            String serverCertPem = certData.get("certificate");
            String privateKeyPem = certData.get("private_key");
            String caCertPem = certData.get("issuing_ca");

            if (serverCertPem == null || privateKeyPem == null || caCertPem == null) {
                throw new Exception("Erreur Vault PKI : Un des certificats est null (Cert=" + (serverCertPem != null) + 
                                   ", Key=" + (privateKeyPem != null) + ", CA=" + (caCertPem != null) + ")");
            }

            // 2. Préparer le KeyStore en mémoire (Certificat Serveur + Clé Privée)
            java.security.KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            // Parser la clé privée et le certificat
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.security.cert.Certificate serverCert = cf.generateCertificate(new java.io.ByteArrayInputStream(serverCertPem.getBytes()));
            java.security.cert.Certificate caCert = cf.generateCertificate(new java.io.ByteArrayInputStream(caCertPem.getBytes()));

            // 3. Charger la clé privée (Gestion PKCS#1 vs PKCS#8)
            byte[] pkDer;
            if (privateKeyPem.contains("BEGIN RSA PRIVATE KEY")) {
                String base64 = privateKeyPem
                        .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                        .replace("-----END RSA PRIVATE KEY-----", "")
                        .replaceAll("\\s+", "");
                pkDer = convertPkcs1ToPkcs8(java.util.Base64.getDecoder().decode(base64));
            } else {
                String base64 = privateKeyPem
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s+", "");
                pkDer = java.util.Base64.getDecoder().decode(base64);
            }

            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(pkDer);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            java.security.PrivateKey privateKey = kf.generatePrivate(spec);

            // Ajouter au KeyStore
            keyStore.setKeyEntry("server", privateKey, "password".toCharArray(), new java.security.cert.Certificate[]{serverCert, caCert});

            // 3. Préparer le TrustStore en mémoire (CA Root)
            java.security.KeyStore trustStore = java.security.KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", caCert);

            // 4. Initialiser le SSLContext
            javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "password".toCharArray());

            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            javax.net.ssl.SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            serverSocket = ssf.createServerSocket(port, 10000);

            AppLogger.info("[SERVER-SSL] Démarré avec succès via Vault PKI (Certificats éphémères)");
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
            InetAddress clientAddr = socketClient.getInetAddress();
            String clientIp = clientAddr.getHostAddress();

            // 0. Filtrage Réseau Local / VPN : Rejeter automatiquement les IP publiques (Internet)
            if (!clientAddr.isSiteLocalAddress() && !clientAddr.isLoopbackAddress()) {
                AppLogger.warn("[SÉCURITÉ] Connexion EXTERNE rejetée depuis " + clientIp
                        + " (seules les connexions réseau local / VPN sont autorisées)");
                socketClient.close();
                return;
            }

            // 1. Vérification de sécurité (Protection DoS / SYN Flood) via
            // ConnectionSecurityManager
            if (!securityManager.isAllowed(clientIp)) {
                AppLogger.warn("[SERVER] Connexion rejetée (Bloqué/BLACKLIST) : " + clientIp);
                socketClient.close();
                return;
            }

            AppLogger.info("[SERVER] Nouveau client connecté (réseau local) : " + clientIp);

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

    private byte[] convertPkcs1ToPkcs8(byte[] pkcs1Bytes) {
        int pkcs1Length = pkcs1Bytes.length;
        int totalLength = pkcs1Length + 22;
        byte[] pkcs8Header = {
            0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff),
            0x02, 0x01, 0x00,
            0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00,
            0x04, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff)
        };
        byte[] result = new byte[pkcs8Header.length + pkcs1Bytes.length];
        System.arraycopy(pkcs8Header, 0, result, 0, pkcs8Header.length);
        System.arraycopy(pkcs1Bytes, 0, result, pkcs8Header.length, pkcs1Bytes.length);
        return result;
    }
}