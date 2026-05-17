# Documentation Complète de la Sécurité : ChriOnline

Cette documentation détaille l'architecture de sécurité implémentée dans l'application ChriOnline-Client-Server-App, couvrant les aspects réseau, authentification, gestion des secrets et audit.

---

## 1. Architecture Globale (A à Z)

La sécurité de ChriOnline repose sur une approche de **Défense en Profondeur**, où chaque couche protège les données même si une autre est compromise.

| Couche | Technologie | Description |
| :--- | :--- | :--- |
| **Transport** | TLS 1.3 + Vault PKI | Chiffrement de bout en bout avec certificats éphémères (72h). |
| **Accès Réseau** | mTLS (Mutual TLS) | Port admin dédié exigeant un certificat client pour la connexion. |
| **Secrets** | HashiCorp Vault | Gestion centralisée via AppRole. Aucun mot de passe en dur. |
| **Authentification** | RSA Challenge-Response | Authentification forte sans mot de passe pour les administrateurs. |
| **MFA** | TOTP (RFC 6238) | Double authentification via application mobile (Google Authenticator). |
| **Sessions** | JWT (JSON Web Tokens) | Gestion de session sans état (stateless) et sécurisée. |
| **Données** | AES-256-GCM | Chiffrement applicatif des données sensibles (Paiements). |
| **Audit** | Log4j2 Security Audit | Journalisation dédiée de tous les événements critiques. |

---

## 2. Analyse Détaillée des Classes de Sécurité

### A. Gestion des Secrets : `VaultServerService.java`
Cette classe est le cœur de l'infrastructure de sécurité. Elle communique avec **HashiCorp Vault** via le protocole **AppRole**.

- **AppRole** : Utilise un `RoleID` et `SecretID` (variables d'environnement) pour obtenir un token temporaire. C'est beaucoup plus sûr qu'un token statique.
- **Moteurs gérés** :
    - `KV` : Stocke les clés publiques RSA des administrateurs.
    - `PKI` : Génère dynamiquement des certificats SSL/TLS.
    - `Transit` : Offre un service de chiffrement/déchiffrement "à la volée".

```java
// Extrait de l'authentification AppRole dans VaultServerService
private static void initWithAppRole(String roleId, String secretId) {
    AuthResponse authResponse = loginVault.auth().loginByAppRole(roleId, secretId);
    String appToken = authResponse.getAuthClientToken();
    initWithToken(appToken);
}
```

### B. Chiffrement des Paiements : `PaymentCrypto.java`
Utilise l'algorithme **AES-256-GCM** (Galois/Counter Mode) pour garantir à la fois la **confidentialité** et l'**intégrité** des données de carte bancaire.

- **Défense en profondeur** : Même si le flux SSL est décrypté par un attaquant, les numéros de carte restent illisibles.
- **Gestion des clés** : La clé de chiffrement maîtresse est récupérée dynamiquement dans Vault au démarrage.

```java
// Chiffrement GCM (IV + Ciphertext + Tag) dans PaymentCrypto
public static String encrypt(String plaintext) {
    byte[] iv = new byte[12]; // IV aléatoire unique
    new SecureRandom().nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
    byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
    // ... concaténation et encodage Base64
}
```

### C. Double Authentification : `TOTPService.java`
Implémente la norme **RFC 6238** pour les codes à usage unique basés sur le temps.

- **Indépendance** : Développé en Java pur sans bibliothèque tierce pour limiter la surface d'attaque.
- **Sécurité** : Utilise HMAC-SHA1 pour la génération du code à 6 chiffres.

### D. Audit et Monitoring : `SecurityAuditLogger.java` & `SecurityLogger.java`
- `SecurityAuditLogger` : Écrit dans un fichier de log dédié (`security_audit.log`). Trace les échecs TLS, les tentatives d'IP Spoofing et les accès refusés.
- `SecurityLogger` : Gère le **bannissement automatique (Auto-Ban)**. Si une IP échoue 5 fois en moins de 60 secondes, elle est bloquée.

```java
// Détection de brute-force dans SecurityLogger
private static void checkThreshold(String ip, String type) {
    timestamps.removeIf(t -> now - t > 60000); // Fenêtre de 60s
    if (timestamps.size() >= 5) {
        blockIP(ip, "Brute-force détecté");
    }
}
```

---

## 3. Scénarios d'Utilisation et Implémentation

### Scénario 1 : Connexion d'un Administrateur (Accès Critique)
**Objectif** : Garantir que seul un admin autorisé avec un appareil de confiance peut accéder au panneau de gestion.

1.  **Couche Transport** : L'admin se connecte sur le port **9445** (mTLS). Le serveur exige un certificat client (`admin.jks`). Si absent, la connexion est coupée avant même de voir la page de login.
2.  **Challenge RSA** : Le serveur envoie un nombre aléatoire (challenge). L'admin le signe avec sa clé privée RSA locale. Le serveur vérifie la signature avec la clé publique stockée dans **Vault KV**.
3.  **MFA** : Si la signature est valide, l'admin doit saisir un code **TOTP** depuis son téléphone.
4.  **Audit** : L'événement `MTLS_AUTH_OK` est enregistré dans `SecurityAuditLogger`.

### Scénario 2 : Processus de Paiement
**Objectif** : Empêcher la fuite de données bancaires même en cas de compromission de la base de données.

1.  **Côté Client** : Le client saisit ses coordonnées. Elles sont envoyées via le tunnel **TLS** chiffré par le CA de Vault.
2.  **Côté Serveur** : Avant d'enregistrer en BDD, la classe `PaymentCrypto` chiffre le numéro de carte avec une clé AES-256 issue de Vault.
3.  **Stockage** : Seul le texte chiffré (Base64) est stocké.
4.  **Accès** : Seule une requête autorisée peut déclencher `PaymentCrypto.decrypt()`, et chaque déchiffrement est loggé (`PAYMENT_DECRYPT`).

### Scénario 3 : Tentative d'Attaque (IP Spoofing / Flood)
**Objectif** : Protéger le serveur contre les attaques de déni de service (DoS) et l'usurpation d'identité réseau.

1.  **Interception** : `SecurityInterceptor` vérifie chaque paquet TCP/UDP entrant.
2.  **Détection Spoofing** : Si une requête prétend venir de `127.0.0.1` (IP interne) mais provient réellement d'une IP publique, elle est bloquée immédiatement.
3.  **Bannissement** : L'IP est ajoutée à la table `security_blacklist` en base de données.
4.  **Protection UDP** : Chaque notification UDP envoyée au client est signée par un **HMAC-SHA256**. Le client rejette toute notification non signée ou mal signée.

---

## 4. Points Forts de l'Implémentation

1.  **Zéro Secret en Dur** : Tout est dans Vault (Clés SSL, Mots de passe Keystore, Clés AES).
2.  **Rotation Dynamique** : Les certificats SSL se renouvellent tous les 3 jours sans redémarrer le serveur (`Server.refreshSSLContext`).
3.  **Résilience** : En cas de panne de Vault, le système bascule sur un mode "dégradé" sécurisé avec des clés de secours (`Fallback Mode`).
4.  **mTLS** : Une barrière physique (certificat client) protège les fonctions les plus sensibles.
