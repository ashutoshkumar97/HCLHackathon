package com.hcl.zbankcard.controller;

import com.hcl.zbankcard.dto.request.CreditCardApplicationRequest;
import com.hcl.zbankcard.dto.response.CreditCardApplicationResponse;
import com.hcl.zbankcard.service.CreditCardApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class CreditCardApplicationController {

    private final CreditCardApplicationService applicationService;

    /**
     * POST /api/v1/applications
     *
     * Submit a new credit card application. On success, returns the application
     * details including the credit rating result.
     *
     * HTTP 201 – Application created (may be APPROVED / UNDER_REVIEW / REJECTED)
     * HTTP 400 – Validation failure
     * HTTP 409 – Duplicate email or phone
     * HTTP 500 – Unexpected error
     */
    @PostMapping
    public ResponseEntity<CreditCardApplicationResponse> applyForCreditCard(
            @Valid @RequestBody CreditCardApplicationRequest request) {

        log.info("Received credit card application request for email: {}",
                request.getCustomerInfo().getEmail());

        CreditCardApplicationResponse response = applicationService.apply(request);

        log.info("Credit card application {} processed with status: {}",
                response.getApplicationNumber(), response.getStatus());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
