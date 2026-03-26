package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import java.util.HashMap;
import java.util.Map;

public class MesCommandesController {

    private Client client;

    public MesCommandesController() {
        this.client = Client.getInstance();
    }

    public Map<String, Object> getMyOrders() {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "GET_MY_ORDERS");
        return client.envoyerRequeteAttendreReponse(req);
    }
}
