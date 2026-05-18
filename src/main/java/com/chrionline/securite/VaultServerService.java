package com.chrionline.securite;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Service centralisé pour le serveur ChriOnline utilisant HashiCorp Vault.
 * 
 * Authentification : AppRole (RoleID + SecretID) — Plus sécurisé qu'un token
 * statique.
 * 
 * Moteurs gérés :
 * - KV v2 (secret/) — Stockage des clés publiques RSA des admins
 * - PKI (pki/) — Certificats SSL dynamiques
 * - Transit (transit/) — Chiffrement/déchiffrement des données sensibles
 */
public class VaultServerService {

    public static final String VAULT_ADDR = "http://127.0.0.1:8200";
    private static final String KV_KEYS_PATH = "admin/keys/";
    private static final String KV_CONFIG_PATH = "server/config";
    private static final String PKI_ROLE = "chrionline-server";
    private static final String TRANSIT_KEY = "chrionline-data";

    private static Vault vaultKV;
    private static Vault vaultPKI;
    private static Vault vaultTransit;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static boolean isVaultAvailable = false;

    // Stockage mémoire de secours (si Vault est indisponible)
    private static final Map<String, String> publicKeysMemory = new HashMap<>();

    // ─── INITIALISATION AVEC APPROLE ────────────────────────────────────────────

    static {
        try {
            // ── Auto-Unseal : déverrouiller Vault s'il est scellé ──
            autoUnsealIfNeeded();

            String roleId = System.getenv("VAULT_ROLE_ID");
            String secretId = System.getenv("VAULT_SECRET_ID");

            // Fallback : support de l'ancien VAULT_TOKEN pour compatibilité
            String token = System.getenv("VAULT_TOKEN");

            if (roleId != null && !roleId.trim().isEmpty()
                    && secretId != null && !secretId.trim().isEmpty()) {
                // ── Mode AppRole (Recommandé) ──
                initWithAppRole(roleId, secretId);
            } else if (token != null && !token.trim().isEmpty()) {
                // ── Mode Token Legacy ──
                System.out.println("[VaultServerService] Authentification par Token (Legacy).");
                initWithToken(token);
            } else {
                System.err.println("[VaultServerService] WARNING: Aucune authentification Vault configurée.");
                System.err.println("[VaultServerService] Configurez VAULT_ROLE_ID + VAULT_SECRET_ID ou VAULT_TOKEN.");
                System.err.println("[VaultServerService] Mode dégradé activé (sans Vault).");
            }
        } catch (Exception e) {
            System.err.println("[VaultServerService] Erreur d'initialisation : " + e.getMessage());
        }
    }

