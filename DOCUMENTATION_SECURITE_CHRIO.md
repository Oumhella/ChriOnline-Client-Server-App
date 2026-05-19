# 🔐 CARTE & RÉPERTOIRE INTERACTIF DE SÉCURITÉ — CHRIOONLINE
> **UAE-ENSAT 2026 — Soutenance Projet de Sécurité Applicative**
>
> Ce document sert de cartographie de sécurité interactive pour le projet **ChriOnline**. Utilisez le raccourci `Ctrl+F` pour rechercher un concept ou un composant, puis **cliquez directement sur les liens** pour ouvrir le code source et la méthode correspondante dans votre IDE.

---

## 🔑 1. Infrastructure Globale & Secrets (VAULT)

L'intégralité des clés de sécurité, certificats SSL et opérations cryptographiques de ChriOnline est gérée de façon centralisée par **HashiCorp Vault**.

*   **1.1 Initialisation de l'environnement de production :** Déploiement du conteneur sécurisé `vault-prod` en mode déverrouillé (Unsealed).
    *   **Script :** [vault/setup.sh](vault/setup.sh)
*   **1.2 Définition des règles d'accès (Policy) :** Rédaction et chargement de la politique restrictive `chrionline-policy` (principe du moindre privilège).
*   **1.3 Authentification M2M (AppRole) :** Configuration des identifiants machines (`RoleID` & `SecretID`) pour éliminer l'usage de tokens permanents.
    *   **Fichier d'initialisation :** [securite/VaultServerService.java](src/main/java/com/chrionline/securite/VaultServerService.java#L155) ➔ Méthode [`initWithAppRole()`](src/main/java/com/chrionline/securite/VaultServerService.java#L155-L187)
    *   **Récupération du token d'AppRole :** [securite/VaultServerService.java](src/main/java/com/chrionline/securite/VaultServerService.java#L189) ➔ Méthode [`initWithToken()`](src/main/java/com/chrionline/securite/VaultServerService.java#L189-L230)
