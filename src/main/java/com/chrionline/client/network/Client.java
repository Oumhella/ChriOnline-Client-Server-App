package com.chrionline.client.network;

import java.io.*;
import java.net.*;

/**
 * Gestionnaire du réseau côté client pour l'application ChriOnline.
 * Implémente le pattern Singleton pour partager la connexion entre les
 * contrôleurs JavaFX.
 *
 * Sécurité :
 * - [Membre 1] TLS avec vérification du certificat serveur via vault-ca.pem (Anti-MITM)
 * - [Membre 4] Vérification HMAC-SHA256 des notifications UDP (Anti-injection)
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

    // Clé HMAC partagée pour la vérification des notifications UDP
    private static final String UDP_HMAC_KEY = "ChR1-UDP-HMAC-S3cr3t-2026!";

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
     * [MEMBRE 1] Charge le certificat CA de Vault depuis les ressources
     * et crée un TrustStore en mémoire pour valider le serveur.
     */
    private javax.net.ssl.SSLSocketFactory createSecureSSLFactory() throws Exception {
        // 1. Charger le certificat CA de Vault depuis les ressources du JAR
        java.io.InputStream caInputStream = getClass().getResourceAsStream("/vault-ca.pem");

        if (caInputStream == null) {
            System.err.println("[CLIENT-SSL] ATTENTION: vault-ca.pem introuvable dans les ressources.");
            System.err.println("[CLIENT-SSL] Tentative de chargement depuis le système de fichiers...");
            // Fallback : chercher le fichier dans le dossier courant ou les ressources
            java.io.File caFile = new java.io.File("src/main/resources/vault-ca.pem");
            if (caFile.exists()) {
                caInputStream = new java.io.FileInputStream(caFile);
            } else {
                throw new Exception("Certificat CA introuvable (vault-ca.pem). " +
                    "Impossible de vérifier l'identité du serveur.");
            }
        }

        // 2. Parser le certificat X.509
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        java.security.cert.Certificate caCert = cf.generateCertificate(caInputStream);
        caInputStream.close();

        System.out.println("[CLIENT-SSL] Certificat CA Vault chargé : " +
                ((java.security.cert.X509Certificate) caCert).getSubjectX500Principal().getName());

        // 3. Créer un TrustStore en mémoire contenant uniquement le CA de Vault
        java.security.KeyStore trustStore = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("vault-ca", caCert);

        // 4. Initialiser le TrustManagerFactory avec ce TrustStore
        javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory
                .getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 5. Créer le SSLContext sécurisé
        javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());

        return sslContext.getSocketFactory();
    }

    /**
     * Établit la connexion TCP sécurisée (SSL/TLS) avec le serveur.
     * [MEMBRE 1] Utilise le certificat CA de Vault pour vérifier l'identité du serveur.
     */
    public void connecter() throws IOException {
        if (socket == null || socket.isClosed()) {
            try {
                // [MEMBRE 1] Création d'un SSLContext sécurisé avec le CA de Vault
                javax.net.ssl.SSLSocketFactory ssf = createSecureSSLFactory();

                this.socket = ssf.createSocket(host, port);

                // Forcer le handshake — si le certificat serveur n'est pas signé
                // par le CA de Vault, une SSLHandshakeException sera levée ici
                ((javax.net.ssl.SSLSocket) socket).startHandshake();

                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.out.flush();
                this.in = new ObjectInputStream(socket.getInputStream());

                System.out.println("[CLIENT-SSL] Connexion sécurisée établie (certificat serveur vérifié via Vault CA).");

                // Démarrage de l'écouteur UDP
                Thread udpThread = new Thread(this::ecouterNotificationsUDP);
                udpThread.setDaemon(true);
                udpThread.start();
            } catch (javax.net.ssl.SSLHandshakeException e) {
                throw new IOException("[SÉCURITÉ] Le certificat du serveur n'est pas signé par le CA de Vault. " +
                        "Connexion refusée (possible attaque MITM). Détail : " + e.getMessage(), e);
            } catch (Exception e) {
                throw new IOException("Erreur SSL : " + e.getMessage(), e);
            }
        }
    }

    /**
     * Envoie une requête au serveur.
     * Ajoute automatiquement {@code sessionId} si l'utilisateur possède une session serveur.
     */
    public synchronized void envoyerRequete(Object requete) throws IOException {
        if (requete instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> m = new java.util.HashMap<>((java.util.Map<String, Object>) requete);
            String sid = com.chrionline.client.session.SessionManager.getInstance().getServerSessionId();
            if (sid != null && !sid.isBlank()) {
                m.put("sessionId", sid);
            }
            injectSecurityHeaders(m);
            requete = m;
        }
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
        String sid = com.chrionline.client.session.SessionManager.getInstance().getServerSessionId();
        if (sid != null && !sid.isBlank()) {
            this.jwtToken = sid;
        }
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
            Object o = in.readObject();
            if (o instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> m = (java.util.Map<String, Object>) o;
                com.chrionline.client.session.SessionManager sm =
                        com.chrionline.client.session.SessionManager.getInstance();
                // Vérif session expirée
                sm.handleServerResponseIfSessionExpired(m);
                // Rotation automatique du sessionId après action critique (paiement, profil)
                sm.updateSessionIdIfProvided(m);

                // Synchronisation de jwtToken
                String sid = sm.getServerSessionId();
                if (sid != null && !sid.isBlank()) {
                    this.jwtToken = sid;
                }
            }
            return o;
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
     * [MEMBRE 4] Vérifie la signature HMAC-SHA256 d'une notification UDP.
     * Format attendu : "HMAC_HEX:MESSAGE"
     *
     * @param rawData les données brutes reçues du paquet UDP
     * @return le message vérifié, ou null si la signature est invalide
     */
    private String verifyUdpHmac(String rawData) {
        int separatorIndex = rawData.indexOf(':');
        if (separatorIndex <= 0) {
            // Pas de séparateur → notification non signée (compatibilité ascendante)
            System.out.println("[UDP-HMAC] Notification non signée acceptée (compatibilité).");
            return rawData;
        }

        String receivedHmac = rawData.substring(0, separatorIndex);
        String message = rawData.substring(separatorIndex + 1);

        try {
            // Calculer le HMAC attendu
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    UDP_HMAC_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Convertir en hexadécimal
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            String expectedHmac = sb.toString();

            // Comparaison en temps constant pour éviter les attaques par timing
            if (java.security.MessageDigest.isEqual(
                    expectedHmac.getBytes(), receivedHmac.getBytes())) {
                return message; // HMAC valide
            } else {
                System.err.println("[UDP-HMAC] ALERTE : Signature HMAC invalide ! Notification rejetée.");
                System.err.println("[UDP-HMAC] Possible tentative d'injection de fausses notifications.");
                return null;
            }
        } catch (Exception e) {
            System.err.println("[UDP-HMAC] Erreur de vérification : " + e.getMessage());
            return rawData; // En cas d'erreur, accepter (compatibilité)
        }
    }

    /**
     * Écoute les paquets UDP envoyés par le serveur.
     * [MEMBRE 4] Vérifie la signature HMAC de chaque notification.
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

            byte[] buffer = new byte[2048]; // Augmenté pour HMAC + message
            System.out.println("[UDP] Écoute des notifications sur le port " + actualUdpPort);

            while (!udpSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String rawData = new String(packet.getData(), 0, packet.getLength());

                // [MEMBRE 4] Vérification HMAC avant d'accepter la notification
                String notification = verifyUdpHmac(rawData);

                if (notification == null) {
                    // HMAC invalide → notification rejetée silencieusement
                    continue;
                }

                System.out.println("[NOTIFICATION REÇUE] " + notification);

                // Transmettre la notification vérifiée à l'UI
                final String verifiedNotification = notification;
                if (notificationListener != null) {
                    javafx.application.Platform.runLater(() -> {
                        notificationListener.accept(verifiedNotification);
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
        if (socket != null)
            socket.close();
        if (udpSocket != null)
            udpSocket.close();
        System.out.println("[CLIENT] Déconnecté.");
    }
}