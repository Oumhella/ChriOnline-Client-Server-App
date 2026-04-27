package com.chrionline.server.core;

import com.chrionline.server.utils.AppLogger;
import com.chrionline.server.dao.UserDAO;
import com.chrionline.server.security.SecurityLogger;
import com.chrionline.server.security.SecurityInterceptor;
import com.chrionline.server.service.AuthenticationService;
import com.chrionline.server.service.EmailService;
import com.chrionline.server.service.InputValidator;
import com.chrionline.server.service.PanierService;
import com.chrionline.server.service.ProduitService;
import com.chrionline.shared.models.User;
import com.chrionline.server.dao.CommandeDAO;
import com.chrionline.server.dao.LigneCommandeDAO;
import com.chrionline.server.service.CommandeService;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LigneCommandeDTO;
import com.chrionline.shared.enums.StatutCommande;
import com.chrionline.database.DatabaseConnection;
import java.sql.Connection;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Gère la communication réseau (TCP/UDP) avec un client connecté.
 * Cette classe se concentre sur le transport et la gestion de session de base.
 * La logique métier sera déléguée aux DAOs/Controllers plus tard.
 */
@SuppressWarnings("unchecked")
public class ClientHandler implements Runnable {

    /**
     * Commandes TCP ne nécessitant pas de session (LOGIN/REGISTER + parcours invité + déconnexion).
     */
    private static final Set<String> COMMANDES_PUBLIQUES = Set.of(
            "CONNEXION",
            "LOGIN_ADMIN",
            "ADMIN_INIT_SECURITY",
            "ADMIN_GET_CHALLENGE",
            "ADMIN_LOGIN_CHALLENGE",
            "ADMIN_VERIFY_TOTP",
            "INSCRIPTION",
            "CONFIRMER_EMAIL",
            "OUBLIER_MOT_DE_PASSE",
            "REINITIALISER_MDP",
            "VERIFIER_OTP",
            "LISTE_PRODUITS",
            "DETAIL_PRODUIT",
            "GET_PRODUIT_BY_ID",
            "LISTE_CATEGORIES",
            "LISTE_LABELS",
            "LISTE_LABEL_VALUES",
            "SUIVRE_COMMANDE",
            "DECONNEXION",
            "UDP_REGISTER",
            "REGISTER_UDP",
            "INCONNUE"
    );

    private final Socket socket;
    private final Server server;
    private final AuthenticationService authService;
    private final ProduitService produitService;
    private final PanierService panierService;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private int udpPort = 9092; // Port par défaut

    // État de la session client
    private int userId = -1;
    private String userEmail = null;
    private String userRole      = null;

    // ─── Commandes réservées aux administrateurs ─────────────────────────────
    private static final Set<String> ADMIN_COMMANDS = Set.of(
        "AJOUTER_PRODUIT", "MODIFIER_PRODUIT", "SUPPRIMER_PRODUIT",
        "UPLOAD_IMAGE",
        "AJOUTER_LABEL", "AJOUTER_LABEL_VALUE", "SUPPRIMER_LABEL_VALUE",
        "AJOUTER_CATEGORIE", "MODIFIER_CATEGORIE", "SUPPRIMER_CATEGORIE",
        "ADMIN_LISTE_USERS", "ADMIN_CHANGER_STATUT_USER",
        "ENVOYER_NEWSLETTER",
        "GET_ALL_ORDERS", "UPDATE_ORDER_STATUS"
    );