*   **1.4 Activation du moteur KV (Key-Value) :** Configuration du stockage sécurisé et dynamique pour les clés de production (`payment_aes_key` et `udp_hmac_key`).
    *   **Lecture KV v2 Java :** [securite/VaultServerService.java](src/main/java/com/chrionline/securite/VaultServerService.java#L266) ➔ Méthode [`getAdminPublicKey(String email)`](src/main/java/com/chrionline/securite/VaultServerService.java#L266-L281)

---

## 🔏 2. Identité & Confiance (KEYSTORE / TRUSTSTORE)

*   **2.1 Instanciation du KeyStore serveur en RAM :** Chargement direct en mémoire vive (sans fichier sur disque) des clés privées et certificats générés par Vault.
    *   **Génération dynamique Java :** [securite/VaultKeystoreService.java](src/main/java/com/chrionline/securite/VaultKeystoreService.java#L58) ➔ Méthode [`genererKeystoreDynamique()`](src/main/java/com/chrionline/securite/VaultKeystoreService.java#L58-L135)
*   **2.2 Création du KeyStore Admin local (`admin.jks`) :** Génération des clés cryptographiques RSA 2048-bits pour l'identité de l'administrateur.
*   **2.3 Configuration du TrustStore Client :** Intégration stricte de `vault-ca.pem` pour valider l'authenticité du serveur (protection réseau).
    *   **Côté Client :** [client/network/Client.java](src/main/java/com/chrionline/client/network/Client.java#L106) ➔ Initialisation SSL context dans [`connecter()`](src/main/java/com/chrionline/client/network/Client.java#L106-L150)
*   **2.4 Configuration du TrustStore mTLS Serveur :** Paramétrage du serveur pour faire confiance au certificat auto-signé de l'admin.

---

## 🚫 3. Sécurité Réseau & Canaux (VALIDER CERTIFICAT VIA VAULT)

*   **3.1 Éradication du "Trust All" :** Suppression définitive de `trustAllCerts` et blocage des connexions si la chaîne de certification est invalide (Anti-MITM).
    *   **Côté Serveur :** [server/core/Server.java](src/main/java/com/chrionline/server/core/Server.java#L71) ➔ Initialisation de la socket TLS standard dans [`demarrer()`](src/main/java/com/chrionline/server/core/Server.java#L71-L179)
*   **3.2 Isolation du port d'administration (9445) :** Configuration du port mTLS pour exiger systématiquement un certificat client valide (`setNeedClientAuth`).
    *   **Fichier Serveur :** [server/core/Server.java](src/main/java/com/chrionline/server/core/Server.java#L540) ➔ Socket mTLS dédiée d'administration dans [`demarrerAdminMTLS()`](src/main/java/com/chrionline/server/core/Server.java#L540-L620)
*   **3.3 Implémentation du planificateur (Scheduler) :** Ajout d'un thread d'arrière-plan (`ScheduledExecutorService`) qui vérifie l'expiration toutes les 12h.
    *   **Planificateur de rotation Java :** [server/core/Server.java](src/main/java/com/chrionline/server/core/Server.java#L628) ➔ Méthode [`demarrerRotationCertificats()`](src/main/java/com/chrionline/server/core/Server.java#L628-L659)
*   **3.4 Renouvellement à chaud (Hot Reload) :** Rechargement dynamique du contexte SSL en mémoire en cas de nouveau certificat, sans couper le serveur.

---

## 💳 4. Protection des Données (CHIFFREMENT PAIEMENT & CANAUX)

*   **4.1 Chiffrement symétrique/asymétrique client (RSA/AES) :** Chiffrement du numéro de carte bancaire avant son départ sur le réseau. L'algorithme utilisé est `RSA/ECB/OAEPWithSHA-256AndMGF1Padding`.
    *   **Chiffrement client JavaFX :** [securite/PaymentCrypto.java](src/main/java/com/chrionline/securite/PaymentCrypto.java#L92) ➔ Méthodes `encrypt(String)` et `encrypt(char[])`
*   **4.2 Lecture sécurisée des clés (Vault KV) :** Récupération dynamique de la clé de décryptage au moment exact du traitement de la transaction.
*   **4.3 Déchiffrement sécurisé en mémoire :** Traitement et décryptage en RAM par le ClientHandler sans jamais laisser la clé ou la carte en clair sur le disque.
    *   **Déchiffrement éphémère serveur :** [securite/PaymentCrypto.java](src/main/java/com/chrionline/securite/PaymentCrypto.java#L147) ➔ Méthode `decrypt(String)`
    *   **Traitement de commande serveur :** [server/core/ClientHandler.java](src/main/java/com/chrionline/server/core/ClientHandler.java#L793) ➔ Méthode [`handleCommandeConfirmer()`](src/main/java/com/chrionline/server/core/ClientHandler.java#L793-L840)
*   **4.4 Signature d'intégrité UDP (HMAC-SHA256) :** Signature systématique de tous les paquets UDP pour empêcher le spoofing et l'injection de fausses alertes.

---

## 👤 5. Authentification Forte (TOTP & CHALLENGE-RESPONSE)

*   **5.1 Générateur de challenge cryptographique :** Génération d'une chaîne aléatoire unique côté serveur pour chaque tentative d'authentification admin.
    *   **Générateur de défi :** [securite/ChallengeGenerator.java](src/main/java/com/chrionline/securite/ChallengeGenerator.java#L7) ➔ Méthode [`generateChallenge()`](src/main/java/com/chrionline/securite/ChallengeGenerator.java#L7-L11)
*   **5.2 Signature asymétrique RSA :** Signature du challenge par la clé privée de l'admin et vérification côté serveur avec sa clé publique.
    *   **Signataire Client :** [securite/Signer.java](src/main/java/com/chrionline/securite/Signer.java#L7) ➔ Méthode [`sign(String challenge, PrivateKey privateKey)`](src/main/java/com/chrionline/securite/Signer.java#L7-L12)
    *   **Vérificateur Serveur :** [securite/Verifier.java](src/main/java/com/chrionline/securite/Verifier.java#L7) ➔ Méthode [`verify(String challenge, byte[] signatureBytes, PublicKey publicKey)`](src/main/java/com/chrionline/securite/Verifier.java#L7-L14)
*   **5.3 Validation Double Facteur (TOTP 2FA) :** Validation obligatoire par code temporaire à 6 chiffres pour confirmer les paiements et connexions critiques.
    *   **Vérificateur Java :** [server/dao/UserDAO.java](src/main/java/com/chrionline/server/dao/UserDAO.java#L100) ➔ Méthode [`connexion(Map<String, Object> data)`](src/main/java/com/chrionline/server/dao/UserDAO.java#L100-L225)
    *   **Vérificateur TOTP :** [securite/TOTPService.java](src/main/java/com/chrionline/securite/TOTPService.java#L64) ➔ Méthode [`verifyCode(String secret, String code)`](src/main/java/com/chrionline/securite/TOTPService.java#L64-L81)
*   **5.4 UX d'enrôlement par QR Code :** Génération de l'URI d'authentification standardisée et affichage du QR Code pour scan sur smartphone.

---

## 📝 6. Traçabilité & Audit (LOGGING)

*   **6.1 Isolation des journaux d'audit :** Création de la classe `SecurityAuditLogger` dédiée uniquement aux événements de sécurité (Log4j2).
    *   **Journalisation centralisée :** [server/security/SecurityAuditLogger.java](src/main/java/com/chrionline/server/security/SecurityAuditLogger.java#L22)
*   **6.2 Tracing des accès TLS/mTLS :** Enregistrement systématique des connexions réussies et des échecs (certificats rejetés, IP clientes).
*   **6.3 Journalisation des événements clés :** Suivi des rotations de certificats, renouvellements Vault et tentatives de signatures UDP invalides.
    *   **Moteur Transit — Chiffrement d'Enveloppe (AES-256-GCM) des logs :** [securite/VaultServerService.java](src/main/java/com/chrionline/securite/VaultServerService.java#L398) ➔ Méthode [`transitEncrypt(String plaintext)`](src/main/java/com/chrionline/securite/VaultServerService.java#L398-L419)
*   **6.4 Persistance et rotation sécurisée :** Configuration de la rotation automatique des fichiers de logs pour éviter la saturation et garantir l'intégrité des traces.

---

## 📊 7. Journalisation d'Audit & Collecte

*   **7.1 Structure standardisée des logs :** Enregistrement d'une trace d'audit complète contenant la date/heure, l'utilisateur, l'adresse IP, le type d'action et le statut.
*   **7.2 Traçabilité des accès initiaux :** Enregistrement systématique des tentatives de connexion et des échecs d'authentification.
    *   **Connexions réussies/échouées :** [server/security/SecurityLogger.java](src/main/java/com/chrionline/server/security/SecurityLogger.java#L140) ➔ Méthodes `loginSucces()` et `loginEchec()`
*   **7.3 Surveillance des privilèges & données :** Journalisation des accès administrateurs, des changements de mots de passe et des consultations de données sensibles.
    *   **Accès non autorisés (RBAC) :** [server/security/SecurityLogger.java](src/main/java/com/chrionline/server/security/SecurityLogger.java#L177) ➔ Méthode `accesNonAutorise()`
*   **7.4 Télémétrie de trafic :** Implémentation d'un compteur en temps réel du nombre total de requêtes envoyées par chaque client.

---

## 🔍 8. Module IDS : Détection d'Intrusions

*   **8.1 Détection de Force Brute (Connexion) :** Alerte automatique si plus de 3 échecs de connexion surviennent en moins d'une minute.
    *   **Détecteur Réseau TCP :** [server/core/ConnectionSecurityManager.java](src/main/java/com/chrionline/server/core/ConnectionSecurityManager.java#L11)
*   **8.2 Analyse d'activité admin anormale :** Détection des connexions administrateurs à des heures inhabituelles ou de requêtes massives de données utilisateurs.
*   **8.3 Détection de Flood de requêtes :** Identification immédiate des volumes anormaux et trop rapides de requêtes envoyées par une même adresse IP.
    *   **Métrique TCP :** **100 requêtes / minute** ([Ligne 14](src/main/java/com/chrionline/server/core/ConnectionSecurityManager.java#L14)).
    *   **Métrique UDP :** **20 paquets / seconde** ([Ligne 389](src/main/java/com/chrionline/server/core/Server.java#L389)).
*   **8.4 Marquage de comportement :** Changement automatique du statut du client en "Suspect" en mémoire et notification immédiate du serveur principal.

---

## 🛡️ 9. Module IPS : Prévention & Blocage Actif

*   **9.1 Blocage temporaire d'IP :** Bannissement automatique au niveau du socket réseau de toute adresse IP à l'origine d'un comportement d'attaque.
    *   **Bannissement TCP :** Automatique pendant **5 minutes** ([Ligne 16](src/main/java/com/chrionline/server/core/ConnectionSecurityManager.java#L16)).
    *   **Méthode de bannissement BDD :** [server/dao/SecurityBlacklistDAO.java](src/main/java/com/chrionline/server/dao/SecurityBlacklistDAO.java#L23) ➔ Méthode [`blacklistIp(String ip, String email, String raison)`](src/main/java/com/chrionline/server/dao/SecurityBlacklistDAO.java#L23-L45)
*   **9.2 Suspension temporaire de compte :** Verrouillage automatique du compte utilisateur attaqué pendant plusieurs minutes pour stopper la brute force.
    *   **Mécanisme de Lockout :** [server/dao/UserDAO.java](src/main/java/com/chrionline/server/dao/UserDAO.java#L330) ➔ Méthodes [`incrementerTentatives()`](src/main/java/com/chrionline/server/dao/UserDAO.java#L330-L337) et [`bloquerCompte()`](src/main/java/com/chrionline/server/dao/UserDAO.java#L339-L348)
*   **9.3 Défis de sécurité forcés :** Déclenchement automatique d'un renouvellement d'OTP obligatoire ou limitation drastique du débit de requêtes autorisées.
*   **9.4 Révocation de session :** Fermeture immédiate et invalidation de la session active d'un utilisateur marqué comme hostile.
    *   **Détection d'IP Spoofing (IDS Réseau) :** [server/security/SecurityInterceptor.java](src/main/java/com/chrionline/server/security/SecurityInterceptor.java#L15)
