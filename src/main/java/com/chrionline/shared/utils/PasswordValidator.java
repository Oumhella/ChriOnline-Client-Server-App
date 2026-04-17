package com.chrionline.shared.utils;

/**
 * Utilitaire de validation de la robustesse des mots de passe.
 * Centralise les règles de sécurité pour le client et le serveur.
 */
public final class PasswordValidator {

    private PasswordValidator() {}

    /**
     * Calcule un score de force entre 0.0 et 1.0.
     */
    public static double calculerScore(String mdp, String nom, String prenom, String dob) {
        if (mdp == null || mdp.isEmpty()) return 0;

        double s = 0;

        // 1. Critères de base (Longueur, Diversité)
        if (mdp.length() >= 6)               s += 0.20;
        if (mdp.length() >= 10)              s += 0.15;
        if (mdp.matches(".*[A-Z].*"))        s += 0.15;
        if (mdp.matches(".*[0-9].*"))        s += 0.15;
        if (mdp.matches(".*[^a-zA-Z0-9].*")) s += 0.15;

        // 2. Vérification des Informations Personnelles (PII)
        // Si le mot de passe contient le nom, prénom ou DOB, on pénalise fortement.
        boolean contientPii = false;

        if (nom != null && !nom.isBlank() && mdp.toLowerCase().contains(nom.toLowerCase())) {
            contientPii = true;
        }
        if (prenom != null && !prenom.isBlank() && mdp.toLowerCase().contains(prenom.toLowerCase())) {
            contientPii = true;
        }
        if (dob != null && !dob.isBlank()) {
            // On extrait les chiffres de la date (ex: 31/03/2005 -> 31032005)
            String chiffres = dob.replaceAll("[^0-9]", "");
            if (chiffres.length() >= 4) {
                String annee = chiffres.substring(chiffres.length() - 4);
                if (mdp.contains(chiffres) || mdp.contains(annee)) {
                    contientPii = true;
                }
            }
        }

        if (contientPii) {
            s -= 0.50; // Pénalité lourde
        } else {
            s += 0.20; // Bonus si aucune info perso n'est présente
        }

        return Math.min(Math.max(s, 0), 1.0);
    }

    /**
     * Détermine si un mot de passe est considéré comme "FORT".
     * Un mot de passe est fort s'il a une longueur >= 8 ET un score >= 0.70 ET ne contient pas de PII.
     */
    public static boolean estFort(String mdp, String nom, String prenom, String dob) {
        if (mdp == null || mdp.length() < 8) return false;
        
        // Vérification explicite des PII même si le score est élevé
        if (nom != null && !nom.isBlank() && mdp.toLowerCase().contains(nom.toLowerCase())) return false;
        if (prenom != null && !prenom.isBlank() && mdp.toLowerCase().contains(prenom.toLowerCase())) return false;
        
        if (dob != null && !dob.isBlank()) {
            String chiffres = dob.replaceAll("[^0-9]", "");
            if (chiffres.length() >= 4) {
                String annee = chiffres.substring(chiffres.length() - 4);
                if (mdp.contains(chiffres) || mdp.contains(annee)) return false;
            }
        }

        return calculerScore(mdp, nom, prenom, dob) >= 0.70;
    }
}