    // Pour injecter le nouveau sessionId après régénération globale
    private String nextSessionIdToInject = null;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.produitService = new ProduitService();
        this.authService = new AuthenticationService();
        this.panierService = new PanierService();
    }

    // ─── Gestion de la Connexion TCP ──────────────────────────────────────────

    @Override
    public void run() {
        try {
            // Initialisation des flux
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());

            AppLogger.info("[HANDLER] Client connecté : " + socket.getInetAddress().getHostAddress());

            Object requete;
            while ((requete = in.readObject()) != null) {
                traiterRequete(requete);
            }

        } catch (EOFException | SocketException e) {
            AppLogger.info("[HANDLER] Déconnexion du client.");
        } catch (IOException | ClassNotFoundException e) {
            AppLogger.error("[HANDLER] Erreur réseau : " + e.getMessage());
        } finally {
            server.gererDeconnexion(this);
            fermerConnexion();
        }
    }

    // ─── Dispatcher de Requêtes ───────────────────────────────────────────────

    /**
     * Reçoit un objet (Map) et l'oriente vers le bon traitement.
     */
    @SuppressWarnings("unchecked")
    private void traiterRequete(Object objet) {
        if (!(objet instanceof Map)) {
            envoyerMessage(creerReponse("ERREUR", "Format de protocole invalide."));
            return;
        }

        Map<String, Object> req = (Map<String, Object>) objet;
        String commande = (String) req.getOrDefault("commande", "INCONNUE");
        String socketIp = socket.getInetAddress().getHostAddress();

        AppLogger.info("[HANDLER] Reçu : " + commande + " de " + socketIp);

        // ─── SÉCURITÉ : Blacklist + Détection IP Spoofing ────────────────────────
        SecurityInterceptor.ValidationResult validation = SecurityInterceptor.validateRequest(commande, req, socketIp);
        if (!validation.isOk()) {
            AppLogger.warn("[SECURITY] Requête rejetée pour IP " + socketIp + " : " + validation.getError());
            envoyerMessage(creerReponse("ERREUR_SECURITE", validation.getError()));
            return;
        }
        // ─────────────────────────────────────────────────────────────────────────

        String clientIp = socket.getInetAddress().getHostAddress();

        // 1. Adapter la durée de session (timeout dynamique en ms)
        long dynamicTimeoutMs = 15L * 60 * 1000; // Défaut : 15 min
        if (commande.startsWith("PANIER_") || commande.startsWith("COMMANDE_")) {
            dynamicTimeoutMs = 5L * 60 * 1000; // Transactions critiques : 5 min
        } else if (commande.startsWith("ADMIN_") || commande.startsWith("AJOUTER_") || commande.startsWith("MODIFIER_") || commande.startsWith("SUPPRIMER_")) {
            dynamicTimeoutMs = 10L * 60 * 1000; // Opérations admin : 10 min
        }

        if (!COMMANDES_PUBLIQUES.contains(commande)) {
            String sessionId = (String) req.get("sessionId");
            com.chrionline.server.session.Session session =
                    com.chrionline.server.session.SessionManager.validateSession(sessionId, clientIp, dynamicTimeoutMs);
            if (session == null) {
                rejeterSessionInvalide(clientIp, commande, sessionId);
                return;
            }
            appliquerIdentiteDepuisSession(session);
            injecterUtilisateurDansRequete(req);

            // 2. Régénération automatique et constante ("roulement") de l'ID après chaque transaction valide
            this.nextSessionIdToInject = com.chrionline.server.session.SessionManager.regenerateSession(sessionId, clientIp);
            req.put("sessionId", this.nextSessionIdToInject);
        } else {
            this.nextSessionIdToInject = null;
        }

        // ─── Contrôle d'accès : Autorisation Admin (Command Injection Prevention) ──
        if (ADMIN_COMMANDS.contains(commande) && !"admin".equals(userRole)) {
            System.out.println("[SECURITY] Commande admin '" + commande + "' rejetée : rôle='"
                    + userRole + "' userId=" + userId + " (" + clientIp + ")");
            envoyerMessage(creerReponse("ERREUR", "Accès refusé. Droits administrateur requis."));
            return;
        }

        switch (commande) {
            case "CONNEXION" -> handleConnexion(req);
            case "LOGIN_ADMIN" -> handleLoginAdmin(req);
            case "ADMIN_GET_CHALLENGE" -> handleAdminGetChallenge(req);
            case "ADMIN_LOGIN_CHALLENGE" -> handleAdminLoginChallenge(req);
            case "ADMIN_INIT_SECURITY" -> handleAdminInitSecurity(req);
            case "ADMIN_VERIFY_TOTP" -> handleAdminVerifyTotp(req);
            case "INSCRIPTION" -> handleInscription(req);
            case "LISTE_PRODUITS" -> handleListeProduits(req);
            case "DETAIL_PRODUIT", "GET_PRODUIT_BY_ID" -> handleDetailProduit(req);
            case "AJOUTER_WISHLIST" -> handleAjouterWishlist(req);
            case "SUPPRIMER_WISHLIST" -> handleSupprimerWishlist(req);
            case "LISTE_WISHLIST" -> handleListeWishlist(req);
            case "CONFIRMER_EMAIL" -> handleConfirmerEmail(req);
            case "OUBLIER_MOT_DE_PASSE" -> handleOublierMotDePasse(req);
            case "REINITIALISER_MDP" -> handleReinitialiserMdp(req);
            case "UDP_REGISTER" -> {
                this.udpPort = (int) req.getOrDefault("port", 9092);
                System.out.println("[HANDLER] Port UDP enregistré pour client " + userId + " : " + udpPort);
            }
            case "PANIER_GET" -> envoyerMessage(panierService.getPanier(req));
            case "PANIER_AJOUTER" -> envoyerMessage(panierService.ajouterProduit(req));
            case "PANIER_MODIFIER_QTE" -> envoyerMessage(panierService.modifierQuantite(req));
            case "PANIER_RETIRER" -> envoyerMessage(panierService.retirerProduit(req));
            case "PANIER_VIDER" -> envoyerMessage(panierService.viderPanier(req));
            case "GET_PROFIL" -> handleGetProfil(req);
            case "UPDATE_PROFIL" -> handleUpdateProfil(req);
            case "GET_MY_ORDERS" -> handleGetMyOrders(req);
            case "SUIVRE_COMMANDE" -> handleSuivreCommande(req);

            // Admin Produits
            case "AJOUTER_PRODUIT" -> handleAjouterProduit(req);
            case "MODIFIER_PRODUIT" -> handleModifierProduit(req);
            case "SUPPRIMER_PRODUIT" -> handleSupprimerProduit(req);
            case "UPLOAD_IMAGE" -> handleUploadImage(req);
            case "LISTE_CATEGORIES" -> handleListeCategories(req);
            case "LISTE_LABELS" -> handleListeLabels(req);
            case "LISTE_LABEL_VALUES" -> handleListeLabelValues(req);
            case "AJOUTER_LABEL" -> handleAjouterLabel(req);
            case "AJOUTER_LABEL_VALUE" -> handleAjouterLabelValue(req);
            case "SUPPRIMER_LABEL_VALUE" -> handleSupprimerLabelValue(req);
            case "AJOUTER_CATEGORIE" -> handleAjouterCategorie(req);
            case "MODIFIER_CATEGORIE" -> handleModifierCategorie(req);
            case "SUPPRIMER_CATEGORIE" -> handleSupprimerCategorie(req);
            case "APPLY_DISCOUNT_CATEGORIE" -> envoyerMessage(produitService.handleApplyDiscountCategorie(req));

            case "PANIER_VALIDER" -> handlePanierValider(req);
            case "COMMANDE_CONFIRMER" -> handleCommandeConfirmer(req);
            case "GET_ALL_ORDERS",
                    "GET_ORDER_DETAILS",
                    "UPDATE_ORDER_STATUS" -> {
                handleAdminCommande(commande, req);
            }
            case "ADMIN_LISTE_USERS" -> {
                if (!"admin".equals(userRole)) {
                    SecurityLogger.accesNonAutorise("ADMIN_LISTE_USERS", userId, userRole,
                            socket.getInetAddress().getHostAddress());
                    envoyerMessage(creerReponse("ERREUR", "Accès refusé."));
                } else {
                    envoyerMessage(com.chrionline.server.service.AdminUserService.handleListerClients());
                }
            }
            case "ADMIN_CHANGER_STATUT_USER" -> {
                if (!"admin".equals(userRole)) {
                    SecurityLogger.accesNonAutorise("ADMIN_CHANGER_STATUT_USER", userId, userRole,
                            socket.getInetAddress().getHostAddress());
                    envoyerMessage(creerReponse("ERREUR", "Accès refusé."));
                } else {
                    req.put("adminId", this.userId);
                    envoyerMessage(com.chrionline.server.service.AdminUserService.handleChangerStatutClient(req));
                }
            }
            case "ENVOYER_NEWSLETTER" -> handleEnvoyerNewsletter(req);

            case "REGISTER_UDP" -> {
                this.udpPort = (int) req.getOrDefault("udpPort", 9092);
                System.out.println(
                        "[HANDLER] Port UDP enregistré pour " + (userId != -1 ? userId : "guest") + " : " + udpPort);
            }

            case "VERIFIER_OTP" -> handleVerifierOTP(req);

            // Sécurité Monitoring (Admin)
            case "ADMIN_BLOCK_IP" -> handleBlockIP(req);
            case "DECONNEXION"          -> handleDeconnexion(req, clientIp);
            // ... autres commandes ...
            default -> envoyerMessage(creerReponse("ERREUR", "Commande non reconnue : " + commande));
        }
    }

    // ─── Placeholders pour la logique (À déplacer en DAO plus tard) ─────────────

    private void handleConnexion(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleConnexion appelée");
        // Injecter l'IP client pour que UserDAO puisse la journaliser
        req.put("clientIp", socket.getInetAddress().getHostAddress());
        try {
            // Injecter l'IP cliente pour la liste noire et le logging
            req.put("clientIp", socket.getInetAddress().getHostAddress());
            Map<String, Object> reponse = authService.login(req);

            System.out.println("[HANDLER] Login statut = " + reponse.get("statut"));

            // Note: On n'établit pas la session (userId, etc.) tant que le 2FA n'est pas OK
            if ("OK".equals(reponse.get("statut"))) {
                enrichirReponseConnexionAvecSession(reponse, req);
            }

            envoyerMessage(reponse);
        } catch (Exception e) {
            System.err.println("[HANDLER] Exception handleConnexion : " + e.getMessage());
            e.printStackTrace();
            SecurityLogger.erreurServeur("handleConnexion", e.getMessage());
            envoyerMessage(creerReponse("ERREUR", "Erreur technique : " + e.getMessage()));
        }
    }

    private void handleVerifierOTP(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleVerifierOTP appelée");
        // Injecter l'IP pour le logging de sécurité
        req.put("clientIp", socket.getInetAddress().getHostAddress());
        try {
            Map<String, Object> reponseMutable = new java.util.HashMap<>(authService.verifierOTPConnexion(req));

            if ("OK".equals(reponseMutable.get("statut"))) {
                // Initialize session upon successful 2FA
                enrichirReponseConnexionAvecSession(reponseMutable, req);

                Map<String, Object> data = (Map<String, Object>) reponseMutable.get("data");
                if (data != null && data.containsKey("userId")) {
                    this.userId = (int) data.get("userId");
                    this.userEmail = (String) data.get("email");
                    this.userRole = (String) data.get("role");

                    // RESTAURATION : Enregistrement du succès dans le tableau de bord de sécurité (log simple)
                    SecurityLogger.loginSucces(userEmail, userRole, userId, socket.getInetAddress().getHostAddress());
                }
            }

            envoyerMessage(reponseMutable);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur verification OTP : " + e.getMessage()));
        }
    }

    private void handleLoginAdmin(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleLoginAdmin appelée");
        String email = (String) req.get("email");
        String mdp   = (String) req.get("mdp");
        
        try {
            String sql = "SELECT u.*, a.idAdmin FROM utilisateur u JOIN admin a ON u.idUtilisateur = a.idAdmin WHERE u.email = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setString(1, email);
                java.sql.ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    if (org.mindrot.jbcrypt.BCrypt.checkpw(mdp, rs.getString("password"))) {
                        // Success!
                        this.userId = rs.getInt("idUtilisateur");
                        this.userEmail = rs.getString("email");
                        this.userRole = "admin";
                        
                        Map<String, Object> data = new HashMap<>();
                        data.put("userId", this.userId);
                        data.put("email", this.userEmail);
                        data.put("role", this.userRole);
                        data.put("nom", rs.getString("nom"));
                        data.put("prenom", rs.getString("prenom"));
                        
                        Map<String, Object> rep = new HashMap<>();
                        rep.put("statut", "OK");
                        rep.put("message", "Authentification Admin réussie.");
                        rep.put("data", data);
                        
                        enrichirReponseConnexionAvecSession(rep, req);
                        envoyerMessage(rep);
                        
                        // Sécurité
                        SecurityLogger.loginSucces(this.userEmail, this.userRole, this.userId, socket.getInetAddress().getHostAddress());
                        return;
                    }
                }
            }
            SecurityLogger.loginEchec(email, socket.getInetAddress().getHostAddress());
            envoyerMessage(creerReponse("ERREUR", "Identifiants invalides ou droits insuffisants."));
        } catch (Exception e) {
            SecurityLogger.erreurServeur("handleLoginAdmin", e.getMessage());
            envoyerMessage(creerReponse("ERREUR", "Erreur serveur : " + e.getMessage()));
        }
    }

    private void handleInscription(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleInscription appelée");
        try {
            Map<String, Object> reponse = authService.register(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur lors de l'inscription : " + e.getMessage()));
        }
    }

    private void handleConfirmerEmail(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleConfirmerEmail");
        try {
            Map<String, Object> reponse = authService.confirmerEmail(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur confirmation : " + e.getMessage()));
        }
    }

    private static class ChallengeData {
        String challenge;
        long timestamp;
        ChallengeData(String challenge) {
            this.challenge = challenge;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > 30_000; // 30 secondes
        }
    }

    private static final Map<String, ChallengeData> pendingChallenges = new java.util.concurrent.ConcurrentHashMap<>();
    // Sessions admin en attente de vérification TOTP (email → timestamp de validation RSA)
    private static final Map<String, Long> pendingTotpSessions = new java.util.concurrent.ConcurrentHashMap<>();

    private void handleAdminInitSecurity(Map<String, Object> req) {
        String email  = (String) req.get("email");
        String mdp    = (String) req.get("mdp");
        String pubKey = (String) req.get("publicKey");

        try {
            // 1. Vérifier que l'email appartient à un administrateur
            String sql = "SELECT a.idAdmin FROM utilisateur u JOIN admin a ON u.idUtilisateur = a.idAdmin WHERE u.email = ?";
            try (Connection conn = com.chrionline.database.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                java.sql.ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    envoyerMessage(creerReponse("ERREUR", "Cet email n'est pas un compte administrateur."));
                    SecurityLogger.loginEchec(email, socket.getInetAddress().getHostAddress());
                    return;
                }
            }

            // 2. Mettre à jour le mot de passe (BCrypt), stocker la clé publique ET générer le secret TOTP
            String totpSecret = com.chrionline.server.dao.UserDAO.initAdminSecurity(email, mdp, pubKey);
            if (totpSecret != null) {
                // Générer l'URI otpauth pour le QR Code
                String otpauthUri = com.chrionline.securite.TOTPService.generateOtpAuthUri(totpSecret, email);
                
                Map<String, Object> resp = creerReponse("OK", "Sécurité Admin initialisée avec succès.");
                resp.put("totpSecret", totpSecret);
                resp.put("otpauthUri", otpauthUri);
                envoyerMessage(resp);
                
                SecurityLogger.logSecurityEvent("ADMIN_INIT_SECURITY", email,
                        socket.getInetAddress().getHostAddress(), "SUCCESS");
            } else {
                envoyerMessage(creerReponse("ERREUR", "Échec de la mise à jour en base de données."));
            }
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur serveur : " + e.getMessage()));
        }
    }

    private void handleAdminGetChallenge(Map<String, Object> req) {
        String email = (String) req.get("email");
        String clientIp = socket.getInetAddress().getHostAddress();

        if (email == null || email.isBlank()) {
            envoyerMessage(creerReponse("ERREUR", "Email requis."));
            return;
        }

        // 1. Protection Brute Force : Vérifier si le compte est bloqué (même sans mot de passe, on protège l'email)
        Map<String, Object> lockStatus = com.chrionline.server.dao.UserDAO.checkAccountLock(email, clientIp);
        if (lockStatus != null) {
            envoyerMessage(lockStatus);
            return;
        }

        // 2. Vérifier que l'email appartient à un admin
        try {
            String sqlCheck = "SELECT a.idAdmin FROM utilisateur u JOIN admin a ON u.idUtilisateur = a.idAdmin WHERE u.email = ?";
            try (Connection conn = com.chrionline.database.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setString(1, email);
                if (!ps.executeQuery().next()) {
                    envoyerMessage(creerReponse("ERREUR", "Accès refusé."));
                    return;
                }
            }
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur serveur : " + e.getMessage()));
            return;
        }

        // 3. Vérifier si la clé publique est déjà enregistrée
        String pubKey = com.chrionline.server.dao.UserDAO.getPublicKey(email);
        if (pubKey == null || pubKey.isBlank()) {
            envoyerMessage(creerReponse("ERREUR_NO_KEY", "Sécurité RSA non initialisée pour cet admin."));
            return;
        }
        
        String challenge = com.chrionline.securite.ChallengeGenerator.generateChallenge();
        pendingChallenges.put(email, new ChallengeData(challenge));
        
        Map<String, Object> resp = creerReponse("OK", "Challenge généré");
        resp.put("challenge", challenge);
        envoyerMessage(resp);
    }

    private void handleAdminLoginChallenge(Map<String, Object> req) {
        String email = (String) req.get("email");
        String signatureBase64 = (String) req.get("signature");
        String clientIp = socket.getInetAddress().getHostAddress();
        
        // 1. Protection Brute Force : Vérifier si le compte est bloqué
        Map<String, Object> lockStatus = com.chrionline.server.dao.UserDAO.checkAccountLock(email, clientIp);
        if (lockStatus != null) {
            envoyerMessage(lockStatus);
            return;
        }

        ChallengeData challengeData = pendingChallenges.get(email);
        if (challengeData == null) {
            envoyerMessage(creerReponse("ERREUR", "Aucun challenge en cours pour cet email. Demandez-en un nouveau."));
            return;
        }

        // 2. Vérification de l'expiration du challenge (30s)
        if (challengeData.isExpired()) {
            pendingChallenges.remove(email);
            envoyerMessage(creerReponse("ERREUR", "Le challenge a expiré (limite de 30 secondes). Veuillez en redemander un."));
            return;
        }
        
        String challenge = challengeData.challenge;
        
        try {
            // 1. Récupérer la clé publique
            String pubKeyBase64 = com.chrionline.server.dao.UserDAO.getPublicKey(email);
            if (pubKeyBase64 == null) {
                envoyerMessage(creerReponse("ERREUR_NO_KEY", "Sécurité RSA non initialisée pour cet admin."));
                return;
            }
            
            // 2. Décoder la clé publique et la signature
            byte[] pubKeyBytes = java.util.Base64.getDecoder().decode(pubKeyBase64);
            byte[] sigBytes = java.util.Base64.getDecoder().decode(signatureBase64);
            
            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(pubKeyBytes);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            java.security.PublicKey publicKey = kf.generatePublic(spec);
            
            // 3. Vérifier la signature
            if (com.chrionline.securite.Verifier.verify(challenge, sigBytes, publicKey)) {
                // Signature RSA valide → demander le code TOTP (2ème facteur)
                pendingChallenges.remove(email);
                com.chrionline.server.dao.UserDAO.resetFailedAttempts(email);
                
                // Stocker l'email dans les sessions en attente de TOTP
                pendingTotpSessions.put(email, System.currentTimeMillis());
                
                Map<String, Object> rep = creerReponse("REQUIRES_TOTP", 
                    "Signature RSA valide. Entrez le code de votre application Authenticator.");
                envoyerMessage(rep);
                
                SecurityLogger.logSecurityEvent("ADMIN_RSA_OK", email,
                        socket.getInetAddress().getHostAddress(), "AWAITING_TOTP");
            } else {
                // Échec de la signature : incrémenter les tentatives
                Map<String, Object> failRes = com.chrionline.server.dao.UserDAO.handleLoginFailure(email, clientIp);
                envoyerMessage(failRes);
            }
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur lors de la vérification : " + e.getMessage()));
        }
    }

    /**
     * Vérifie le code TOTP soumis par l'admin (3ème facteur après mdp + RSA).
     * Finalise la session admin si le code est valide.
     */
    private void handleAdminVerifyTotp(Map<String, Object> req) {
        String email = (String) req.get("email");
        String totpCode = (String) req.get("totpCode");
        String clientIp = socket.getInetAddress().getHostAddress();

        if (email == null || totpCode == null) {
            envoyerMessage(creerReponse("ERREUR", "Email et code TOTP requis."));
            return;
        }

        // 1. Vérifier qu'une session TOTP est en attente pour cet email
        Long rsaTimestamp = pendingTotpSessions.get(email);
        if (rsaTimestamp == null) {
            envoyerMessage(creerReponse("ERREUR", "Aucune session RSA validée. Recommencez la connexion."));
            return;
        }

        // 2. Vérifier que la session TOTP n'a pas expiré (2 minutes après la validation RSA)
        if (System.currentTimeMillis() - rsaTimestamp > 120_000) {
            pendingTotpSessions.remove(email);
            envoyerMessage(creerReponse("ERREUR", "Session TOTP expirée. Recommencez la connexion."));
            return;
        }

        // 3. Récupérer le secret TOTP de l'admin
        String totpSecret = com.chrionline.server.dao.UserDAO.getTotpSecret(email);
        if (totpSecret == null) {
            envoyerMessage(creerReponse("ERREUR", "TOTP non configuré pour cet admin."));
            return;
        }

        // 4. Vérifier le code TOTP
        if (!com.chrionline.securite.TOTPService.verifyCode(totpSecret, totpCode)) {
            envoyerMessage(creerReponse("ERREUR", "Code TOTP invalide. Vérifiez votre application Authenticator."));
            SecurityLogger.logSecurityEvent("ADMIN_TOTP_FAIL", email, clientIp, "INVALID_CODE");
            return;
        }

        // 5. Succès ! Nettoyer et finaliser la session
        pendingTotpSessions.remove(email);

        try {
            String sql = "SELECT u.*, a.idAdmin FROM utilisateur u JOIN admin a ON u.idUtilisateur = a.idAdmin WHERE u.email = ?";
            try (Connection conn = com.chrionline.database.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    this.userId = rs.getInt("idUtilisateur");
                    this.userEmail = rs.getString("email");
                    this.userRole = "admin";

                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", this.userId);
                    data.put("email", this.userEmail);
                    data.put("role", this.userRole);
                    data.put("nom", rs.getString("nom"));
                    data.put("prenom", rs.getString("prenom"));

                    Map<String, Object> rep = new HashMap<>();
                    rep.put("statut", "OK");
                    rep.put("message", "Authentification 3 facteurs réussie.");
                    rep.put("data", data);

                    enrichirReponseConnexionAvecSession(rep, req);
                    envoyerMessage(rep);

                    SecurityLogger.loginSucces(this.userEmail, this.userRole, this.userId, clientIp);
                }
            }
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur serveur : " + e.getMessage()));
        }
    }

    private void handleBlockIP(Map<String, Object> req) {
        if (!"admin".equals(userRole)) {
            envoyerMessage(creerReponse("ERREUR", "Accès refusé."));
            return;
        }
        String ip = (String) req.get("ip");
        SecurityLogger.blockIP(ip);
        envoyerMessage(creerReponse("OK", "IP bloquée avec succès."));
    }

    private void handleOublierMotDePasse(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleOublierMotDePasse");
        try {
            Map<String, Object> reponse = authService.oublierMotDePasse(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur : " + e.getMessage()));
        }
    }

    private void handleReinitialiserMdp(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleReinitialiserMdp");
        try {
            Map<String, Object> reponse = authService.reinitialiserMotDePasse(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur : " + e.getMessage()));
        }
    }

    private void handlePanierValider(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handlePanierValider");
        try {
            Map<String, Object> reponse = panierService.validerPanier(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur réseau : " + e.getMessage()));
        }
    }

    /** Paiement : 2FA simulé géré dans {@link com.chrionline.server.service.PanierService#confirmerCommande(Map)} (clé {@code payment2faCode}). */
    private void handleCommandeConfirmer(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleCommandeConfirmer");
        try {
            Map<String, Object> reponse = panierService.confirmerCommande(req);

            // Si la commande est validée avec succès, on notifie les administrateurs via
            // UDP
            if ("OK".equals(reponse.get("statut"))) {
                CommandeDTO recap = (CommandeDTO) reponse.get("commandeResult");
                String ref = recap != null ? recap.getReference() : "Inconnue";
                String messageAdmin = "NOUVELLE_COMMANDE:" + ref + ":Utilisateur " + this.userId;
                server.notifierAdmins(messageAdmin, "commande");

                // Notifier les admins pour chaque produit en alerte de stock
                if (recap != null && recap.getAlertesStock() != null && !recap.getAlertesStock().isEmpty()) {
                    List<String> adminEmails = com.chrionline.server.dao.UserDAO.getAllAdminEmails();
                    for (String alerte : recap.getAlertesStock()) {
                        // "nomProduit:stock=X:seuil=Y"
                        String msgAlerte = "STOCK_ALERTE:" + alerte;
                        server.notifierAdmins(msgAlerte, "stock");
                        System.out.println("[HANDLER] Notification stock alerte envoyée : " + msgAlerte);

                        // --- Envoi d'email aux admins ---
                        new Thread(() -> {
                            try {
                                String[] parts = alerte.split(":");
                                String nomP = parts.length > 0 ? parts[0] : "Inconnu";
                                String stockP = parts.length > 1 ? parts[1].replace("stock=", "") : "?";
                                String seuilP = parts.length > 2 ? parts[2].replace("seuil=", "") : "?";
                                for (String emailAdmin : adminEmails) {
                                    com.chrionline.server.service.EmailService.envoyerAlerteStock(emailAdmin, nomP,
                                            stockP, seuilP);
                                }
                            } catch (Exception ex) {
                                System.err.println("[HANDLER] Erreur email alerte stock : " + ex.getMessage());
                            }
                        }).start();
                    }
                }
            }

            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur serveur : " + e.getMessage()));
        }
    }

    private void handleListeProduits(Map<String, Object> req) {
        try {

            Map<String, Object> reponse = produitService.handleListeProduits(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur lors de la récupération des produits : " + e.getMessage()));
        }
    }

    private void handleDetailProduit(Map<String, Object> req) {
        try {
            Map<String, Object> reponse = produitService.handleGetProduitById(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur lors de la récupération du produit : " + e.getMessage()));
        }
    }

    private void handleAjouterWishlist(Map<String, Object> req) {
        try {
            Map<String, Object> reponse = new com.chrionline.server.service.WishlistService()
                    .handleAjouterWishlist(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur réseau : " + e.getMessage()));
        }
    }

    private void handleSupprimerWishlist(Map<String, Object> req) {
        try {
            Map<String, Object> reponse = new com.chrionline.server.service.WishlistService()
                    .handleSupprimerWishlist(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur réseau : " + e.getMessage()));
        }
    }

    private void handleListeWishlist(Map<String, Object> req) {
        try {
            Map<String, Object> reponse = new com.chrionline.server.service.WishlistService().handleGetWishlist(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur réseau : " + e.getMessage()));
        }
    }

    private void handleGetProfil(Map<String, Object> req) {
        req.put("userId", this.userId);
        envoyerMessage(authService.getProfil(req));
    }

    private void handleUpdateProfil(Map<String, Object> req) {
        req.put("userId", this.userId);
        Map<String, Object> reponse = authService.updateProfil(req);
        envoyerMessage(reponse);
    }

    private void handleGetMyOrders(Map<String, Object> req) {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            CommandeService service = new CommandeService(new CommandeDAO(conn), new LigneCommandeDAO(conn));
            List<CommandeDTO> orders = service.getCommandesByClient(this.userId);
            Map<String, Object> res = new HashMap<>();
            res.put("statut", "OK");
            res.put("commandes", new ArrayList<>(orders));
            envoyerMessage(res);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleAjouterProduit(Map<String, Object> req) {
        try {
            Map<String, Object> resp = produitService.handleAjouterProduit(req);
            envoyerMessage(resp);

            // Si succès, on notifie tous les clients
            if ("OK".equals(resp.get("statut"))) {
                String nom = (String) req.get("nom");
                server.notifierTousLesClients("NOUVEAU_PRODUIT:" + nom, "nouveaute");
            }
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleModifierProduit(Map<String, Object> req) {
        try {
            envoyerMessage(produitService.handleModifierProduit(req));
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleSupprimerProduit(Map<String, Object> req) {
        try {
            envoyerMessage(produitService.handleSupprimerProduit(req));
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleUploadImage(Map<String, Object> req) {
        try {
            envoyerMessage(produitService.handleUploadImage(req));
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleListeCategories(Map<String, Object> req) {
        envoyerMessage(produitService.handleListeCategories(req));
    }

    private void handleListeLabels(Map<String, Object> req) {
        envoyerMessage(produitService.handleListeLabels(req));
    }

    private void handleListeLabelValues(Map<String, Object> req) {
        envoyerMessage(produitService.handleListeLabelValues(req));
    }

    private void handleAjouterLabel(Map<String, Object> req) {
        envoyerMessage(produitService.handleAjouterLabel(req));
    }

    private void handleAjouterLabelValue(Map<String, Object> req) {
        envoyerMessage(produitService.handleAjouterLabelValue(req));
    }

    private void handleSupprimerLabelValue(Map<String, Object> req) {
        envoyerMessage(produitService.handleSupprimerLabelValue(req));
    }

    private void handleAjouterCategorie(Map<String, Object> req) {
        envoyerMessage(produitService.handleAjouterCategorie(req));
    }

    private void handleModifierCategorie(Map<String, Object> req) {
        envoyerMessage(produitService.handleModifierCategorie(req));
    }

    private void handleSupprimerCategorie(Map<String, Object> req) {
        envoyerMessage(produitService.handleSupprimerCategorie(req));
    }
    // ─── Gestion UDP ──────────────────────────────────────────────────────────

    /**
     * Envoie une notification via le protocole UDP (délégué au serveur).
     */
    public void envoyerNotificationUDP(String msg, int portClientUDP) {
        server.diffuserNotification(msg, socket.getInetAddress(), portClientUDP);
    }

    // ─── Utilitaires de Communication ─────────────────────────────────────────

    public synchronized void envoyerMessage(Object objet) {
        try {
            // Injection de la nouvelle session si générée
            if (objet instanceof Map && nextSessionIdToInject != null) {
                Map<String, Object> map = (Map<String, Object>) objet;
                try {
                    map.put("newSessionId", nextSessionIdToInject);
                } catch (UnsupportedOperationException e) {
                    Map<String, Object> mutableMap = new HashMap<>(map);
                    mutableMap.put("newSessionId", nextSessionIdToInject);
                    objet = mutableMap;
                }
                nextSessionIdToInject = null;
            }

            if (out != null) {
                out.writeObject(objet);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            System.err.println("[HANDLER] Échec d'envoi client : " + e.getMessage());
        }
    }

    public int getUdpPort() {
        return udpPort;
    }

    private Map<String, Object> creerReponse(String statut, String message) {
        Map<String, Object> r = new HashMap<>();
        r.put("statut", statut);
        r.put("message", message);
        return r;
    }

    private boolean isAdmin() {
        if (this.userId <= 0)
            return false;

        try (java.sql.Connection conn = com.chrionline.database.DatabaseConnection.getInstance().getConnection();
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM admin WHERE idAdmin = ?")) {
            ps.setInt(1, this.userId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true si présent dans la table admin
            }
        } catch (Exception e) {
            System.err.println("[HANDLER] Erreur isAdmin : " + e.getMessage());
            return false;
        }
    }

    public void fermerConnexion() {
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            // Ignoré
        }
    }

    // ─── Gestion des Commandes Admin ─────────────────────────────────────────

    private void handleSuivreCommande(Map<String, Object> req) {
        try {
            String reference = (String) req.get("reference");
            if (reference == null || reference.trim().isEmpty()) {
                envoyerMessage(creerReponse("ERREUR", "Reference non fournie."));
                return;
            }
            Connection conn = DatabaseConnection.getInstance().getConnection();
            CommandeService service = new CommandeService(
                    new CommandeDAO(conn),
                    new LigneCommandeDAO(conn));
            CommandeDTO dto = service.getCommandeByReference(reference);
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("statut", "OK");
            reponse.put("commande", dto);
            envoyerMessage(reponse);
        } catch (IllegalArgumentException e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur serveur : " + e.getMessage()));
        }
    }

    private void handleAdminCommande(String commande, Map<String, Object> req) {
        if (!"admin".equals(userRole)) {
            SecurityLogger.accesNonAutorise(commande, userId, userRole,
                    socket.getInetAddress().getHostAddress());
            envoyerMessage(creerReponse("ERREUR", "Accès refusé."));
            return;
        }
        switch (commande) {
            case "GET_ALL_ORDERS" -> handleGetAllOrders(req);
            case "GET_ORDER_DETAILS" -> handleGetOrderDetails(req);
            case "UPDATE_ORDER_STATUS" -> handleUpdateOrderStatus(req);
        }
    }

    private void handleGetAllOrders(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleGetAllOrders appelée — userId=" + userId + " role=" + userRole);
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            CommandeService service = new CommandeService(
                    new CommandeDAO(conn),
                    new LigneCommandeDAO(conn));
            List<CommandeDTO> commandes = service.getAllCommandes();
            System.out.println("[HANDLER] commandes trouvées : " + commandes.size());
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("statut", "OK");
            reponse.put("commandes", new java.util.ArrayList<>(commandes)); // ArrayList est sérialisable
            envoyerMessage(reponse);
        } catch (Exception e) {
            System.err.println("[HANDLER] ERREUR handleGetAllOrders : " + e.getMessage());
            e.printStackTrace(); // Stack trace complet dans la console du serveur
            envoyerMessage(creerReponse("ERREUR", e.getClass().getSimpleName() + " : " + e.getMessage()));
        }
    }

    private void handleGetOrderDetails(Map<String, Object> req) {
        try {
            String idCommande = (String) req.get("idCommande");
            Connection conn = DatabaseConnection.getInstance().getConnection();
            CommandeService service = new CommandeService(
                    new CommandeDAO(conn),
                    new LigneCommandeDAO(conn));
            CommandeDTO dto = service.getCommandeById(idCommande);
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("statut", "OK");
            reponse.put("commande", dto);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleUpdateOrderStatus(Map<String, Object> req) {
        try {
            String idCommande = (String) req.get("idCommande");
            String nouveauStatut = (String) req.get("statut");
            Connection conn = DatabaseConnection.getInstance().getConnection();

            CommandeDAO dao = new CommandeDAO(conn);
            CommandeService service = new CommandeService(dao, new LigneCommandeDAO(conn));

            // 1. Récupérer les détails avant maj pour avoir le userId et la ref
            com.chrionline.shared.models.Commande c = dao.findById(idCommande);

            // 2. Maj en BDD
            String resultat = service.updateStatut(idCommande, nouveauStatut);

            if (resultat.startsWith("SUCCESS") && c != null) {
                // 3. Notifier l'utilisateur concerné par UDP
                String msg = "VOTRE COMMANDE #" + c.getReference() + " est passée au statut : " + nouveauStatut;
                server.notifierClient(c.getIdUtilisateur(), msg, "commande");
            }

            Map<String, Object> reponse = new HashMap<>();
            reponse.put("statut", resultat.startsWith("SUCCESS") ? "OK" : "ERREUR");
            reponse.put("message", resultat);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleEnvoyerNewsletter(Map<String, Object> req) {
        if (!"admin".equals(userRole)) {
            SecurityLogger.accesNonAutorise("ENVOYER_NEWSLETTER", userId, userRole,
                    socket.getInetAddress().getHostAddress());
            envoyerMessage(creerReponse("ERREUR", "Accès refusé."));
            return;
        }

        String sujet = (String) req.get("sujet");
        String corps = (String) req.get("corps");

        if (sujet == null || sujet.isEmpty() || corps == null || corps.isEmpty()) {
            envoyerMessage(creerReponse("ERREUR", "Sujet et corps obligatoires."));
            return;
        }

        // Exécuter en arrière-plan pour ne pas bloquer le handler TCP
        new Thread(() -> {
            try {
                System.out.println("[NEWSLETTER] Démarrage de l'envoi massif...");

                // 1. Emails réels (Mailing list)
                List<String> emails = UserDAO.getAllEmails();
                int successCount = 0;
                for (String email : emails) {
                    try {
                        EmailService.envoyer(email, sujet, corps);
                        successCount++;
                    } catch (Exception e) {
                        System.err.println("[NEWSLETTER] Échec envoi à " + email + ": " + e.getMessage());
                    }
                }
                System.out.println("[NEWSLETTER] Fin d'envoi SMTP : " + successCount + "/" + emails.size());

                // 2. Notification UDP (In-app) pour les clients connectés
                // On limite la taille pour l'UDP (max ~1KB par paquet)
                String shortCorps = corps.replaceAll("<[^>]*>", ""); // Enlever HTML pour la notif UDP
                if (shortCorps.length() > 300)
                    shortCorps = shortCorps.substring(0, 300) + "...";

                server.notifierTousLesClients("NEWSLETTER:" + sujet + ":" + shortCorps, "newsletter");

            } catch (Exception e) {
                System.err.println("[NEWSLETTER] Erreur globale: " + e.getMessage());
            }
        }).start();

        Map<String, Object> res = creerReponse("OK", "Envoi en cours...");
        res.put("count", UserDAO.getAllEmails().size());
        envoyerMessage(res);
    }

    // ─── Sessions TCP (anti détournement) ─────────────────────────────────────

    private void rejeterSessionInvalide(String clientIp, String commande, String sessionId) {
        com.chrionline.server.session.SessionManager.LastValidationFailure kind =
                com.chrionline.server.session.SessionManager.getLastValidationFailure();
        if (kind == com.chrionline.server.session.SessionManager.LastValidationFailure.EXPIRED) {
            SecurityLogger.logSecurityEvent("SESSION_EXPIRED", "UNKNOWN", clientIp, "commande=" + commande + " sessionId=" + sessionId);
        } else {
            SecurityLogger.logSecurityEvent("SESSION_INVALID", "UNKNOWN", clientIp, "commande=" + commande + " sessionId=" + sessionId);
        }
        envoyerMessage(creerReponseSessionExpiree());
    }

    private void appliquerIdentiteDepuisSession(com.chrionline.server.session.Session session) {
        this.userId = session.getUserId();
        String[] ctx = UserDAO.getEmailAndRoleById(this.userId);
        if (ctx != null) {
            this.userEmail = ctx[0];
            this.userRole = ctx[1];
        } else {
            this.userEmail = null;
            this.userRole = null;
        }
    }

    private void injecterUtilisateurDansRequete(Map<String, Object> req) {
        req.put("userId", this.userId);
        req.put("idUtilisateur", this.userId);
    }

    private Map<String, Object> creerReponseSessionExpiree() {
        Map<String, Object> r = new HashMap<>();
        r.put("statut", "ERROR");
        r.put("message", "SESSION_EXPIRED");
        return r;
    }

    /**
     * Après LOGIN réussi : régénère ou crée un sessionId serveur et l'ajoute à {@code data}.
     */
    @SuppressWarnings("unchecked")
    private void enrichirReponseConnexionAvecSession(Map<String, Object> reponse, Map<String, Object> req) {
        Map<String, Object> data = (Map<String, Object>) reponse.get("data");
        if (data == null) return;
        Object uidObj = data.get("userId");
        if (!(uidObj instanceof Integer) && !(uidObj instanceof Long)) return;
        int uid = uidObj instanceof Integer ? (Integer) uidObj : ((Long) uidObj).intValue();

        String ip = socket.getInetAddress().getHostAddress();
        String oldSid = (String) req.get("sessionId");
        String newSid = com.chrionline.server.session.SessionManager.regenerateSession(oldSid, ip);
        if (newSid == null) {
            newSid = com.chrionline.server.session.SessionManager.createSession(uid, ip);
        }
        data.put("sessionId", newSid);

        this.userId = uid;
        this.userEmail = (String) data.get("email");
        this.userRole = (String) data.get("role");

        SecurityLogger.logSecurityEvent("SESSION_CREATED", this.userEmail != null ? this.userEmail : "UNKNOWN", ip, "sessionId=" + newSid);
    }

    private void handleDeconnexion(Map<String, Object> req, String clientIp) {
        String sid = (String) req.get("sessionId");
        if (sid != null && !sid.isBlank()) {
            com.chrionline.server.session.SessionManager.invalidateSession(sid);
            SecurityLogger.logSecurityEvent("LOGOUT", this.userEmail != null ? this.userEmail : "UNKNOWN", clientIp, "sessionId=" + sid);
        }
        this.userId = -1;
        this.userEmail = null;
        this.userRole = null;
        envoyerMessage(Map.of("statut", "OK", "message", "Déconnexion effectuée."));
    }

    // Getters/Setters session
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRole() {
        return userRole;
    }

    public Socket getSocket() {
        return socket;
    }

}