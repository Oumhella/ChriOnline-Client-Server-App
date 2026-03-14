package com.chrionline.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Entry point for the multi-threaded ServerSocket logic.
 */
public class ServerApp {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        System.out.println("Starting ChriOnline Server on port " + PORT + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                // Handle client connection in a new thread
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
