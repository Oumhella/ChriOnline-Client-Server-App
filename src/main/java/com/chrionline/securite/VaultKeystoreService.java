package com.chrionline.securite;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;

/**
 * Service pour récupérer le mot de passe du Keystore admin depuis Vault.
 * 
 * Supporte deux modes d'authentification :
 * 1. AppRole (VAULT_ROLE_ID + VAULT_SECRET_ID) — Recommandé
 * 2. Token statique (VAULT_TOKEN) — Legacy / Compatibilité
 * 
 * Si Vault est indisponible, retourne un mot de passe de secours (mode dégradé).
 */
public class VaultKeystoreService {

    private static final String VAULT_ADDR = "http://127.0.0.1:8200";
    private static final String SECRET_PATH = "secret/admin/keystore";
    private static final String FALLBACK_PASSWORD = "testPass123";

    /**
     * Récupère le mot de passe du Keystore depuis Vault.
     * En mode dégradé (Vault indisponible), retourne le mot de passe de secours.
     */
    public static char[] getKeystorePassword() throws Exception {
        // 1. Vérifier si Vault est joignable
        if (!isVaultReachable()) {
            System.err.println("[VaultKeystoreService] Vault injoignable. Utilisation du mot de passe de secours.");
            return FALLBACK_PASSWORD.toCharArray();
        }

        // 2. Tenter l'authentification AppRole
        String roleId = System.getenv("VAULT_ROLE_ID");
        String secretId = System.getenv("VAULT_SECRET_ID");
        String token = System.getenv("VAULT_TOKEN");

        if (roleId != null && !roleId.trim().isEmpty()
                && secretId != null && !secretId.trim().isEmpty()) {
            return getPasswordWithAppRole(roleId, secretId);
        } else if (token != null && !token.trim().isEmpty()) {
            return getPasswordWithToken(token);
        } else {
            System.err.println("[VaultKeystoreService] Aucune authentification configurée. Mode secours.");
            return FALLBACK_PASSWORD.toCharArray();
        }
    }

    /**
     * Récupère le mot de passe via authentification AppRole.
     */
    private static char[] getPasswordWithAppRole(String roleId, String secretId) {
        try {
            // 1. Authentification AppRole → obtenir un token temporaire
            VaultConfig loginConfig = new VaultConfig()
                    .address(VAULT_ADDR)
                    .build();
            Vault loginVault = new Vault(loginConfig);
            AuthResponse authResponse = loginVault.auth().loginByAppRole(roleId, secretId);
            String appToken = authResponse.getAuthClientToken();

            // 2. Utiliser le token pour lire le secret
            return getPasswordWithToken(appToken);
        } catch (VaultException e) {
            System.err.println("[VaultKeystoreService] Échec AppRole : " + e.getMessage());
            return FALLBACK_PASSWORD.toCharArray();
        }
    }

    /**
     * Récupère le mot de passe avec un token Vault.
     */
    private static char[] getPasswordWithToken(String token) {
        try {
            VaultConfig config = new VaultConfig()
                    .address(VAULT_ADDR)
                    .token(token)
                    .engineVersion(2)
                    .build();

            Vault vault = new Vault(config);
            LogicalResponse response = vault.logical().read(SECRET_PATH);

            if (response.getRestResponse().getStatus() != 200) {
                System.err.println("[VaultKeystoreService] Erreur Vault (Status: "
                        + response.getRestResponse().getStatus() + "). Mode secours.");
                return FALLBACK_PASSWORD.toCharArray();
            }

            String passwordStr = response.getData().get("password");
            if (passwordStr == null) {
                System.err.println("[VaultKeystoreService] Clé 'password' absente dans le secret. Mode secours.");
                return FALLBACK_PASSWORD.toCharArray();
            }

            return passwordStr.toCharArray();
        } catch (VaultException e) {
            System.err.println("[VaultKeystoreService] Erreur Vault : " + e.getMessage() + ". Mode secours.");
            return FALLBACK_PASSWORD.toCharArray();
        }
    }

    /**
     * Vérifie si le serveur Vault est joignable (Health Check).
     */
    private static boolean isVaultReachable() {
        try {
            java.net.URL url = new java.net.URL(VAULT_ADDR + "/v1/sys/health");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            // Vault renvoie 200 (OK), 429 (Standby), 472 (DR), 473 (Performance Standby), 501 (Not Init), 503 (Sealed)
            return code > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
