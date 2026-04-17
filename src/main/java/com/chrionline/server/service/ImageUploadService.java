package com.chrionline.server.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ImageUploadService {

    private static final String UPLOAD_DIR = "uploads/";

    static {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    /**
     * Sauvegarde une image uploadée après validation de sécurité.
     *
     * @param data      données binaires de l'image
     * @param extension extension demandée par le client
     * @return chemin relatif du fichier sauvegardé, ou null en cas d'erreur
     */
    public static String saveImage(byte[] data, String extension) {
        // ─── Validation des données ──────────────────────────────────────
        if (data == null || data.length == 0) {
            System.err.println("[ImageService] Données image vides ou nulles.");
            return null;
        }

        if (data.length > InputValidator.MAX_IMAGE_SIZE) {
            System.err.println("[ImageService] Image trop volumineuse : " + data.length + " octets (max " + InputValidator.MAX_IMAGE_SIZE + ").");
            return null;
        }

        // ─── Validation de l'extension (whitelist) ───────────────────────
        if (!InputValidator.isValidImageExtension(extension)) {
            System.err.println("[ImageService] Extension rejetée : '" + extension + "'. Extensions autorisées : png, jpg, jpeg, gif, webp.");
            return null;
        }

        // ─── Vérification anti-traversée de chemin ───────────────────────
        if (!InputValidator.isSafePath(extension)) {
            System.err.println("[ImageService] Extension contient des caractères de traversée : '" + extension + "'.");
            return null;
        }

        // ─── Génération du nom de fichier sécurisé ───────────────────────
        String cleanExt = extension.toLowerCase().trim();
        String fileName = UUID.randomUUID().toString() + "." + cleanExt;
        String filePath = UPLOAD_DIR + fileName;

        // ─── Vérification canonique du chemin final ──────────────────────
        try {
            File targetFile = new File(filePath);
            File uploadDir = new File(UPLOAD_DIR);
            String canonicalTarget = targetFile.getCanonicalPath();
            String canonicalUploadDir = uploadDir.getCanonicalPath();

            if (!canonicalTarget.startsWith(canonicalUploadDir)) {
                System.err.println("[ImageService] Tentative de traversée de chemin détectée : " + canonicalTarget);
                return null;
            }

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(data);
                System.out.println("[ImageService] Fichier sauvegardé : " + filePath);
                return filePath;
            }
        } catch (IOException e) {
            System.err.println("[ImageService] Erreur sauvegarde : " + e.getMessage());
            return null;
        }
    }
}
