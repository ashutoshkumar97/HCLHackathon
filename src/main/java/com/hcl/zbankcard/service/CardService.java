package com.hcl.zbankcard.service;

import com.hcl.zbankcard.dto.request.ChangePinRequest;
import com.hcl.zbankcard.dto.request.LoginRequest;
import com.hcl.zbankcard.dto.response.LoginResponse;

public interface CardService {

    /**
     * Authenticates a cardholder using their 16-digit card number and 4-digit PIN.
     * Tracks failed attempts and locks the card after 3 consecutive failures.
     */
    LoginResponse login(LoginRequest request);

    /**
     * First-time PIN setup — only allowed when {@code firstTimeLogin=true}.
     * Validates the issuance PIN, then replaces it with the customer-chosen PIN
     * and marks {@code firstTimeLogin=false}.
     */
    void setPin(ChangePinRequest request);

    /**
     * Changes the PIN for an active card. Verifies the current PIN before
     * updating to the new one.
     */
    void changePin(ChangePinRequest request);
}
