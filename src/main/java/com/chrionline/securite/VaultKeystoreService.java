package com.chrionline.securite;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;

/**
 * Service pour interagir avec HashiCorp Vault.
 * Permet de récupérer le mot de passe du Keystore de façon sécurisée via API.
 */
public class VaultKeystoreService {

    // L'URL de développement locale de Vault
    private static final String VAULT_ADDR = "http://127.0.0.1:8200";
    
    // Le chemin vers le secret (sans le prefixe 'data/' car le driver v2 l'ajoute automatiquement)
    private static final String SECRET_PATH = "secret/admin/keystore";

    /**
     * Se connecte à Vault en utilisant le VAULT_TOKEN et récupère le mot de passe JKS.
     * 
     * @return le mot de passe sous forme de char[]
     * @throws Exception si la connexion échoue, si le token est invalide, ou si le secret n'existe pas
     */
    public static char[] getKeystorePassword() throws Exception {
        // 1. Lire le Token depuis les variables d'environnement système
        String vaultToken = System.getenv("VAULT_TOKEN");
        if (vaultToken == null || vaultToken.trim().isEmpty()) {
            throw new Exception("VAULT_TOKEN introuvable dans les variables d'environnement.\n" +
                                "Veuillez configurer la variable d'environnement VAULT_TOKEN sur votre OS.");
        }

        try {
            // 2. Configurer le client Vault
            VaultConfig config = new VaultConfig()
                    .address(VAULT_ADDR)
                    .token(vaultToken)
                    .engineVersion(2) // On force l'utilisation du moteur KV v2
                    .build();

            // 3. Instancier le client Vault
            Vault vault = new Vault(config);

            // 4. Effectuer la requête GET
            LogicalResponse response = vault.logical().read(SECRET_PATH);

            // Vérification de la réponse
            if (response.getRestResponse().getStatus() != 200) {
                throw new Exception("Erreur API Vault (Status: " + response.getRestResponse().getStatus() + ")");
            }

            // 5. Extraire le mot de passe depuis la map de données ("data" -> "password" ou directement "password" selon le KV)
            // Dans KV v2, le secret est encapsulé dans "data"
            String passwordStr = response.getData().get("password");

            if (passwordStr == null) {
                throw new Exception("Le secret '" + SECRET_PATH + "' ne contient pas la clé 'password'.");
            }

            // 6. Convertir en char[] pour une meilleure sécurité mémoire
            return passwordStr.toCharArray();

        } catch (VaultException e) {
            // 7. Gestion des erreurs spécifiques à Vault (Vault injoignable, token expiré...)
            throw new Exception("Impossible de récupérer le mot de passe depuis Vault. Raison : " + e.getMessage(), e);
        }
    }
}
