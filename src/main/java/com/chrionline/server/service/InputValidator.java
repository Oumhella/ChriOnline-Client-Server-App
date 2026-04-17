package com.chrionline.server.service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilitaire centralisé de validation et d'assainissement des entrées côté serveur.
 * Protège contre les injections de commandes, SQL, path traversal, etc.
 */
public class InputValidator {

    // ─── Patterns de validation ──────────────────────────────────────────────

    /** Email : format standard RFC simplifié */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    /** Nom/Prénom : lettres (y compris accents), espaces, tirets, apostrophes */
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\p{L} '\\-]{1,50}$");

    /** Téléphone : chiffres, +, espaces, tirets */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9+\\s\\-]{0,20}$");

    /** Extensions d'image autorisées */
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "gif", "webp");

    /** Statuts de compte autorisés */
    private static final Set<String> ALLOWED_ACCOUNT_STATUTS =
            Set.of("actif", "bloque");

    /** Taille maximale d'un upload image : 10 Mo */
    public static final int MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    // ─── Assainissement (Sanitization) ───────────────────────────────────────

    /**
     * Assainit une chaîne : supprime les caractères de contrôle, les octets nuls,
     * et tronque à la longueur maximale.
     *
     * @param input     la chaîne brute (peut être null)
     * @param maxLength longueur maximale autorisée
     * @return chaîne assainie, ou chaîne vide si null
     */
    public static String sanitizeString(String input, int maxLength) {
        if (input == null) return "";
        // Supprimer les caractères de contrôle et les octets nuls
        String cleaned = input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        // Tronquer à la longueur maximale
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned.trim();
    }

    // ─── Validation ──────────────────────────────────────────────────────────

    /**
     * Vérifie qu'un email a un format valide.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Vérifie qu'un nom/prénom ne contient que des caractères autorisés.
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isBlank()) return false;
        return NAME_PATTERN.matcher(name.trim()).matches();
    }

    /**
     * Vérifie qu'un numéro de téléphone a un format valide.
     * Accepte les chaînes vides (champ optionnel).
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) return true; // Optionnel
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Vérifie qu'une extension de fichier est dans la liste blanche d'images.
     */
    public static boolean isValidImageExtension(String extension) {
        if (extension == null || extension.isBlank()) return false;
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase().trim());
    }

    /**
     * Vérifie qu'un statut de compte est dans la liste autorisée.
     */
    public static boolean isValidAccountStatut(String statut) {
        if (statut == null || statut.isBlank()) return false;
        return ALLOWED_ACCOUNT_STATUTS.contains(statut.toLowerCase().trim());
    }

    /**
     * Vérifie qu'un nom de fichier ne contient pas de caractères de traversée de chemin.
     * Rejette : "..", "/", "\", les caractères nuls.
     */
    public static boolean isSafePath(String path) {
        if (path == null || path.isBlank()) return false;
        return !path.contains("..") &&
               !path.contains("/") &&
               !path.contains("\\") &&
               !path.contains("\0");
    }

    /**
     * Vérifie la force d'un mot de passe.
     * Minimum 6 caractères (cohérent avec le contrôle existant).
     */
    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.length() >= 6;
    }

    /**
     * Vérifie qu'une valeur de code postal est valide (chiffres et lettres, max 10 chars).
     */
    public static boolean isValidCodePostal(String cp) {
        if (cp == null || cp.isBlank()) return true; // Optionnel
        return cp.trim().matches("^[A-Za-z0-9\\s\\-]{1,10}$");
    }
}
