package com.hcl.zbankcard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hcl.zbankcard.entity.enums.ApplicationStatus;
import com.hcl.zbankcard.entity.enums.CardStatus;
import com.hcl.zbankcard.entity.enums.CardType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String token;
    private boolean firstLogin;
    private CustomerSummary customer;
    private List<ApplicationSummary> applications;
    private List<CardSummary> cards;

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
    public static class ApplicationSummary {
        private UUID applicationId;
        private String applicationNumber;
        private ApplicationStatus status;
        private LocalDateTime appliedAt;
    }

    @Getter
    @Builder
    public static class CardSummary {
        private UUID cardId;
        private String maskedCardNumber;
        private CardType cardType;
        private BigDecimal creditLimit;
        private CardStatus cardStatus;
        private LocalDateTime issuedAt;
    }
}