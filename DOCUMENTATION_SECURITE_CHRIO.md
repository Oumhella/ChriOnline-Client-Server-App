# 🔐 CARTE & RÉPERTOIRE INTERACTIF DE SÉCURITÉ — CHRIOONLINE
> **UAE-ENSAT 2026 — Soutenance Projet de Sécurité Applicative**
>
> Ce document sert de cartographie de sécurité interactive pour le projet **ChriOnline**. Utilisez le raccourci `Ctrl+F` pour rechercher un concept ou un composant, puis **cliquez directement sur les liens** pour ouvrir le code source et la méthode correspondante dans votre IDE.

---

## 🏛️ PARTIE 1 : L'ÉCOSYSTÈME HASHICORP VAULT (EaaS & Secrets)

L'intégralité des clés de sécurité, certificats SSL et opérations cryptographiques de ChriOnline est gérée de façon centralisée par **HashiCorp Vault**. L'application n'utilise aucun secret statique ou clé codée en dur.

```
                    ┌──► Moteur KV v2 (secret/) ──► Stocke clés publiques RSA & Mots de passe JKS
                    │
[Serveur ChriOnline]├──► Moteur Transit (transit/) ──► Chiffre / Déchiffre les colonnes de la BDD (AES-GCM)
                    │
                    └──► Moteur PKI (pki/) ──► Émet dynamiquement les certificats SSL (TTL: 72h)
```

