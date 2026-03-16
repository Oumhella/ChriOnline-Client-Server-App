package com.chrionline.client;

import com.chrionline.client.view.CatalogueView;
import com.chrionline.client.view.InscriptionView;
import javafx.application.Application;

/**
 * Point d'entrée principal de l'application client ChriOnline.
 */
public class ClientMain {
    public static void main(String[] args) {
        Application.launch(CatalogueView.class, args);
    }
}