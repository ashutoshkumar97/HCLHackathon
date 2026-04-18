package com.hcl.zbankcard.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hcl.zbankcard.dto.ApiResponse;
import com.hcl.zbankcard.dto.LoginResponse;
import com.hcl.zbankcard.entity.CreditCard;
import com.hcl.zbankcard.entity.CreditCardApplication;
import com.hcl.zbankcard.entity.Customer;
import com.hcl.zbankcard.exception.ResourceNotFoundException;
import com.hcl.zbankcard.repository.CreditCardApplicationRepository;
import com.hcl.zbankcard.repository.CreditCardRepository;
import com.hcl.zbankcard.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CreditCardApplicationRepository applicationRepository;
    private final CreditCardRepository creditCardRepository;

    /**
     * GET /api/v1/customers/me/applications
     *
     * Returns all credit card applications submitted by the authenticated customer.
     * Requires: Authorization: Bearer <token>
     */
    @GetMapping("/me/applications")
    public ApiResponse<List<LoginResponse.ApplicationSummary>> getMyApplications(Authentication auth) {
        Customer customer = resolveCustomer(auth.getName());
        List<CreditCardApplication> applications =
                applicationRepository.findByCustomerIdAndDeletedFalse(customer.getId());

        List<LoginResponse.ApplicationSummary> result = applications.stream()
                .map(app -> LoginResponse.ApplicationSummary.builder()
                        .applicationId(app.getId())
                        .applicationNumber(app.getApplicationNumber())
                        .status(app.getStatus())
                        .appliedAt(app.getAppliedAt())
                        .build())
                .collect(Collectors.toList());

        log.info("Fetched {} application(s) for customer: {}", result.size(), customer.getEmail());
        return new ApiResponse<>(200, "SUCCESS", "Applications retrieved successfully", result);
    }

    /**
     * GET /api/v1/customers/me/cards
     *
     * Returns all credit cards issued to the authenticated customer.
     * Requires: Authorization: Bearer <token>
     */
    @GetMapping("/me/cards")
    public ApiResponse<List<LoginResponse.CardSummary>> getMyCards(Authentication auth) {
        Customer customer = resolveCustomer(auth.getName());
        List<CreditCard> cards =
                creditCardRepository.findByCustomerIdAndDeletedFalse(customer.getId());

        List<LoginResponse.CardSummary> result = cards.stream()
                .map(card -> LoginResponse.CardSummary.builder()
                        .cardId(card.getId())
                        .maskedCardNumber(maskCardNumber(card.getCardNumber()))
                        .cardType(card.getCardType())
                        .creditLimit(card.getCreditLimit())
                        .cardStatus(card.getStatus())
                        .issuedAt(card.getIssuedAt())
                        .build())
                .collect(Collectors.toList());

        log.info("Fetched {} card(s) for customer: {}", result.size(), customer.getEmail());
        return new ApiResponse<>(200, "SUCCESS", "Cards retrieved successfully", result);
    }

    private Customer resolveCustomer(String email) {
        return customerRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for email: " + email));
    }

    private String maskCardNumber(String cardNumber) {
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
