package com.chrionline.securite;

import java.security.SecureRandom;
import java.util.Base64;

public class ChallengeGenerator {
    public static String generateChallenge() {
        byte[] random = new byte[20];
        new SecureRandom().nextBytes(random);
        return Base64.getEncoder().encodeToString(random) ;
    }
}
