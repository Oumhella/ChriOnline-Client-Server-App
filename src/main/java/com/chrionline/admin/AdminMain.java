package com.chrionline.admin;

import javafx.application.Application;

/**
 * Point d'entrée de l'application d'administration ChriOnline.
 *
 * Cette classe NE doit PAS étendre Application directement,
 * sinon le JDK 9+ bloque le lancement avec l'erreur
 * "JavaFX runtime components are missing".
 *
 * C'est le même pattern que ClientMain → HomeView.
 *
 * Lancement :
 *   java -cp ChriOnline-Admin.jar com.chrionline.admin.AdminMain
 */
public class AdminMain {

    public static void main(String[] args) {
        Application.launch(AdminApp.class, args);
    }
}