### A. Authentification AppRole (Zéro Secret Statique)
Au démarrage, le serveur Java se connecte à Vault de manière dynamique sans utiliser de jeton maître :
1.  Il présente un `RoleID` et un `SecretID` (extraits des variables d'environnement système).
2.  Vault lui retourne un jeton d'accès à usage unique à durée de vie très courte (**1 heure**).
*   **Fichier d'initialisation :** [securite/VaultServerService.java](src/main/java/com/chrionline/securite/VaultServerService.java#L80) ➔ Méthode [`initConnection()`](src/main/java/com/chrionline/securite/VaultServerService.java#L80-L120)
*   **Récupération du token d'AppRole :** [securite/VaultServerService.java](src/main/java/com/chrionline/securite/VaultServerService.java#L130) ➔ Méthode [`getAppRoleToken()`](src/main/java/com/chrionline/securite/VaultServerService.java#L130-L180)

### B. Le Moteur Transit — Chiffrement d'Enveloppe (AES-256-GCM)
Le moteur Transit (`transit/`) offre le **Chiffrement en tant que Service (EaaS)**. Il est sans état (stateless) et assure que les données sensibles écrites en BDD (comme les logs d'audit) soient chiffrées sans que la clé ne sorte jamais de Vault.
*   **Algorithme sous-jacent :** **AES-256-GCM** avec clé `chrionline-data` générée par Vault.
*   **Activation du moteur Transit :** [vault/setup.sh](vault/setup.sh#L105) ➔ Commande [`vault secrets enable transit`](vault/setup.sh#L107) et création de clé [`vault write -f transit/keys/chrionline-data`](vault/setup.sh#L110)
*   **Méthode de chiffrement Java :** [securite/VaultServerService.java](src/main/java/com/chrionline/securite/VaultServerService.java#L398) ➔ Méthode [`transitEncrypt(String plaintext)`](src/main/java/com/chrionline/securite/VaultServerService.java#L398-L419)
*   **Méthode de déchiffrement Java :** [securite/VaultServerService.java](src/main/java/com/chrionline/securite/VaultServerService.java#L428) ➔ Méthode [`transitDecrypt(String ciphertext)`](src/main/java/com/chrionline/securite/VaultServerService.java#L428-L449)

### C. Le Moteur PKI (Génération et Rotation TLS)
Vault agit comme Autorité de Certification racine (`ChriOnline Root CA`). Il génère dynamiquement des certificats éphémères pour le serveur d'une durée de vie de **72 heures**.
*   **Rotation automatique toutes les 12 heures :** Un planificateur de tâches recharge en mémoire vive le nouveau certificat SSL du serveur sans aucune interruption de service.
*   **Génération PKI Vault :** [vault/setup.sh](vault/setup.sh#L66) ➔ Rôle [`vault write pki/roles/chrionline-server`](vault/setup.sh#L86-L92)
*   **Génération dynamique Java :** [securite/VaultKeystoreService.java](src/main/java/com/chrionline/securite/VaultKeystoreService.java#L58) ➔ Méthode [`genererKeystoreDynamique()`](src/main/java/com/chrionline/securite/VaultKeystoreService.java#L58-L135)
*   **Planificateur de rotation Java :** [server/core/Server.java](src/main/java/com/chrionline/server/core/Server.java#L625) ➔ Méthode [`demarrerRotationCertificat()`](src/main/java/com/chrionline/server/core/Server.java#L625-L640)

### D. Le Moteur Key-Value v2 (Stockage des Clés Statiques)
Le moteur `secret/` sert à stocker de manière versionnée les mots de passe des KeyStores et les clés publiques de validation RSA des administrateurs.
*   **Chemin des clés publiques Admin :** `secret/admin/keys/{email}`
*   **Lecture KV v2 Java :** [securite/VaultServerService.java](src/main/java/com/chrionline/securite/VaultServerService.java#L302) ➔ Méthode [`getAdminPublicKey(String email)`](src/main/java/com/chrionline/securite/VaultServerService.java#L302-L325)

---

## 🌐 PARTIE 2 : LE TUNNELING RÉSEAU (TLS & Mutual TLS)

Le serveur ChriOnline écoute sur deux ports distincts configurés avec des exigences de transport TLS asymétriques selon le niveau de sensibilité.

```
Client (Standard)  ───(Port 12345: TLS 1.3 Unidirectionnel)───► [Serveur ChriOnline]
Client (Admin)     ───(Port 9445: Mutual TLS Bidirectionnel)───► [Serveur ChriOnline]
```

### A. Port Client `12345` (TLS 1.3 Standard)
*   **Protocole :** **TLS v1.3** forcé. Assure le chiffrement des données de navigation de tous les clients.
*   **Vérification :** Le client JavaFX importe le certificat `ChriOnline Root CA` pour éliminer le risque d'attaque de type Man-in-the-Middle (MITM).
*   **Côté Serveur :** [server/core/Server.java](src/main/java/com/chrionline/server/core/Server.java#L182) ➔ Initialisation de la socket TLS standard dans [`demarrerTCP()`](src/main/java/com/chrionline/server/core/Server.java#L182-L210)
*   **Côté Client :** [client/network/Client.java](src/main/java/com/chrionline/client/network/Client.java#L80) ➔ Initialisation SSL context dans [`connecter()`](src/main/java/com/chrionline/client/network/Client.java#L80-L115)

### B. Port Administrateur `9445` (mTLS / Mutual TLS)
*   **Protocole :** **mTLS** forcé avec exigence stricte de certificat client (`NeedClientAuth = true`).
*   **Vérification :** Le serveur refuse immédiatement la socket TCP si l'ordinateur de l'administrateur ne présente pas un certificat client valide signé par la CA du projet.
*   **Fichier Serveur :** [server/core/Server.java](src/main/java/com/chrionline/server/core/Server.java#L215) ➔ Socket mTLS dédiée d'administration dans [`demarrerTCP()`](src/main/java/com/chrionline/server/core/Server.java#L215-L230)

---

## 👤 PARTIE 3 : LES AUTHENTIFICATIONS & MFA (Client & Admin)

### A. Authentification Client (2FA)
1.  **Identifiant & Empreinte BCrypt (Work Factor = 12) :**
    *   Les mots de passe ne sont pas stockés en clair mais hachés avec du sel via l'algorithme fort BCrypt.
    *   **Vérification Java :** [server/dao/UserDAO.java](src/main/java/com/chrionline/server/dao/UserDAO.java#L143) ➔ Méthode [`verifierIdentifiants(String email, String plaintextPassword)`](src/main/java/com/chrionline/server/dao/UserDAO.java#L143-L175)
2.  **Vérification SMTP active & DNS (Sécurité Inscription) :**
    *   Lors de l'inscription, le serveur effectue une vérification SMTP sur le port 25 du serveur MX de destination pour s'assurer que la boîte mail de l'utilisateur existe réellement.
    *   **Fichier :** [server/service/EmailService.java](src/main/java/com/chrionline/server/service/EmailService.java#L370) ➔ Méthode [`checkEmailExists(String email)`](src/main/java/com/chrionline/server/service/EmailService.java#L370-L395)
3.  **Intégration reCAPTCHA v2 (Masque de Rognage / Clipping) :**
    *   La WebView est configurée de façon permanente en taille de rendu desktop (`420x580`) pour forcer Google à initialiser la grille de défi à sa taille d'origine nette et sans scrollbars.
    *   Sur le formulaire, elle est encapsulée dans un `StackPane` de rognage (`302x78`) avec un masque de type rectangle pour ne laisser voir que la case *"Je ne suis pas un robot"*.
    *   Dès que le défi se lance, la WebView est glissée dynamiquement dans un pop-up modal recréé à la volée pour éviter tout plantage du cycle de vie JavaFX (`IllegalStateException`).
    *   **Composant de Rognage & Pop-up :** [client/view/RecaptchaWidget.java](src/main/java/com/chrionline/client/view/RecaptchaWidget.java#L67-L115)
    *   **Script observateur DOM HTML :** [resources/recaptcha.html](src/main/resources/recaptcha.html#L33-L66)

### B. Authentification Administrateur (3FA — Ultra Sécurisée)
1.  **Facteur 1 — Challenge-Response RSA-2048 (Asymétrique) :**
    *   Le serveur envoie un défi aléatoire de **32 octets** cryptographiques.
    *   L'administrateur signe ce défi avec sa clé privée présente dans son keystore local (`admin.jks`).
    *   Le serveur télécharge sa clé publique depuis Vault et valide la signature cryptographique (`SHA256withRSA`).
    *   **Générateur de défi :** [securite/ChallengeGenerator.java](src/main/java/com/chrionline/securite/ChallengeGenerator.java#L14) ➔ Méthode [`genererChallenge()`](src/main/java/com/chrionline/securite/ChallengeGenerator.java#L14-L19)
    *   **Signataire Client :** [securite/Signer.java](src/main/java/com/chrionline/securite/Signer.java#L19) ➔ Méthode [`signerChallenge(byte[] challenge, PrivateKey privateKey)`](src/main/java/com/chrionline/securite/Signer.java#L19-L23)
    *   **Vérificateur Serveur :** [securite/Verifier.java](src/main/java/com/chrionline/securite/Verifier.java#L19) ➔ Méthode [`verifierSignature(byte[] challenge, byte[] signature, PublicKey publicKey)`](src/main/java/com/chrionline/securite/Verifier.java#L19-L23)
2.  **Facteur 2 — TOTP Google Authenticator (RFC 6238) :**
    *   Vérification d'un code OTP à usage unique à 6 chiffres régénéré toutes les **30 secondes** (Hachage SHA-1).
    *   **Vérificateur TOTP :** [securite/TOTPService.java](src/main/java/com/chrionline/securite/TOTPService.java#L39) ➔ Méthode [`verifierCode(String secret, int code)`](src/main/java/com/chrionline/securite/TOTPService.java#L39-L55)
3.  **Facteur 3 — Le port de transport mTLS (`9445`) :**
    *   Exposé dans la **Partie 2.B**.

---

## 💳 PARTIE 4 : LE CHIFFREMENT CLIENT-SIDE (Paiements)

Pour valider à 100% les exigences de conformité bancaire (PCI-DSS), les données brutes de cartes de crédit ne doivent jamais circuler en clair sur le réseau ou être inscrites en BDD.

```
[Saisie de Carte] ➔ [Chiffrement AES-256-GCM local] ➔ [Tunnel TLS 1.3] ➔ [Déchiffrement temporaire RAM Serveur]
```

*   **Chiffrement symétrique AES-256-GCM :** Réalisé directement sur le poste de l'utilisateur. Le mot de passe de dérivation est partagé et l'IV (vecteur d'initialisation) est généré via un générateur de nombres aléatoires cryptographiques forts de **12 octets** avec un tag d'intégrité de **128 bits**.
*   **Module cryptographique client/serveur :** [securite/PaymentCrypto.java](src/main/java/com/chrionline/securite/PaymentCrypto.java#L15)
*   **Chiffrement client JavaFX :** [securite/PaymentCrypto.java](src/main/java/com/chrionline/securite/PaymentCrypto.java#L28) ➔ Méthode [`chiffrerCarte(String plainText)`](src/main/java/com/chrionline/securite/PaymentCrypto.java#L28-L45)
*   **Déchiffrement éphémère serveur :** [securite/PaymentCrypto.java](src/main/java/com/chrionline/securite/PaymentCrypto.java#L55) ➔ Méthode [`dechiffrerCarte(String cipherText)`](src/main/java/com/chrionline/securite/PaymentCrypto.java#L55-L72)
*   **Traitement de commande serveur :** [server/core/ClientHandler.java](src/main/java/com/chrionline/server/core/ClientHandler.java#L390) ➔ Méthode [`handleCommandeConfirmer()`](src/main/java/com/chrionline/server/core/ClientHandler.java#L390-L425)

---

## 🛡️ PARTIE 5 : LES MÉTRIQUES & DÉTECTIONS IDS/IPS (Parfeu Applicatif)

Le cœur défensif de ChriOnline surveille, journalise (IDS) et neutralise (IPS) de manière proactive toutes les attaques au niveau réseau TCP/UDP et applicatif.

### A. Détection Anti-SYN Flood & DoS (TCP)
*   **Détecteur Réseau TCP :** Compte et stocke en mémoire les connexions TCP par adresse IP dans une fenêtre glissante.
*   **Fichier :** [server/core/ConnectionSecurityManager.java](src/main/java/com/chrionline/server/core/ConnectionSecurityManager.java#L11)
*   **Métrique (Seuil) :** **`100` connexions maximum par minute** par adresse IP ([Ligne 14](src/main/java/com/chrionline/server/core/ConnectionSecurityManager.java#L14)).
*   **Action IPS (Blocage) :** Bannissement automatique de l'IP pendant **5 minutes** ([Ligne 16](src/main/java/com/chrionline/server/core/ConnectionSecurityManager.java#L16)). Toute tentative suivante de la même IP est immédiatement rejetée à la réception de la socket via un `socket.close()`.

### B. Détection Anti-Flood UDP (Notifications)
*   **Détecteur Réseau UDP :** Compte les datagrammes UDP reçus sur le port `9091` ([Ligne 42](src/main/java/com/chrionline/server/core/Server.java#L42)).
*   **Métrique (Seuil) :** **`20` paquets UDP maximum par seconde** par IP ([Ligne 389](src/main/java/com/chrionline/server/core/Server.java#L389)).
*   **Action IPS (Blocage) :** Droppage automatique des paquets UDP de cette IP pendant **10 secondes** ([Ligne 390](src/main/java/com/chrionline/server/core/Server.java#L390)).

### C. Protection Brute Force (Lockout Comptes)
*   **Détecteur d'échecs applicatif :** Incrémente `failed_attempts` en BDD lors d'échecs de mots de passe.
*   **Fichier :** [server/dao/UserDAO.java](src/main/java/com/chrionline/server/dao/UserDAO.java#L115) ➔ Méthodes [`enregistrerEchec()`](src/main/java/com/chrionline/server/dao/UserDAO.java#L115-L135) et [`bloquerCompte()`](src/main/java/com/chrionline/server/dao/UserDAO.java#L177-L195)
*   **Métriques de blocage :**
    *   **3 échecs :** Compte verrouillé pendant **1 minute**.
    *   **6 échecs :** Compte verrouillé pendant **15 minutes** + **IP bannie automatiquement**.
    *   **9 échecs :** Compte verrouillé pendant **1 heure**.
    *   **12 échecs et plus :** Compte verrouillé pendant **24 heures**.

### D. Détection d'IP Spoofing (IDS Réseau)
*   **Détecteur de masquage IP :** Compare l'adresse IP réseau réelle de la socket établie avec l'IP déclarée par le client.
*   **Fichier :** [server/security/SecurityInterceptor.java](src/main/java/com/chrionline/server/security/SecurityInterceptor.java#L15)
*   **Action IPS (Blocage) :** Si une IP externe publique prétend appartenir au réseau interne privé (ex: `127.0.0.1`, `10.x.x.x`, `192.168.x.x`), elle est déclarée hostile et **blacklistée de manière permanente (durée 10 ans)**.

---

## 📊 PARTIE 6 : LE MONITORING & DASHBOARD ADMIN ( UAE-ENSAT Exigences )

L'administrateur dispose d'une console graphique de supervision codée en JavaFX pour observer les événements en direct et appliquer des sanctions manuelles.

```
[Intrusion Détectée] ➔ [SecurityLogger] ➔ [Chiffrement Transit] ➔ [BDD MySQL] ➔ [SecurityDashboardView]
```

### A. Événements Journalisés de Sécurité
*   **Connexions réussies/échouées :** [server/security/SecurityLogger.java](src/main/java/com/chrionline/server/security/SecurityLogger.java#L80) ➔ Méthodes [`loginSucces()`](src/main/java/com/chrionline/server/security/SecurityLogger.java#L80-L95) et [`loginEchec()`](src/main/java/com/chrionline/server/security/SecurityLogger.java#L100-L115)
*   **Accès non autorisés (RBAC) :** [server/security/SecurityLogger.java](src/main/java/com/chrionline/server/security/SecurityLogger.java#L120) ➔ Méthode [`accesNonAutorise()`](src/main/java/com/chrionline/server/security/SecurityLogger.java#L120-L135)
*   **Logs d'audit sensibles chiffrés :** [server/security/SecurityAuditLogger.java](src/main/java/com/chrionline/server/security/SecurityAuditLogger.java#L12) ➔ Chiffre et écrit les actions critiques avec Vault Transit.

### B. Le Panel de Supervision JavaFX (Dashboard Administrateur)
*   **Tableau d'alertes dynamique :** Affiche une table réactive des logs de sécurité contenant l'horodatage, le type d'alerte, l'IP source, et le contexte.
*   **Le Panel de Supervision :** [admin/view/SecurityDashboardView.java](src/main/java/com/chrionline/admin/view/SecurityDashboardView.java#L27)
*   **Blocage manuel d'adresse IP :** Un bouton permet à l'admin de sélectionner une IP et d'envoyer la commande d'exclusion réseau `ADMIN_BLOCK_IP` au serveur.
*   **Méthode de bannissement BDD :** [server/dao/SecurityBlacklistDAO.java](src/main/java/com/chrionline/server/dao/SecurityBlacklistDAO.java#L23) ➔ Méthode [`blacklistIp(String ip, String email, String raison)`](src/main/java/com/chrionline/server/dao/SecurityBlacklistDAO.java#L23-L45)

---

## 📈 PARTIE 7 : TABLEAU DE SYNTHÈSE DES MÉTRIQUES SYSTÈME

Ce tableau regroupe l'intégralité des variables, des limites physiques et des timeouts à retenir par cœur pour les questions du jury :

| Composant de Sécurité | Variable Système / Constante | Valeur Métrique | Fichier Référence (IDE Clickable) |
| :--- | :--- | :--- | :--- |
| **Pool de Connexions TCP** | `threadPool` | **50 Threads concurrents** | [server/core/Server.java](src/main/java/com/chrionline/server/core/Server.java#L61) |
| **Timeout d'inactivité standard** | `INACTIVITY_TIMEOUT_MS` | **15 minutes** | [server/session/Session.java](src/main/java/com/chrionline/server/session/Session.java#L12) |
| **Timeout Transactions Sensibles**| `dynamicTimeoutMs` | **5 minutes** | [server/core/ClientHandler.java](src/main/java/com/chrionline/server/core/ClientHandler.java#L189) |
| **Timeout Dashboard Admin** | `dynamicTimeoutMs` | **10 minutes** | [server/core/ClientHandler.java](src/main/java/com/chrionline/server/core/ClientHandler.java#L192) |
| **Durée de Session Absolue** | `ABSOLUTE_TIMEOUT_MS` | **24 heures** | [server/session/Session.java](src/main/java/com/chrionline/server/session/Session.java#L13) |
| **SYN Flood Protection (Seuil)** | `MAX_CONNEXIONS_PAR_MINUTE`| **100 requêtes / minute** | [server/core/ConnectionSecurityManager.java](src/main/java/com/chrionline/server/core/ConnectionSecurityManager.java#L14) |
| **Bannissement SYN Flood** | `BAN_DURATION_MS` | **5 minutes** | [server/core/ConnectionSecurityManager.java](src/main/java/com/chrionline/server/core/ConnectionSecurityManager.java#L16) |
| **UDP Flood Protection (Seuil)** | `MAX_UDP_PACKETS_PER_SECOND`| **20 paquets / seconde** | [server/core/Server.java](src/main/java/com/chrionline/server/core/Server.java#L389) |
| **Bannissement UDP Flood** | `UDP_BLOCK_DURATION_MS` | **10 secondes** | [server/core/Server.java](src/main/java/com/chrionline/server/core/Server.java#L390) |
| **Timeout Validation SMTP** | `socket.connect(..., 2500)`| **2,5 secondes** | [server/service/EmailService.java](src/main/java/com/chrionline/server/service/EmailService.java#L388) |
| **TTL Certificat SSL (PKI)** | `pki/issue/` | **72 heures** | [securite/VaultKeystoreService.java](src/main/java/com/chrionline/securite/VaultKeystoreService.java#L70) |
| **Scheduler Rotation SSL** | `certRotationScheduler` | **Toutes les 12 heures** | [server/core/Server.java](src/main/java/com/chrionline/server/core/Server.java#L628) |
| **Durée de vie OTP Activation** | `email_tokens.expiration` | **24 heures** | [server/dao/TokenDAO.java](src/main/java/com/chrionline/server/dao/TokenDAO.java#L21) |
| **Durée de vie OTP Reset MDP** | `email_tokens.expiration` | **1 heure** | [server/dao/TokenDAO.java](src/main/java/com/chrionline/server/dao/TokenDAO.java#L21) |
| **Durée de vie OTP 2FA Achat** | `PaymentTwoFactorService` | **5 minutes** | [server/service/PaymentTwoFactorService.java](src/main/java/com/chrionline/server/service/PaymentTwoFactorService.java#L83) |
