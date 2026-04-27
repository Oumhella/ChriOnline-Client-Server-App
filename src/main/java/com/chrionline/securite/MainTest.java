package com.chrionline.securite;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class MainTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Test de l'authentification Challenge-Response avec KeyStore ===");

            // 1. Génération du challenge
            String challenge = ChallengeGenerator.generateChallenge();
            System.out.println("[1] Challenge généré : " + challenge);

            // 2. Chargement de la clé privée depuis le KeyStore
            String keystorePath = "keystore_test.jks"; // Généré à la racine du projet
            String pass = "testPass123";
            String alias = "testAlias";
            
            PrivateKey privateKey = KeyStoreManager.getPrivateKey(keystorePath, alias);
            System.out.println("[2] Clé privée chargée avec succès.");
            System.out.println("    Voici son contenu (Base64) :");
            System.out.println("    " + Base64.getEncoder().encodeToString(privateKey.getEncoded()));
            System.out.println("---------------------------------------------------");

            // 3. Signature du challenge
            byte[] signature = Signer.sign(challenge, privateKey);
            System.out.println("[3] Challenge signé par le serveur/client.");

            // 4. Chargement de la clé publique
            PublicKey publicKey = KeyStoreManager.getPublicKey(keystorePath, alias);
            System.out.println("[4] Clé publique chargée avec succès.");
            System.out.println("    Voici son contenu (Base64) :");
            System.out.println("    " + Base64.getEncoder().encodeToString(publicKey.getEncoded()));
            System.out.println("---------------------------------------------------");

            // 5. Vérification
            boolean isValid = Verifier.verify(challenge, signature, publicKey);
            
            if (isValid) {
                System.out.println("[5] SUCCÈS : La signature du challenge est VALIDE !");
            } else {
                System.out.println("[5] ÉCHEC : La signature est invalide !");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du test : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
