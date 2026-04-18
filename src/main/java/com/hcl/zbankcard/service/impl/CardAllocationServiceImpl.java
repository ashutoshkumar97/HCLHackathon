package com.hcl.zbankcard.service.impl;

import com.hcl.zbankcard.entity.CardPin;
import com.hcl.zbankcard.entity.CreditCard;
import com.hcl.zbankcard.entity.CreditCardApplication;
import com.hcl.zbankcard.entity.Customer;
import com.hcl.zbankcard.entity.enums.CardStatus;
import com.hcl.zbankcard.entity.enums.CardType;
import com.hcl.zbankcard.repository.CardPinRepository;
import com.hcl.zbankcard.repository.CreditCardRepository;
import com.hcl.zbankcard.service.CardAllocationService;
import com.hcl.zbankcard.service.CardIssuanceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Allocates a credit card tier based on the applicant's credit score and
 * generates a first-time 4-digit PIN.
 *
 * Score → Card mapping:
 *   500 → PLATINUM, $40,000 limit
 *   300 → GOLD,     $20,000 limit
 *   150 → VISA,     $10,000 limit
 *   50  → No card – additional documents requested
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardAllocationServiceImpl implements CardAllocationService {

    static final BigDecimal LIMIT_PLATINUM = new BigDecimal("40000.00");
    static final BigDecimal LIMIT_GOLD     = new BigDecimal("20000.00");
    static final BigDecimal LIMIT_VISA     = new BigDecimal("10000.00");

    private final CreditCardRepository creditCardRepository;
    private final CardPinRepository    cardPinRepository;
    private final PasswordEncoder      passwordEncoder;

    @Override
    public Optional<CardIssuanceResult> allocate(
            Customer customer, CreditCardApplication application, int creditScore) {

        return resolveCardConfig(creditScore)
                .map(config -> issueCard(customer, application, config));
    }

    // ── private helpers ──────────────────────────────────────────────────────────

    private Optional<CardConfig> resolveCardConfig(int score) {
        if (score >= 500) {
            return Optional.of(new CardConfig(CardType.PLATINUM, LIMIT_PLATINUM));
        } else if (score >= 300) {
            return Optional.of(new CardConfig(CardType.GOLD, LIMIT_GOLD));
        } else if (score >= 150) {
            return Optional.of(new CardConfig(CardType.VISA, LIMIT_VISA));
        } else {
            return Optional.empty(); // score 50 → request additional documents
        }
    }

    private CardIssuanceResult issueCard(
            Customer customer, CreditCardApplication application, CardConfig config) {

        String cardNumber = generateCardNumber(config.cardType());

        CreditCard card = CreditCard.builder()
                .customer(customer)
                .application(application)
                .cardNumber(cardNumber)
                .cardType(config.cardType())
                .creditLimit(config.limit())
                .status(CardStatus.INACTIVE)
                .issuedAt(LocalDateTime.now())
                .build();

        CreditCard savedCard = creditCardRepository.save(card);

        String plainPin = generatePin();
        CardPin pin = CardPin.builder()
                .creditCard(savedCard)
                .pinHash(passwordEncoder.encode(plainPin))
                .firstTimeLogin(true)
                .build();
        cardPinRepository.save(pin);

        log.info("Issued {} card (limit={}) for application {}",
                config.cardType(), config.limit(), application.getApplicationNumber());

        return new CardIssuanceResult(savedCard, pin, plainPin);
    }

    /**
     * Generates a 16-digit card number with a valid Luhn check digit.
     * Package-private for unit testing.
     */
    String generateCardNumber(CardType cardType) {
        String prefix = switch (cardType) {
            case PLATINUM -> "5100";
            case GOLD     -> "4532";
            case VISA     -> "4111";
            default       -> "4000";
        };
        StringBuilder sb = new StringBuilder(prefix);
        IntStream.range(0, 15 - prefix.length())
                .forEach(i -> sb.append(ThreadLocalRandom.current().nextInt(10)));
        sb.append(computeLuhnCheckDigit(sb.toString()));
        return sb.toString();
    }

    /**
     * Generates a random 4-digit PIN (1000–9999). Package-private for unit testing.
     */
    String generatePin() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 10000));
    }

    private int computeLuhnCheckDigit(String partialNumber) {
        int sum = 0;
        boolean doubleIt = true;
        for (int i = partialNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(partialNumber.charAt(i));
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return (10 - (sum % 10)) % 10;
    }

    private record CardConfig(CardType cardType, BigDecimal limit) {}
}
