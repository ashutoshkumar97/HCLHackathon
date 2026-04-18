package com.hcl.zbankcard.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hcl.zbankcard.entity.enums.ApplicationStatus;
import com.hcl.zbankcard.entity.enums.CardStatus;
import com.hcl.zbankcard.entity.enums.CardType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditCardApplicationResponse {

    private UUID applicationId;
    private String applicationNumber;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;

    /** True when score is 50 — customer must submit additional identity documents. */
    private boolean additionalDocumentsRequired;

    private CustomerSummary customer;
    private CreditRatingSummary creditRating;

    /** Populated only when the application is APPROVED and a card is issued. */
    private CardIssuanceDetails cardDetails;

    @Getter
    @Builder
    public static class CustomerSummary {
        private UUID customerId;
        private String name;
        private String email;
        private String phone;
    }

    @Getter
    @Builder
    public static class CreditRatingSummary {
        private Integer score;
        private String scoreSource;
        private String message;
    }

    @Getter
    @Builder
    public static class CardIssuanceDetails {
        /** Last-4 masked: **** **** **** 1234 */
        private String maskedCardNumber;
        private CardType cardType;
        private BigDecimal creditLimit;
        private CardStatus cardStatus;
        private LocalDateTime issuedAt;
        /**
         * One-time plain-text PIN shown to the customer exactly once.
         * Never stored in plain text anywhere else in the system.
         */
        private String firstTimePin;
    }
}
