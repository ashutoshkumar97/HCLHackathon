package com.hcl.zbankcard.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class CardNumbergenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a masked 16-digit card number in the format:
     *   XXXX-XXXX-XXXX-XXXX
     *
     * The first group is a fixed IIN (Issuer Identification Number) prefix
     * for Z Bank: 4532 (Visa range). The middle two groups are masked with
     * 'X' for storage — only the last 4 digits are real and stored.
     *
     * In a real system the full PAN would be encrypted and stored in a
     * separate HSM-backed vault; here we store the display-safe masked form.
     */
    public String generate() {
        // Fixed IIN prefix for Z Bank
        String prefix = "4532";

        // Last 4 digits — real, shown to customer
        int lastFour = 1000 + SECURE_RANDOM.nextInt(9000);

        return prefix + "-XXXX-XXXX-" + lastFour;
    }

    /**
     * Extracts the last 4 digits from a masked card number.
     * Useful for display and audit logging.
     */
    public String lastFour(String maskedCardNumber) {
        return maskedCardNumber.substring(maskedCardNumber.length() - 4);
    }
}