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
public class ApplicationStatusResponse {

    private UUID applicationId;
    private String applicationNumber;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private boolean additionalDocumentsRequired;
    private Integer creditScore;

    /** Populated only when application is APPROVED. */
    private CardDetails cardDetails;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CardDetails {
        private String maskedCardNumber;
        private CardType cardType;
        private BigDecimal creditLimit;
        private CardStatus cardStatus;
        private LocalDateTime issuedAt;
    }
}
