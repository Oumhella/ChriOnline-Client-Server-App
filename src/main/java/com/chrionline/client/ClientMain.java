package com.chrionline.client;

import com.chrionline.client.view.HomeView;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Point d'entrée principal de l'application client ChriOnline.
 */
public class ClientMain {

    public static void main(String[] args) {
        // Appeler launch sur la classe HomeView directement
        // Cela permet de contourner le check strict du JDK sur le thread principal
        Application.launch(HomeView.class, args);
    }
}