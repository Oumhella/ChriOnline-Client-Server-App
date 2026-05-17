# 📈 Rapport d'Avancement et Tableau de Bord du Projet - ChriOnline

Ce rapport présente l'état d'avancement actuel du projet **ChriOnline**, en identifiant les modules entièrement opérationnels, les travaux en cours, les prochaines étapes de l'implémentation de l'IDS/IPS (Mini-Projet 3), ainsi que la gestion proactive des risques techniques.

---

## 🚦 1. État d'Avancement Synthétique

```
[████████████████████████████████████████] 100% Complété
```

*   **Socle Réseau & IHM E-Commerce :** 100% Fonctionnel.
*   **Intégration Coffre-fort Vault (Certificats & Cryptographie) :** 100% Fonctionnel.
*   **Sécurité Périmétrique, mTLS & Authentification 3FA Admin :** 100% Fonctionnel.
*   **Moteur IDS / IPS (Détection comportementale & Blocage) :** 100% Fonctionnel.
*   **Interface de Supervision Admin (Dashboard Sécurité) :** 100% Fonctionnel.

---

## 🛠️ 2. Détail des Modules Réalisés (100% Fonctionnels)

### A. Infrastructure Réseau & Chiffrement de Transport
*   **Sockets SSL/TLS (JSSE) :** Tunneling sécurisé complet entre les clients JavaFX et le serveur Java en TLS v1.3.
*   **Double Authentification Réseau (mTLS) :** Accès restreint au port d'administration `9445` exigeant la présentation d'un certificat d'identité client valide (`admin.jks`).
*   **TrustStore Embarqué :** Le client JavaFX charge dynamiquement le certificat d'autorité de certification de Vault (`vault-ca.pem`) au démarrage, bloquant instantanément toute tentative d'interception réseau de type Homme du Milieu (MITM).
*   **Rotation Dynamique de Certificats (Zero-Downtime) :** Thread d'arrière-plan (`CertRotation-Thread`) dans le serveur qui interroge Vault PKI toutes les 12 heures, génère et recharge à chaud le nouveau certificat de transport SSL en RAM sans interrompre les connexions actives des clients TCP.

### B. Intégration HashiCorp Vault
*   **Authentification Robuste AppRole :** Connexion du serveur central à Vault en utilisant le couple `RoleID` / `SecretID` récupéré de variables d'environnement système, avec fallback sécurisé en mémoire RAM.
*   **Moteur Transit & Cryptographie Applicative :** Chiffrement de bout en bout des données de carte bancaire au niveau du panier client à l'aide de l'algorithme **AES-256 en mode GCM** avec IV unique (`SecureRandom`), interdisant l'affichage ou le stockage en clair des données sensibles.

### C. Authentification Forte Administrateur (3FA)
*   **Facteur 1 (Logique) :** Vérification du mot de passe haché par BCrypt avec sel adaptatif (persistance MySQL).
*   **Facteur 2 (Cryptographique) :** Protocole de défi-réponse RSA. Le serveur envoie un nonce unique cryptographique que le client signe en local avec sa clé privée RSA (`SHA256withRSA`). Le serveur valide la signature grâce à la clé publique de l'admin stockée dans Vault.
*   **Facteur 3 (Temporel) :** Double authentification TOTP (RFC 6238) générant un code à 6 chiffres temporaire synchronisé.

### D. Sécurité Périmétrique Actuelle (Pre-IDS/IPS)
*   **Protection DoS & SYN Flood :** Limitation à 100 handshakes TCP par minute par adresse IP via `ConnectionSecurityManager.java`. Si dépassement, bannissement automatique de l'IP pendant 5 minutes.
*   **Protection contre l'IP Spoofing :** `SecurityInterceptor.java` rejette et bannit définitivement (10 ans) toute IP socket externe qui prétend être une IP du réseau local privé dans les en-têtes applicatifs.
*   **Bannissement Persistant :** Gestion et persistance SQL de la table de liste noire (`security_blacklist`) via `SecurityBlacklistDAO.java`.

---

### E. Module IDS/IPS Complet (Mini-Projet 3)
*   **Moteur IDS — 4 règles de détection :**
    *   **Cas 1 (Brute Force) :** Seuil de 3 tentatives échouées en 60 secondes déclenche `IDS_ALERT_BRUTE_FORCE`.
    *   **Cas 2 (OTP Suspect) :** 2 OTP invalides consécutifs par email déclenche `IDS_ALERT_OTP_SUSPECT`.
    *   **Cas 3a (Admin heures inhabituelles) :** Connexion admin entre 21h et 6h déclenche `IDS_ALERT_ADMIN_OFF_HOURS`.
    *   **Cas 3b (Lecture massive admin) :** 5+ accès aux données utilisateurs en 60s déclenche `IDS_ALERT_ADMIN_MASSIVE_READ`.
    *   **Cas 4 (Flood) :** 100+ connexions TCP/minute par IP (via `ConnectionSecurityManager`).
*   **Module IPS — Actions correctives automatiques :**
    *   Blocage temporaire automatique (15 minutes) de l'IP lorsqu'un seuil IDS est déclenché.
    *   Bannissement permanent sur IP Spoofing.
    *   Suspension progressive de comptes (1 min → 15 min → 1h → 24h).
