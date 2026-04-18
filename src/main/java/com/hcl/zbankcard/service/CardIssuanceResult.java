package com.hcl.zbankcard.service;

import com.hcl.zbankcard.entity.CardPin;
import com.hcl.zbankcard.entity.CreditCard;

/**
 * Holds the result of a successful card issuance.
 * {@code plainPin} is the one-time plain-text PIN returned to the customer —
 * it is NEVER stored; only the BCrypt hash is persisted in {@link CardPin}.
 */
public record CardIssuanceResult(
        CreditCard creditCard,
        CardPin    cardPin,
        String     plainPin
) {}
