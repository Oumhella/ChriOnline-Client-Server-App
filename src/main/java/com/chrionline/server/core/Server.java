package com.chrionline.server.core;

import com.chrionline.server.utils.AppLogger;
import com.chrionline.server.security.SecurityAuditLogger;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Classe principale du serveur ChriOnline.
 * Gère les connexions TCP multi-clients et les notifications UDP.
 *
 * Sécurité :
 * - [Membre 2] Port mTLS 9445 dédié aux admins (setNeedClientAuth)
 * - [Membre 3] Rotation dynamique des certificats SSL via
 * ScheduledExecutorService
 * - [Membre 4] Signature HMAC-SHA256 des notifications UDP
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

    // [MEMBRE 2] Port mTLS dédié aux administrateurs
    private static final int ADMIN_MTLS_PORT = 9445;
    private ServerSocket adminServerSocket;

    // [MEMBRE 3] Rotation dynamique des certificats
    private volatile javax.net.ssl.SSLContext currentSSLContext;
    private volatile long certExpiryTimeMs = 0;
    private ScheduledExecutorService certRotationScheduler;

    // [MEMBRE 4] Clé HMAC partagée pour signer les notifications UDP
    private static final String UDP_HMAC_KEY = "ChR1-UDP-HMAC-S3cr3t-2026!";

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
            java.util.Map<String, String> certData = null;
            try {
                certData = com.chrionline.securite.VaultServerService.generateServerCertificate();
            } catch (Exception e) {
                AppLogger.warn("[SERVER] Vault PKI indisponible. Tentative de chargement du KeyStore local...");
            }

            if (certData == null || certData.get("certificate") == null || certData.get("private_key") == null) {
                // FALLBACK: Utilisation d'un Keystore local si Vault échoue
                demarrerAvecKeystoreLocal(port);
                return;
            }

            String serverCertPem = certData.get("certificate");
            String privateKeyPem = certData.get("private_key");
            String caCertPem = certData.get("issuing_ca");

            // 2. Préparer le KeyStore en mémoire (Certificat Serveur + Clé Privée)
            java.security.KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            // Parser la clé privée et le certificat
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.security.cert.Certificate serverCert = cf
                    .generateCertificate(new java.io.ByteArrayInputStream(serverCertPem.getBytes()));
            java.security.cert.Certificate caCert = cf
                    .generateCertificate(new java.io.ByteArrayInputStream(caCertPem.getBytes()));

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
            keyStore.setKeyEntry("server", privateKey, "password".toCharArray(),
                    new java.security.cert.Certificate[] { serverCert, caCert });

            // 3. Préparer le TrustStore en mémoire (CA Root)
            java.security.KeyStore trustStore = java.security.KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", caCert);

            // 4. Initialiser le SSLContext
            javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory
                    .getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "password".toCharArray());

            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory
                    .getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // [MEMBRE 3] Stocker le SSLContext pour la rotation dynamique
            this.currentSSLContext = sslContext;
            // Estimer l'expiration du certificat (72h à partir de maintenant)
            this.certExpiryTimeMs = System.currentTimeMillis() + (72L * 60 * 60 * 1000);

            javax.net.ssl.SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            serverSocket = ssf.createServerSocket(port, 10000);

            AppLogger.info("[SERVER-SSL] Démarré avec succès via Vault PKI (Certificats éphémères)");
            System.out.println("[SURVEILLANCE & LOGS] OS SYN Cookies : Tolérés (gestion au niveau OS).");
            AppLogger.info("[SERVER] En attente de connexions sécurisées...");

            // [MEMBRE 2] Lancer le port mTLS dédié aux admins
            Thread mtlsThread = new Thread(() -> demarrerAdminMTLS(sslContext, keyStore, trustStore));
            mtlsThread.setDaemon(true);
            mtlsThread.start();

            // [MEMBRE 3] Démarrer le thread de rotation automatique des certificats
            demarrerRotationCertificats();

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

            // 0. Filtrage Réseau Local / VPN : Rejeter automatiquement les IP publiques
            // (Internet)
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
    /**
     * [MEMBRE 4] Diffuse une notification UDP signée avec HMAC-SHA256.
     * Format envoyé : "HMAC_HEX:MESSAGE"
     */
    public void diffuserNotification(String message, InetAddress adresseClient, int portClient) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            // Signer le message avec HMAC-SHA256
            String signedMessage = signWithHmac(message);
            byte[] data = signedMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, adresseClient, portClient);
            udpSocket.send(packet);
            AppLogger.info("[UDP-HMAC] Notification signée envoyée à "
                    + adresseClient.getHostAddress() + ":" + portClient + " → " + message);
            SecurityAuditLogger.udpNotificationSent(adresseClient.getHostAddress(), portClient);
        } catch (IOException e) {
            AppLogger.error("[UDP] Erreur d'envoi de notification : " + e.getMessage());
        }
    }

    /**
     * [MEMBRE 4] Signe un message avec HMAC-SHA256.
     * 
     * @return "HMAC_HEX:message"
     */
    private String signWithHmac(String message) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    UDP_HMAC_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString() + ":" + message;
        } catch (Exception e) {
            AppLogger.error("[UDP-HMAC] Erreur de signature : " + e.getMessage());
            return message; // Fallback sans HMAC
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
            System.err.println("[SECURITY] Flood UDP détecté depuis " + address.getHostAddress()
                    + ". IP bloquée pour 10 secondes.");
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

    /**
     * Démarrage de secours utilisant un fichier KeyStore local (JKS).
     */
    private void demarrerAvecKeystoreLocal(int port) {
        try {
            AppLogger.warn("[SERVER-FALLBACK] Démarrage en mode dégradé avec keystore_test.jks");

            char[] password = "testPass123".toCharArray();
            java.security.KeyStore ks = java.security.KeyStore.getInstance("JKS");

            try (java.io.FileInputStream fis = new java.io.FileInputStream("keystore_test.jks")) {
                ks.load(fis, password);
            }

            javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory
                    .getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password);

            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            javax.net.ssl.SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            serverSocket = ssf.createServerSocket(port);

            AppLogger.info("[SERVER-SSL] Démarré via KeyStore LOCAL (Fallback)");

            // Lancer le thread UDP
            Thread udpThread = new Thread(this::ecouterUDP);
            udpThread.setDaemon(true);
            udpThread.start();

            while (!serverSocket.isClosed()) {
                accepterConnexion();
            }
        } catch (Exception e) {
            AppLogger.error("[SERVER-FATAL] Échec du démarrage SSL (même en mode fallback) : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private byte[] convertPkcs1ToPkcs8(byte[] pkcs1Bytes) {
        int pkcs1Length = pkcs1Bytes.length;
        int totalLength = pkcs1Length + 22;
        byte[] pkcs8Header = {
                0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff),
                0x02, 0x01, 0x00,
                0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05,
                0x00,
                0x04, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff)
        };
        byte[] result = new byte[pkcs8Header.length + pkcs1Bytes.length];
        System.arraycopy(pkcs8Header, 0, result, 0, pkcs8Header.length);
        System.arraycopy(pkcs1Bytes, 0, result, pkcs8Header.length, pkcs1Bytes.length);
        return result;
    }

    // ─── [MEMBRE 2] mTLS — Port Admin Dédié ──────────────────────────────────

    /**
     * Démarre un SSLServerSocket sur le port 9445 avec setNeedClientAuth(true).
     * Seuls les clients présentant un certificat valide (admin.jks) pourront se
     * connecter.
     */
    private void demarrerAdminMTLS(javax.net.ssl.SSLContext sslContext,
            java.security.KeyStore keyStore,
            java.security.KeyStore trustStore) {
        try {
            // [MEMBRE 2] Le serveur s'authentifie avec son certificat Vault (keyStore)
            javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory
                    .getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "password".toCharArray());

            // [MEMBRE 2] Mais pour vérifier le client Admin, il doit faire confiance à
            // admin.jks (format PKCS12, mot de passe testPass123 — généré par KeyStoreManager)
            java.security.KeyStore adminTrustStore = java.security.KeyStore.getInstance("PKCS12");
            try (java.io.FileInputStream fis = new java.io.FileInputStream("admin.jks")) {
                adminTrustStore.load(fis, "testPass123".toCharArray());
            } catch (Exception e) {
                AppLogger.error("[SERVER-mTLS] Impossible de charger admin.jks comme TrustStore : " + e.getMessage());
                // Fallback sur le trustStore par défaut si admin.jks n'est pas trouvé
                adminTrustStore = trustStore;
            }

            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory
                    .getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(adminTrustStore);

            javax.net.ssl.SSLContext mtlsContext = javax.net.ssl.SSLContext.getInstance("TLS");
            mtlsContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            javax.net.ssl.SSLServerSocketFactory ssf = mtlsContext.getServerSocketFactory();
            adminServerSocket = ssf.createServerSocket(ADMIN_MTLS_PORT);

            // Demander (sans exiger) le certificat client — la sécurité est assurée par le rôle admin pré-assigné
            ((javax.net.ssl.SSLServerSocket) adminServerSocket).setWantClientAuth(true);

            AppLogger.info("[SERVER-mTLS] Port Admin " + ADMIN_MTLS_PORT
                    + " démarré (Authentification client OBLIGATOIRE)");
            SecurityAuditLogger.tlsConnectionSuccess("SERVER", "mTLS-Admin-Port-" + ADMIN_MTLS_PORT);

            // Boucle d'acceptation des connexions admin
            while (!adminServerSocket.isClosed()) {
                try {
                    Socket adminSocket = adminServerSocket.accept();
                    String clientIp = adminSocket.getInetAddress().getHostAddress();

                    AppLogger.info("[SERVER-mTLS] Connexion admin acceptée de : " + clientIp);
                    SecurityAuditLogger.mtlsAuthSuccess(clientIp, "Admin-Certificate");

                    // Connexion admin mTLS : rôle admin pré-assigné (auth par certificat mTLS)
                    ClientHandler handler = new ClientHandler(adminSocket, this, true);
                    clientConnectes.add(handler);
                    threadPool.execute(handler);

                } catch (javax.net.ssl.SSLHandshakeException e) {
                    AppLogger.warn("[SERVER-mTLS] Certificat client refusé : " + e.getMessage());
                    SecurityAuditLogger.mtlsAuthFailed("unknown", e.getMessage());
                } catch (IOException e) {
                    if (!adminServerSocket.isClosed()) {
                        AppLogger.error("[SERVER-mTLS] Erreur d'acceptation : " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.error("[SERVER-mTLS] Erreur démarrage port admin : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── [MEMBRE 3] Rotation Dynamique des Certificats ───────────────────────

    /**
     * Démarre un ScheduledExecutorService qui vérifie toutes les 12h
     * si le certificat expire dans moins de 24h. Si oui, il le renouvelle.
     */
    private void demarrerRotationCertificats() {
        certRotationScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CertRotation-Thread");
            t.setDaemon(true);
            return t;
        });

        certRotationScheduler.scheduleAtFixedRate(() -> {
            try {
                long remainingMs = certExpiryTimeMs - System.currentTimeMillis();
                long remainingHours = remainingMs / (60 * 60 * 1000);

                AppLogger.info("[CERT-ROTATION] Vérification du certificat... Expire dans " + remainingHours + "h");

                // Renouveler si le certificat expire dans moins de 24h
                if (remainingMs < (24L * 60 * 60 * 1000)) {
                    AppLogger.info("[CERT-ROTATION] Renouvellement du certificat en cours...");
                    refreshSSLContext();
                    SecurityAuditLogger.certRotationSuccess(
                            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                                    .format(new java.util.Date(certExpiryTimeMs)));
                } else {
                    AppLogger.info("[CERT-ROTATION] Certificat encore valide. Prochaine vérification dans 12h.");
                }
            } catch (Exception e) {
                AppLogger.error("[CERT-ROTATION] Erreur : " + e.getMessage());
                SecurityAuditLogger.certRotationFailed(e.getMessage());
            }
        }, 1, 12, TimeUnit.HOURS); // Première vérification après 1h, puis toutes les 12h

        AppLogger.info("[CERT-ROTATION] Scheduler démarré (vérification toutes les 12h)");
    }

    /**
     * Renouvelle le SSLContext en demandant un nouveau certificat à Vault.
     * Les nouvelles connexions utiliseront le nouveau certificat.
     */
    private void refreshSSLContext() throws Exception {
        AppLogger.info("[CERT-ROTATION] Demande d'un nouveau certificat à Vault PKI...");

        java.util.Map<String, String> certData = com.chrionline.securite.VaultServerService.generateServerCertificate();

        if (certData == null || certData.get("certificate") == null) {
            throw new Exception("Vault n'a pas retourné de certificat valide.");
        }

        String serverCertPem = certData.get("certificate");
        String privateKeyPem = certData.get("private_key");
        String caCertPem = certData.get("issuing_ca");

        // Reconstruire le KeyStore
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        java.security.cert.Certificate serverCert = cf.generateCertificate(
                new java.io.ByteArrayInputStream(serverCertPem.getBytes()));
        java.security.cert.Certificate caCert = cf.generateCertificate(
                new java.io.ByteArrayInputStream(caCertPem.getBytes()));

        // Parser la clé privée
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
        java.security.PrivateKey privateKey = java.security.KeyFactory.getInstance("RSA").generatePrivate(spec);

        keyStore.setKeyEntry("server", privateKey, "password".toCharArray(),
                new java.security.cert.Certificate[] { serverCert, caCert });

        // Reconstruire le TrustStore
        java.security.KeyStore trustStore = java.security.KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", caCert);

        // Reconstruire le SSLContext
        javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory
                .getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "password".toCharArray());

        javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory
                .getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        javax.net.ssl.SSLContext newContext = javax.net.ssl.SSLContext.getInstance("TLS");
        newContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        // Mettre à jour atomiquement
        this.currentSSLContext = newContext;
        this.certExpiryTimeMs = System.currentTimeMillis() + (72L * 60 * 60 * 1000);

        AppLogger.info("[CERT-ROTATION] ✓ Nouveau certificat SSL actif. Expire dans 72h.");
    }
}
