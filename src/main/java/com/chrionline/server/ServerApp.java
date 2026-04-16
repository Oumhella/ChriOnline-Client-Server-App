package com.chrionline.server;

import com.chrionline.server.core.Server;

/**
 * Point d'entrée principal du serveur ChriOnline.
 */
public class ServerApp {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        System.out.println("=ChriOnline Server — Initialisation ");
        
        // On instancie la classe Server (qui gère ClientHandler, TCP et UDP)
        Server server = new Server(PORT);
        
        // On démarre la boucle infinie d'écoute
        server.demarrer();
    }
}
