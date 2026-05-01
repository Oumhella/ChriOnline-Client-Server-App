package com.chrionline.admin;

import com.chrionline.admin.view.AdminLoginFrame;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application JavaFX d'administration ChriOnline.
 * Lancée par AdminMain (contournement du check strict JDK).
 */
public class AdminApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        AdminLoginFrame loginFrame = new AdminLoginFrame();
        loginFrame.setTitle("ChriOnline — Administration");
        loginFrame.show();
    }
}
