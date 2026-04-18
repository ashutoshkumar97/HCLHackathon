package com.hcl.zbankcard.service;

import com.hcl.zbankcard.entity.CreditCardApplication;
import com.hcl.zbankcard.entity.CreditScore;
import com.hcl.zbankcard.entity.Customer;

public interface CreditRatingService {

    /**
     * Evaluates the credit score for a given customer and application.
     * <p>
     * Rules:
     * <ol>
     *   <li>If an existing credit score is on record, return that value.</li>
     *   <li>Otherwise calculate based on card count and annual salary:
     *     <ul>
     *       <li>Holds 2+ credit cards  → score 300</li>
     *       <li>Salary &gt; $200,000   → score 500</li>
     *       <li>Salary $50,001–$200,000 → score 150</li>
     *       <li>Salary ≤ $50,000       → score 50</li>
     *     </ul>
     *   </li>
     * </ol>
     * The returned {@link CreditScore} is NOT yet persisted.
     */
    CreditScore evaluate(Customer customer, CreditCardApplication application);
}
