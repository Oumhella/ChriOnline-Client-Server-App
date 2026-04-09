package com.chrionline.shared.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modèle pour représenter un événement de sécurité dans le dashboard.
 */
public class SecurityEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String timestamp;
    private String type;
    private String ip;
    private String context;

    public SecurityEvent(String type, String ip, String context) {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.type = type;
        this.ip = ip;
        this.context = context;
    }

    // Getters
    public String getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public String getIp() { return ip; }
    public String getContext() { return context; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + type + " - " + ip + " (" + context + ")";
    }
}
