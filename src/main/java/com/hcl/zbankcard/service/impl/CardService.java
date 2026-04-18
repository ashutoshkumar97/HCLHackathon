package com.zbank.creditcard.service;

import com.zbank.creditcard.config.CardNumberGenerator;
import com.zbank.creditcard.config.PinHashUtil;
import com.zbank.creditcard.dto.request.CardIssueRequest;
import com.zbank.creditcard.dto.response.CardResponse;
import com.zbank.creditcard.entity.CardPin;
import com.zbank.creditcard.entity.CreditCard;
import com.zbank.creditcard.entity.CreditCardApplication;
import com.zbank.creditcard.entity.CreditScore;
import com.zbank.creditcard.enums.ApplicationStatus;
import com.zbank.creditcard.enums.CardStatus;
import com.zbank.creditcard.enums.CardType;
import com.zbank.creditcard.enums.EventType;
import com.zbank.creditcard.exception.BusinessRuleException;
import com.zbank.creditcard.exception.DuplicateResourceException;
import com.zbank.creditcard.exception.ResourceNotFoundException;
import com.zbank.creditcard.repository.ApplicationRepository;
import com.zbank.creditcard.repository.CardPinRepository;
import com.zbank.creditcard.repository.CreditCardRepository;
import com.zbank.creditcard.repository.CreditScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final ApplicationRepository applicationRepository;
    private final CreditScoreRepository creditScoreRepository;
    private final CreditCardRepository creditCardRepository;
    private final CardPinRepository cardPinRepository;
    private final CardNumberGenerator cardNumberGenerator;
    private final PinHashUtil pinHashUtil;
    private final AuditService auditService;

    // ─────────────────────────────────────────────────────────────────────────
    // ISSUE CARD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Issues a credit card for an approved application.
     *
     * Flow:
     *  1. Validate application exists and is not already issued
     *  2. Load credit score for this application
     *  3. Derive card type from score via CardType.fromScore()
     *  4. Generate masked card number and random first-time PIN
     *  5. Persist CreditCard + CardPin atomically
     *  6. Update application status to APPROVED
     *  7. Write CARD_ISSUED audit entry
     *
     * @throws ResourceNotFoundException   if application or score not found
     * @throws DuplicateResourceException  if card already issued for this application
     * @throws BusinessRuleException       if score too low (docs required) or status invalid
     */
    @Transactional
    public CardResponse issueCard(CardIssueRequest request) {
        Long applicationId = request.getApplicationId();
        log.info("Issuing card for applicationId={}", applicationId);

        // ── Step 1: guard checks ──────────────────────────────────────────────
        CreditCardApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found: " + applicationId));

        if (creditCardRepository.existsByApplicationId(applicationId)) {
            throw new DuplicateResourceException(
                    "A card has already been issued for application: " + applicationId);
        }

        // Application must be UNDER_REVIEW or APPROVED (score was just computed)
        if (application.getStatus() == ApplicationStatus.REJECTED) {
            throw new BusinessRuleException(
                    "Cannot issue card — application " + applicationId + " was rejected.");
        }
        if (application.getStatus() == ApplicationStatus.ADDITIONAL_DOCS_REQUIRED) {
            throw new BusinessRuleException(
                    "Cannot issue card — additional documents are required for application: "
                            + applicationId);
        }

        // ── Step 2: fetch credit score ────────────────────────────────────────
        CreditScore creditScore = creditScoreRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Credit score not found for application: " + applicationId
                                + ". Run credit evaluation first."));

        int score = creditScore.getScore();
        log.debug("Credit score for applicationId={} is {}", applicationId, score);

        // ── Step 3: derive card type ──────────────────────────────────────────
        CardType cardType = CardType.fromScore(score);

        if (cardType == null) {
            // Score of 50 — additional documents required, no card issued
            application.setStatus(ApplicationStatus.ADDITIONAL_DOCS_REQUIRED);
            applicationRepository.save(application);
            auditService.log(
                    application.getCustomer().getId(),
                    EventType.ADDITIONAL_DOCS_REQUESTED,
                    "Score " + score + " — additional documents requested, card not issued.");
            throw new BusinessRuleException(
                    "Credit score too low (" + score + "). Additional documents are required.");
        }

        // ── Step 4: generate card number + first-time PIN ─────────────────────
        String maskedCardNumber = cardNumberGenerator.generate();
        String rawFirstTimePin  = pinHashUtil.generateRawPin();
        String hashedPin        = pinHashUtil.hash(rawFirstTimePin);

        log.debug("Generated card number={} for applicationId={}", maskedCardNumber, applicationId);
        // rawFirstTimePin would be sent via secure channel (SMS/post) — not stored
        log.info("First-time PIN generated for card {} [not logged for security]",
                cardNumberGenerator.lastFour(maskedCardNumber));

        // ── Step 5: persist CreditCard ────────────────────────────────────────
        CreditCard creditCard = CreditCard.builder()
                .application(application)
                .customer(application.getCustomer())
                .cardNumber(maskedCardNumber)
                .cardType(cardType)
                .creditLimit(cardType.getCreditLimit())
                .status(CardStatus.INACTIVE)
                .build();

        creditCard = creditCardRepository.save(creditCard);

        // ── Step 5b: persist CardPin ──────────────────────────────────────────
        CardPin cardPin = CardPin.builder()
                .creditCard(creditCard)
                .pinHash(hashedPin)
                .isFirstTimePin(true)
                .build();

        cardPinRepository.save(cardPin);

        // ── Step 6: update application status ────────────────────────────────
        application.setStatus(ApplicationStatus.APPROVED);
        applicationRepository.save(application);

        // ── Step 7: audit ─────────────────────────────────────────────────────
        auditService.log(
                application.getCustomer().getId(),
                creditCard,
                EventType.CARD_ISSUED,
                String.format("%s card issued — limit %.2f — card ending %s",
                        cardType.name(),
                        cardType.getCreditLimit(),
                        cardNumberGenerator.lastFour(maskedCardNumber)));

        log.info("Card issued successfully — cardId={} type={} applicationId={}",
                creditCard.getId(), cardType, applicationId);

        // ── Build + return response ───────────────────────────────────────────
        return CardResponse.builder()
                .cardId(creditCard.getId())
                .cardNumber(maskedCardNumber)
                .cardType(cardType)
                .creditLimit(cardType.getCreditLimit())
                .status(CardStatus.INACTIVE)
                .issuedAt(creditCard.getIssuedAt())
                .message("Card issued successfully. First-time PIN sent via secure channel.")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET CARD BY CUSTOMER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves the current card details for a customer.
     *
     * @throws ResourceNotFoundException if no card exists for the customer
     */
    @Transactional(readOnly = true)
    public CardResponse getCardByCustomer(Long customerId) {
        CreditCard card = creditCardRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No card found for customerId: " + customerId));

        return toResponse(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private CardResponse toResponse(CreditCard card) {
        return CardResponse.builder()
                .cardId(card.getId())
                .cardNumber(card.getCardNumber())
                .cardType(card.getCardType())
                .creditLimit(card.getCreditLimit())
                .status(card.getStatus())
                .issuedAt(card.getIssuedAt())
                .activatedAt(card.getActivatedAt())
                .build();
    }
}