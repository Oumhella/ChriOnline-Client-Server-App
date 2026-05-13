package com.chrionline.securite;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Service centralisé pour le serveur ChriOnline utilisant HashiCorp Vault.
 * Gère :
 * - Le moteur KV (Clés publiques des admins)
 * - Le moteur PKI (Certificats SSL dynamiques)
 */
public class VaultServerService {

    public static final String VAULT_ADDR = "http://127.0.0.1:8200";
    private static final String KV_KEYS_PATH = "admin/keys/"; // Le driver v2 ajoutera 'secret/data/'
    private static final String KV_CONFIG_PATH = "server/config"; 
    private static final String PKI_ROLE = "chrionline-server";

    private static Vault vaultKV;
    private static Vault vaultPKI;
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        try {
            // 1. Récupération du Token depuis les variables d'environnement
            String token = System.getenv("VAULT_TOKEN");
            
            if (token == null || token.trim().isEmpty()) {
                System.err.println("[VaultServerService] CRITICAL: VAULT_TOKEN non configuré !");
                // On peut soit lever une exception, soit laisser Vault échouer plus tard
                // mais il est préférable de prévenir tout de suite.
            }

            // Instance pour le KV (v2)
            VaultConfig configKV = new VaultConfig()
                    .address(VAULT_ADDR)
                    .token(token)
                    .engineVersion(2)
                    .build();
            vaultKV = new Vault(configKV);

            // Instance pour le PKI (v1)
            VaultConfig configPKI = new VaultConfig()
                    .address(VAULT_ADDR)
                    .token(token)
                    .engineVersion(1)
                    .build();
            vaultPKI = new Vault(configPKI);
        } catch (VaultException e) {
            System.err.println("[VaultServerService] Erreur d'initialisation : " + e.getMessage());
        }
    }
    public static Vault getVault() {
        return vaultKV;
    }

    // ─── GESTION DES CLÉS PUBLIQUES (KV) ──────────────────────────────────────

    public static void saveAdminPublicKey(String email, String publicKeyBase64) throws Exception {
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

    /**
     * Récupère la configuration globale du serveur (Secrets partagés).
     */
    public static Map<String, String> getServerConfig() {
        Map<String, String> config = new HashMap<>();
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

    // ─── GESTION SSL / PKI ────────────────────────────────────────────────────

    /**
     * Récupère le certificat du Root CA pour le TrustStore.
     */
    public static String getRootCA() throws Exception {
        LogicalResponse response = vaultPKI.logical().read("pki/ca/pem");
        return response.getRestResponse().getBody() != null ? 
               new String(response.getRestResponse().getBody()) : null;
    }

    public static Map<String, String> generateServerCertificate() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("common_name", "localhost");
        params.put("ttl", "72h");

        LogicalResponse response = vaultPKI.logical().write("pki/issue/" + PKI_ROLE, params);
        
        if (response.getRestResponse().getStatus() != 200) {
            throw new Exception("Vault PKI Error: " + response.getRestResponse().getStatus() + " - " + new String(response.getRestResponse().getBody()));
        }

        JsonNode root = mapper.readTree(response.getRestResponse().getBody());
        JsonNode data = root.path("data");

        Map<String, String> certData = new HashMap<>();
        certData.put("certificate", data.path("certificate").asText(null));
        certData.put("private_key", data.path("private_key").asText(null));
        certData.put("issuing_ca", data.path("issuing_ca").asText(null));
        
        return certData;
    }

    private static String sanitize(String email) {
        return email.replaceAll("[^a-zA-Z0-9._@-]", "_");
    }
}
