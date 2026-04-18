package com.hcl.zbankcard.service.impl;

import com.hcl.zbankcard.entity.CardPin;
import com.hcl.zbankcard.entity.CreditCard;
import com.hcl.zbankcard.entity.CreditCardApplication;
import com.hcl.zbankcard.entity.Customer;
import com.hcl.zbankcard.entity.enums.CardType;
import com.hcl.zbankcard.repository.CardPinRepository;
import com.hcl.zbankcard.repository.CreditCardRepository;
import com.hcl.zbankcard.service.CardIssuanceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardAllocationServiceImpl")
class CardAllocationServiceImplTest {

    @Mock private CreditCardRepository creditCardRepository;
    @Mock private CardPinRepository    cardPinRepository;
    @Mock private PasswordEncoder      passwordEncoder;

    @InjectMocks private CardAllocationServiceImpl service;

    // ── Card issuance ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("allocate: score 500 should issue PLATINUM card with $40,000 limit")
    void allocate_score500_issuesPlatinumWith40kLimit() {
        stubRepositories();
        Optional<CardIssuanceResult> result = service.allocate(customer(), application(), 500);

        assertThat(result).isPresent();
        CreditCard card = result.get().creditCard();
        assertThat(card.getCardType()).isEqualTo(CardType.PLATINUM);
        assertThat(card.getCreditLimit()).isEqualByComparingTo(CardAllocationServiceImpl.LIMIT_PLATINUM);
    }

    @Test
    @DisplayName("allocate: score 300 should issue GOLD card with $20,000 limit")
    void allocate_score300_issuesGoldWith20kLimit() {
        stubRepositories();
        Optional<CardIssuanceResult> result = service.allocate(customer(), application(), 300);

        assertThat(result).isPresent();
        CreditCard card = result.get().creditCard();
        assertThat(card.getCardType()).isEqualTo(CardType.GOLD);
        assertThat(card.getCreditLimit()).isEqualByComparingTo(CardAllocationServiceImpl.LIMIT_GOLD);
    }

    @Test
    @DisplayName("allocate: score 150 should issue VISA card with $10,000 limit")
    void allocate_score150_issuesVisaWith10kLimit() {
        stubRepositories();
        Optional<CardIssuanceResult> result = service.allocate(customer(), application(), 150);

        assertThat(result).isPresent();
        CreditCard card = result.get().creditCard();
        assertThat(card.getCardType()).isEqualTo(CardType.VISA);
        assertThat(card.getCreditLimit()).isEqualByComparingTo(CardAllocationServiceImpl.LIMIT_VISA);
    }

    @Test
    @DisplayName("allocate: score 50 should return empty (additional documents required)")
    void allocate_score50_returnsEmpty() {
        Optional<CardIssuanceResult> result = service.allocate(customer(), application(), 50);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("allocate: issued card should include a hashed PIN, and return plain PIN once")
    void allocate_whenCardIssued_persistsHashedPinAndReturnsPlainPin() {
        given(passwordEncoder.encode(any())).willReturn("$2a$hashed");
        stubRepositories();

        Optional<CardIssuanceResult> result = service.allocate(customer(), application(), 500);

        assertThat(result).isPresent();
        assertThat(result.get().plainPin()).hasSize(4).matches("\\d{4}");

        ArgumentCaptor<CardPin> pinCaptor = ArgumentCaptor.forClass(CardPin.class);
        verify(cardPinRepository).save(pinCaptor.capture());
        assertThat(pinCaptor.getValue().getPinHash()).isEqualTo("$2a$hashed");
        assertThat(pinCaptor.getValue().isFirstTimeLogin()).isTrue();
    }

    // ── generateCardNumber ──────────────────────────────────────────────────────

    @Test
    @DisplayName("generateCardNumber: PLATINUM prefix should start with 5100")
    void generateCardNumber_platinum_startsWith5100() {
        assertThat(service.generateCardNumber(CardType.PLATINUM)).startsWith("5100").hasSize(16);
    }

    @Test
    @DisplayName("generateCardNumber: GOLD prefix should start with 4532")
    void generateCardNumber_gold_startsWith4532() {
        assertThat(service.generateCardNumber(CardType.GOLD)).startsWith("4532").hasSize(16);
    }

    @Test
    @DisplayName("generateCardNumber: VISA prefix should start with 4111")
    void generateCardNumber_visa_startsWith4111() {
        assertThat(service.generateCardNumber(CardType.VISA)).startsWith("4111").hasSize(16);
    }

    @Test
    @DisplayName("generateCardNumber: generated number should pass Luhn validation")
    void generateCardNumber_passesLuhnCheck() {
        for (CardType type : new CardType[]{CardType.PLATINUM, CardType.GOLD, CardType.VISA}) {
            String number = service.generateCardNumber(type);
            assertThat(luhnValid(number))
                    .as("Card number %s for %s should pass Luhn check", number, type)
                    .isTrue();
        }
    }

    // ── generatePin ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generatePin: should produce a 4-digit numeric string")
    void generatePin_returns4DigitNumericString() {
        String pin = service.generatePin();
        assertThat(pin).hasSize(4).matches("\\d{4}");
        int value = Integer.parseInt(pin);
        assertThat(value).isBetween(1000, 9999);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private void stubRepositories() {
        given(creditCardRepository.save(any(CreditCard.class)))
                .willAnswer(inv -> {
                    CreditCard c = inv.getArgument(0);
                    c.setId(UUID.randomUUID());
                    return c;
                });
        given(cardPinRepository.save(any(CardPin.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(passwordEncoder.encode(any())).willReturn("$2a$bcrypt$hash");
    }

    private Customer customer() {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        return c;
    }

    private CreditCardApplication application() {
        CreditCardApplication app = new CreditCardApplication();
        app.setId(UUID.randomUUID());
        app.setApplicationNumber("ZBNK-TEST-001");
        return app;
    }

    /** Simple Luhn validation for test assertions. */
    private boolean luhnValid(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
