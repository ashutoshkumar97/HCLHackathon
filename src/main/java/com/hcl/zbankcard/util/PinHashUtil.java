package com.zbank.creditcard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class PinHashUtil {

    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a random 4-digit numeric PIN.
     * Only called once — at card issuance — to produce the first-time PIN.
     * The raw value is returned here so it can be delivered to the customer
     * via a secure channel; it must never be persisted in plain text.
     */
    public String generateRawPin() {
        int pin = 1000 + SECURE_RANDOM.nextInt(9000); // range: 1000–9999
        return String.valueOf(pin);
    }

    /**
     * Hashes a raw PIN using BCrypt.
     * Used both at card issuance (first-time PIN) and during first-login (new PIN).
     */
    public String hash(String rawPin) {
        return passwordEncoder.encode(rawPin);
    }

    /**
     * Verifies a raw PIN against a stored BCrypt hash.
     * Used during the first-login flow to validate the bank-issued PIN.
     */
    public boolean verify(String rawPin, String storedHash) {
        return passwordEncoder.matches(rawPin, storedHash);
    }
}