package com.chrionline.securite;
import java.security.*;

public class RSAKeyPairGenerator {
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();



    }
}
