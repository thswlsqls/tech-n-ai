package com.tech.n.ai.api.auth.service;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureTokenGenerator {
    
    private static final int TOKEN_BYTE_SIZE = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    private SecureTokenGenerator() {
    }
    
    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTE_SIZE];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
