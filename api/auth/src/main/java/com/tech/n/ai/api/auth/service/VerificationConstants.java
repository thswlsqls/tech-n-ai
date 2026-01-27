package com.tech.n.ai.api.auth.service;

public final class VerificationConstants {
    
    public static final String EMAIL_VERIFICATION_TYPE = "EMAIL_VERIFICATION";
    public static final String PASSWORD_RESET_TYPE = "PASSWORD_RESET";
    public static final int TOKEN_EXPIRY_HOURS = 24;
    public static final int TOKEN_BYTE_SIZE = 32;
    
    private VerificationConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
