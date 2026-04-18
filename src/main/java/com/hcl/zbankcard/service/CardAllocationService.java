package com.hcl.zbankcard.service;

import com.hcl.zbankcard.entity.CreditCardApplication;
import com.hcl.zbankcard.entity.Customer;

import java.util.Optional;

public interface CardAllocationService {

    /**
     * Allocates a credit card based on credit score rules:
     * <ul>
     *   <li>Score 500 → PLATINUM card, limit $40,000</li>
     *   <li>Score 300 → GOLD card, limit $20,000</li>
     *   <li>Score 150 → VISA card, limit $10,000</li>
     *   <li>Score 50  → Empty (additional documents required)</li>
     * </ul>
     * When a card IS allocated, a first-time 4-digit PIN is generated, BCrypt-hashed,
     * persisted, and returned in plain text (one-time display).
     *
     * @return populated {@link CardIssuanceResult} if card is issued, empty if additional docs needed
     */
    Optional<CardIssuanceResult> allocate(Customer customer, CreditCardApplication application, int creditScore);
}
