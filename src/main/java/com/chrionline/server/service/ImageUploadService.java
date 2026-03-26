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

    public static String saveImage(byte[] data, String extension) {
        String fileName = UUID.randomUUID().toString() + "." + extension;
        String filePath = UPLOAD_DIR + fileName;
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(data);
            System.out.println("[ImageService] Fichier sauvegardé : " + filePath);
            return filePath;
        } catch (IOException e) {
            System.err.println("[ImageService] Erreur sauvegarde : " + e.getMessage());
            return null;
        }
    }
}