    /**
     * Vérifie si Vault est scellé (sealed) et tente de le déverrouiller
     * automatiquement en lisant la clé depuis vault/data/unseal_key.txt.
     */
    private static void autoUnsealIfNeeded() {
        try {
            // 1. Vérifier le statut de Vault via l'API /sys/health
            URL url = new URL(VAULT_ADDR + "/v1/sys/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int httpCode = conn.getResponseCode();

            // Code 503 = Vault est scellé (sealed)
            if (httpCode == 503) {
                System.out.println("[VaultServerService] Vault est scellé — tentative d'auto-unseal...");

                // 2. Chercher le fichier unseal_key.txt dans vault/data/
                Path unsealKeyPath = Paths.get("vault", "data", "unseal_key.txt");
                if (!Files.exists(unsealKeyPath)) {
                    System.err.println(
                            "[VaultServerService] Fichier unseal_key.txt introuvable. Impossible d'auto-unseal.");
                    return;
                }

                String unsealKey = Files.readString(unsealKeyPath).trim();
                if (unsealKey.isEmpty()) {
                    System.err.println("[VaultServerService] Clé unseal vide.");
                    return;
                }

                // 3. Envoyer la clé de déverrouillage via PUT /sys/unseal
                URL unsealUrl = new URL(VAULT_ADDR + "/v1/sys/unseal");
                HttpURLConnection unsealConn = (HttpURLConnection) unsealUrl.openConnection();
                unsealConn.setRequestMethod("PUT");
                unsealConn.setRequestProperty("Content-Type", "application/json");
                unsealConn.setDoOutput(true);

                String jsonBody = "{\"key\":\"" + unsealKey + "\"}";
                unsealConn.getOutputStream().write(jsonBody.getBytes());

                int unsealStatus = unsealConn.getResponseCode();
                if (unsealStatus == 200) {
                    // Lire la réponse pour vérifier si c'est déverrouillé
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(unsealConn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    if (response.toString().contains("\"sealed\":false")) {
                        System.out.println("[VaultServerService] ✓ Vault déverrouillé automatiquement avec succès !");
                    } else {
                        System.out.println(
                                "[VaultServerService] Unseal envoyé, mais Vault nécessite peut-être d'autres clés.");
                    }
                } else {
                    System.err.println("[VaultServerService] Échec auto-unseal (HTTP " + unsealStatus + ")");
                }
            } else if (httpCode == 200 || httpCode == 429) {
                System.out.println("[VaultServerService] Vault est déjà déverrouillé et opérationnel.");
            }
            // Si Vault n'est pas accessible du tout, on ignore silencieusement
        } catch (java.net.ConnectException e) {
            System.out.println("[VaultServerService] Vault non accessible à " + VAULT_ADDR + " — mode dégradé.");
        } catch (Exception e) {
            System.err
                    .println("[VaultServerService] Erreur lors de la vérification du statut Vault : " + e.getMessage());
        }
    }

    /**
     * Authentification via AppRole : envoie RoleID + SecretID à Vault
     * et récupère un token temporaire avec des droits limités.
     */
    private static void initWithAppRole(String roleId, String secretId) {
        try {
            System.out.println("[VaultServerService] Authentification AppRole en cours...");

            // 1. Configuration temporaire sans token pour l'authentification
            VaultConfig loginConfig = new VaultConfig()
                    .address(VAULT_ADDR)
                    .build();
            Vault loginVault = new Vault(loginConfig);

            // 2. Authentification AppRole → obtention d'un token temporaire
            AuthResponse authResponse = loginVault.auth().loginByAppRole(roleId, secretId);
            String appToken = authResponse.getAuthClientToken();

            System.out.println("[VaultServerService] AppRole : Token temporaire obtenu (TTL: "
                    + authResponse.getAuthLeaseDuration() + "s)");

            // 3. Initialiser les instances Vault avec le token obtenu
            initWithToken(appToken);

            if (isVaultAvailable) {
                System.out.println("[VaultServerService] ✓ Authentification AppRole réussie.");
            }
        } catch (VaultException e) {
            System.err.println("[VaultServerService] Échec AppRole : " + e.getMessage());
            System.err.println("[VaultServerService] Vérifiez VAULT_ROLE_ID et VAULT_SECRET_ID.");
            System.err.println("[VaultServerService] Mode dégradé activé.");
        }
    }

    /**
     * Initialise les instances Vault (KV, PKI, Transit) avec un token donné.
     */
    private static void initWithToken(String token) {
        try {
            // Instance KV (v2) — Stockage des clés publiques
            VaultConfig configKV = new VaultConfig()
                    .address(VAULT_ADDR)
                    .token(token)
                    .engineVersion(2)
                    .build();
            vaultKV = new Vault(configKV);

            // Instance PKI (v1) — Certificats SSL
            VaultConfig configPKI = new VaultConfig()
                    .address(VAULT_ADDR)
                    .token(token)
                    .engineVersion(1)
                    .build();
            vaultPKI = new Vault(configPKI);

            // Instance Transit (v1) — Chiffrement des données
            VaultConfig configTransit = new VaultConfig()
                    .address(VAULT_ADDR)
                    .token(token)
                    .engineVersion(1)
                    .build();
            vaultTransit = new Vault(configTransit);

            // Test de connexion
            try {
                vaultKV.logical().read("secret/admin/keystore");
                isVaultAvailable = true;
                System.out.println("[VaultServerService] Connexion à Vault établie avec succès.");
            } catch (Exception e) {
                System.err.println("[VaultServerService] Vault injoignable à " + VAULT_ADDR
                        + ". Mode Fallback activé.");
            }
        } catch (VaultException e) {
            System.err.println("[VaultServerService] Erreur configuration Vault : " + e.getMessage());
        }
    }

    // ─── ACCESSEURS ─────────────────────────────────────────────────────────────

    public static boolean isAvailable() {
        return isVaultAvailable;
    }

    public static Vault getVault() {
        return vaultKV;
    }

    // ─── GESTION DES CLÉS PUBLIQUES (KV) ────────────────────────────────────────

    public static void saveAdminPublicKey(String email, String publicKeyBase64) throws Exception {
        if (!isVaultAvailable) {
            System.out.println("[VAULT-KV] Sauvegarde clé en MÉMOIRE (Vault indisponible) : " + email);
            publicKeysMemory.put(email, publicKeyBase64);
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("publicKey", publicKeyBase64);

        String path = "secret/" + KV_KEYS_PATH + sanitize(email);
        System.out.println("[VAULT-KV] Écriture vers : " + path);

        LogicalResponse response = vaultKV.logical().write(path, data);
        int status = response.getRestResponse().getStatus();

        if (status != 200 && status != 204) {
            String error = new String(response.getRestResponse().getBody());
            throw new Exception("Vault Write Error: " + status + " - " + error);
        }
        System.out.println("[VAULT-KV] Clé sauvegardée avec succès.");
    }

    public static String getAdminPublicKey(String email) {
        if (!isVaultAvailable) {
            return publicKeysMemory.get(email);
        }
        try {
            String path = "secret/" + KV_KEYS_PATH + sanitize(email);
            LogicalResponse response = vaultKV.logical().read(path);

            if (response.getRestResponse().getStatus() == 200) {
                return response.getData().get("publicKey");
            }
        } catch (Exception e) {
            System.err.println("[VaultServerService] Erreur lecture clé : " + e.getMessage());
        }
        return null;
    }

    public static Map<String, String> getServerConfig() {
        Map<String, String> config = new HashMap<>();
        if (!isVaultAvailable)
            return config;
        try {
            LogicalResponse response = vaultKV.logical().read("secret/" + KV_CONFIG_PATH);
            if (response.getRestResponse().getStatus() == 200) {
                config.putAll(response.getData());
            }
        } catch (Exception e) {
            System.err.println("[VaultServerService] Erreur lecture config : " + e.getMessage());
        }
        return config;
    }

    /**
     * Récupère une valeur secrète spécifique depuis un chemin Vault complet (ex: secret/keystore-password)
     */
    public static String getSecret(String path) {
        if (!isVaultAvailable) {
            System.err.println("[VaultServerService] Vault est indisponible. Impossible de lire : " + path);
            return null;
        }
        try {
            // Vault v2 structure data inside a 'data' object, bettercloud Vault driver handles this mostly,
            // but if the path is explicitly 'secret/keystore-password', let's read it.
            LogicalResponse response = vaultKV.logical().read(path);
            if (response.getRestResponse().getStatus() == 200) {
                Map<String, String> data = response.getData();
                if (data != null && !data.isEmpty()) {
                    // Usually secrets in KV are key-value. If it's a single value, maybe under 'value' or the key name itself
                    // Let's assume it's stored under the key 'value' or we return the first value.
                    if (data.containsKey("value")) {
                        return data.get("value");
                    }
                    if (data.containsKey("password")) {
                        return data.get("password");
                    }
                    return data.values().iterator().next(); // Return the first value found
                }
            }
        } catch (Exception e) {
            System.err.println("[VaultServerService] Erreur lecture secret " + path + " : " + e.getMessage());
        }
        return null;
    }

    // ─── GESTION SSL / PKI ──────────────────────────────────────────────────────

    /**
     * Récupère le certificat du Root CA pour le TrustStore.
     */
    public static String getRootCA() throws Exception {
        if (!isVaultAvailable)
            return null;
        LogicalResponse response = vaultPKI.logical().read("pki/ca/pem");
        return response.getRestResponse().getBody() != null ? new String(response.getRestResponse().getBody()) : null;
    }

    public static Map<String, String> generateServerCertificate() throws Exception {
        if (!isVaultAvailable) {
            System.out.println("[VaultServerService] Utilisation du certificat de secours (Mode Fallback)");
            return getFallbackCertificate();
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("common_name", "localhost");
            params.put("ttl", "72h");

            LogicalResponse response = vaultPKI.logical().write("pki/issue/" + PKI_ROLE, params);

            if (response.getRestResponse().getStatus() != 200) {
                throw new Exception("Vault PKI Error: " + response.getRestResponse().getStatus());
            }

            JsonNode root = mapper.readTree(response.getRestResponse().getBody());
            JsonNode data = root.path("data");

            Map<String, String> certData = new HashMap<>();
            certData.put("certificate", data.path("certificate").asText(null));
            certData.put("private_key", data.path("private_key").asText(null));
            certData.put("issuing_ca", data.path("issuing_ca").asText(null));

            return certData;
        } catch (Exception e) {
            System.err.println("[VaultServerService] Erreur PKI, repli sur certificat local : " + e.getMessage());
            return getFallbackCertificate();
        }
    }

    /**
     * Fournit un certificat auto-signé de secours pour le développement.
     */
    private static Map<String, String> getFallbackCertificate() {
        Map<String, String> certData = new HashMap<>();
        certData.put("certificate",
                "-----BEGIN CERTIFICATE-----\n" +
                        "MIICpDCCAYwCCQDU5m1Kz9uGzTANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQDDAls\n" +
                        "b2NhbGhvc3QwHhcNMjQwMTAxMDAwMDAwWhcNMzQwMTAxMDAwMDAwWjAUMRIwEAYD\n" +
                        "VQQDDAlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDC\n" +
                        "-----END CERTIFICATE-----");
        return certData;
    }

    // ─── MOTEUR TRANSIT (CHIFFREMENT / DÉCHIFFREMENT) ───────────────────────────

    /**
     * Chiffre une donnée sensible via le moteur Transit de Vault.
     * Exemple : chiffrer un email ou une adresse avant de le stocker en BDD.
     *
     * @param plaintext Le texte en clair à chiffrer
     * @return Le texte chiffré (format vault:v1:xxxxx), ou le texte original si
     *         Vault est indisponible
     */
    public static String transitEncrypt(String plaintext) {
        if (!isVaultAvailable || vaultTransit == null) {
            return plaintext; // Pas de chiffrement si Vault est absent
        }
        try {
            // Transit attend du Base64
            String base64Input = java.util.Base64.getEncoder().encodeToString(plaintext.getBytes());

            Map<String, Object> params = new HashMap<>();
            params.put("plaintext", base64Input);

            LogicalResponse response = vaultTransit.logical()
                    .write("transit/encrypt/" + TRANSIT_KEY, params);

            if (response.getRestResponse().getStatus() == 200) {
                return response.getData().get("ciphertext");
            }
        } catch (Exception e) {
            System.err.println("[VAULT-TRANSIT] Erreur chiffrement : " + e.getMessage());
        }
        return plaintext;
    }

    /**
     * Déchiffre une donnée chiffrée par le moteur Transit.
     *
     * @param ciphertext Le texte chiffré (format vault:v1:xxxxx)
     * @return Le texte en clair, ou le texte chiffré tel quel si Vault est
     *         indisponible
     */
    public static String transitDecrypt(String ciphertext) {
        if (!isVaultAvailable || vaultTransit == null) {
            return ciphertext;
        }
        // Si ce n'est pas un texte chiffré par Vault, le retourner tel quel
        if (ciphertext == null || !ciphertext.startsWith("vault:")) {
            return ciphertext;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("ciphertext", ciphertext);

            LogicalResponse response = vaultTransit.logical()
                    .write("transit/decrypt/" + TRANSIT_KEY, params);

            if (response.getRestResponse().getStatus() == 200) {
                String base64Result = response.getData().get("plaintext");
                return new String(java.util.Base64.getDecoder().decode(base64Result));
            }
        } catch (Exception e) {
            System.err.println("[VAULT-TRANSIT] Erreur déchiffrement : " + e.getMessage());
        }
        return ciphertext;
    }

    // ─── UTILITAIRES ────────────────────────────────────────────────────────────

    private static String sanitize(String email) {
        return email.replaceAll("[^a-zA-Z0-9._@-]", "_");
    }
}
