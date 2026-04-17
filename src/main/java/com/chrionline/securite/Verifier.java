package com.chrionline.securite;

import java.security.PublicKey;
import java.security.Signature;

public class Verifier {
    public static boolean verify (String challenge, byte[]signatureBytes, PublicKey publicKey )throws Exception {
        Signature signature =Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(challenge.getBytes());
        return signature.verify(signatureBytes);

    }
}
