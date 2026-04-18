package com.hcl.zbankcard.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class LoginResponse {

    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String maskedCardNumber;
    private CardType cardType;
    private BigDecimal creditLimit;
    private CardStatus cardStatus;
    private boolean firstTimeLogin;
    private LocalDateTime lastLogin;
    private String message;
}
