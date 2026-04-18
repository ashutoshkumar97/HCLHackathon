package com.hcl.zbankcard.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcl.zbankcard.dto.request.ChangePinRequest;
import com.hcl.zbankcard.dto.request.LoginRequest;
import com.hcl.zbankcard.dto.response.LoginResponse;
import com.hcl.zbankcard.entity.CardPin;
import com.hcl.zbankcard.entity.CreditCard;
import com.hcl.zbankcard.exception.InvalidCredentialsException;
import com.hcl.zbankcard.exception.ResourceNotFoundException;
import com.hcl.zbankcard.repository.CardPinRepository;
import com.hcl.zbankcard.repository.CreditCardRepository;
import com.hcl.zbankcard.service.CardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final CreditCardRepository creditCardRepository;
    private final CardPinRepository cardPinRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        CreditCard card = findActiveCard(request.getCardNumber());
        CardPin cardPin = findCardPin(card);

        verifyPinWithLockout(request.getPin(), cardPin);

        // Successful login — reset failed attempts and record login time
        cardPin.setFailedAttemptCount(0);
        cardPin.setLastLogin(LocalDateTime.now());
        cardPinRepository.save(cardPin);

        log.info("Successful login for card ending in {}",
                request.getCardNumber().substring(request.getCardNumber().length() - 4));

        String message = cardPin.isFirstTimeLogin()
                ? "Login successful. Please set your new PIN using the /api/v1/cards/set-pin endpoint."
                : "Login successful.";

        return LoginResponse.builder()
                .customerId(card.getCustomer().getId())
                .customerName(card.getCustomer().getName())
                .customerEmail(card.getCustomer().getEmail())
                .maskedCardNumber(maskCardNumber(request.getCardNumber()))
                .cardType(card.getCardType())
                .creditLimit(card.getCreditLimit())
                .cardStatus(card.getStatus())
                .firstTimeLogin(cardPin.isFirstTimeLogin())
                .lastLogin(cardPin.getLastLogin())
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public void setPin(ChangePinRequest request) {
        CreditCard card = findActiveCard(request.getCardNumber());
        CardPin cardPin = findCardPin(card);

        if (!cardPin.isFirstTimeLogin()) {
            throw new InvalidCredentialsException(
                    "PIN has already been set. Please use the /api/v1/cards/change-pin endpoint.");
        }

        verifyPinWithLockout(request.getCurrentPin(), cardPin);
        validateNewPin(request.getNewPin(), request.getCurrentPin());

        cardPin.setPinHash(passwordEncoder.encode(request.getNewPin()));
        cardPin.setFirstTimeLogin(false);
        cardPin.setFailedAttemptCount(0);
        cardPin.setLastLogin(LocalDateTime.now());
        cardPinRepository.save(cardPin);

        log.info("PIN set successfully for card ending in {}",
                request.getCardNumber().substring(request.getCardNumber().length() - 4));
    }

    @Override
    @Transactional
    public void changePin(ChangePinRequest request) {
        CreditCard card = findActiveCard(request.getCardNumber());
        CardPin cardPin = findCardPin(card);

        verifyPinWithLockout(request.getCurrentPin(), cardPin);
        validateNewPin(request.getNewPin(), request.getCurrentPin());

        cardPin.setPinHash(passwordEncoder.encode(request.getNewPin()));
        cardPin.setFailedAttemptCount(0);
        cardPinRepository.save(cardPin);

        log.info("PIN changed successfully for card ending in {}",
                request.getCardNumber().substring(request.getCardNumber().length() - 4));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private CreditCard findActiveCard(String cardNumber) {
        return creditCardRepository.findByCardNumberAndDeletedFalse(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active card found for the provided card number."));
    }

    private CardPin findCardPin(CreditCard card) {
        return cardPinRepository.findByCreditCardIdAndDeletedFalse(card.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card PIN record not found."));
    }

    private void verifyPinWithLockout(String rawPin, CardPin cardPin) {
        if (cardPin.getFailedAttemptCount() >= MAX_FAILED_ATTEMPTS) {
            throw new InvalidCredentialsException(
                    "Card is locked due to too many failed PIN attempts. Please contact support.");
        }
        if (!passwordEncoder.matches(rawPin, cardPin.getPinHash())) {
            cardPin.setFailedAttemptCount(cardPin.getFailedAttemptCount() + 1);
            cardPin.setLastFailedAttemptAt(LocalDateTime.now());
            cardPinRepository.save(cardPin);
            int remaining = MAX_FAILED_ATTEMPTS - cardPin.getFailedAttemptCount();
            throw new InvalidCredentialsException(
                    "Invalid PIN. " + remaining + " attempt(s) remaining before card is locked.");
        }
    }

    private void validateNewPin(String newPin, String currentPin) {
        if (newPin.equals(currentPin)) {
            throw new InvalidCredentialsException("New PIN must be different from the current PIN.");
        }
    }

    private String maskCardNumber(String cardNumber) {
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