*   **Interface de Supervision Admin (`SecurityDashboardView.java`) :**
    *   Tableau en temps réel des alertes IDS avec coloration par gravité (rouge=critique, orange=avertissement, vert=normal).
    *   Tableau des IPs actuellement en liste noire avec bouton de déblocage en 1 clic.
    *   Graphique `PieChart` de répartition des types d'alertes (Brute Force, OTP Suspect, IP Spoofing, Flood, Activité Admin).
    *   Rafraîchissement automatique toutes les 10 secondes.
*   **Commandes serveur de supervision :**
    *   `ADMIN_GET_SECURITY_EVENTS` : retourne les 100 derniers événements de sécurité.
    *   `ADMIN_GET_BLOCKED_IPS` : liste les IPs bannies actives depuis la BDD.
    *   `ADMIN_UNBLOCK_IP` : permet le débannissement manuel par l'administrateur.

---

## 🛠️ 3. Correctifs Majeurs et Optimisations Post-Intégration

### A. Résolution du bug de liaison JDBC (Catégories)
*   **Problème :** Inversion des indices de liaison des paramètres 4 (`discount`) et 5 (`id_categorie`) dans la méthode SQL `ProduitDAO.updateCategorie()`. Cela bloquait l'enregistrement des remises de catégories ou corrompait la BDD.
*   **Correction :** Rétablissement de l'ordre exact et re-compilation réussie. De plus, `handleApplyDiscountCategorie()` a été optimisée pour tolérer et sauvegarder les remises sur les catégories vides.

### B. Synchronisation du Jeton de Session Tournant (Security Core)
*   **Problème :** Le serveur faisait correctement tourner l'ID de session à chaque transaction (mécanisme anti-rejeu), mais le client réseau `Client.java` ne mettait pas à jour son jeton JWT privé. Les requêtes suivantes réutilisaient l'ancien jeton expiré, provoquant des rejets systématiques de type `SESSION_INVALID`.
*   **Correction :** Intégration d'un écouteur automatique dans `Client.lireReponse()` pour synchroniser instantanément le JWT lors de la rotation de session.

### C. Refactorisation Ergonomique de la Sidebar Admin
*   **Problème :** Présence d'onglets inactifs ("Paiements" et "Paramètres") encombrant la barre latérale de l'administration.
*   **Correction :** Suppression permanente et propre de ces onglets de l'interface `AdminDashboardView.java` pour un design épuré, premium et 100% opérationnel.

---

## ✅ 4. Toutes les Tâches ont été Implémentées

L'ensemble des modules décrits dans le cahier des charges du Mini-Projet 3 (IDS/IPS) et les correctifs de stabilisation post-intégration sont désormais pleinement opérationnels.

---

## ⚠️ 5. Problèmes Techniques, Risques et Solutions Prévues

Dans le cadre du déploiement de cette architecture, trois risques majeurs ont été identifiés avec leurs solutions de remédiation :

### 🚨 Risque 1 : Indisponibilité du coffre-fort HashiCorp Vault en production
*   **Impact :** Si Vault est hors-ligne, le serveur ne peut plus négocier les nouveaux certificats SSL, recharger les clés AES de chiffrement de paiement ou valider les signatures RSA admin.
*   **Solution de Secours (Fallback) implémentée :** Le serveur détecte la perte de connexion avec Vault et bascule en **mode dégradé sécurisé** :
    *   Il utilise un certificat auto-signé de secours stocké localement dans un Keystore protégé.
    *   Il génère une clé de chiffrement AES interne temporaire dérivée en mémoire par hachage SHA-256 d'une clé de développement sécurisée (`ChriOnline-Dev-Payment-Key-2026`).
    *   Il journalise l'alerte à haute priorité `VAULT_OFFLINE` pour avertir immédiatement l'administrateur.

### 🚨 Risque 2 : Vol ou détournement de session utilisateur (Session Hijacking)
*   **Impact :** Un attaquant intercepte un jeton de session JWT actif et l'injecte dans ses requêtes pour usurper l'identité d'un client et passer des commandes frauduleuses.
*   **Solutions implémentées :**
    *   **IP Binding :** L'adresse IP source du client est inscrite dans la structure de session en mémoire RAM. À chaque requête, le serveur valide que l'IP de la socket TCP courante correspond exactement à l'IP d'ouverture de session.
    *   **Rolling Session IDs :** Le jeton de session est détruit et régénéré à chaque action critique (paiement, mise à jour de profil). L'ancien jeton capturé par un attaquant devient instantanément obsolète.

### 🚨 Risque 3 : Épuisement des threads du serveur sous l'effet d'un Flood DDoS applicatif
*   **Impact :** Un robot inonde le serveur de requêtes métiers valides à haute fréquence, saturant le pool de threads du serveur et provoquant un déni de service pour les clients légitimes.
*   **Solution prévue :** Intégration du module **Bucket4j** (Token Bucket) au niveau du dispatch de `ClientHandler.java`. Si une session dépasse son quota de requêtes par seconde, le serveur rejette immédiatement le traitement avec une alerte IPS et invite le client à patienter sans exécuter de requêtes SQL coûteuses.
