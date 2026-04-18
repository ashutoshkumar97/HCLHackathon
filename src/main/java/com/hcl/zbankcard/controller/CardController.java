package com.hcl.zbank.controller;

import com.zbank.creditcard.dto.request.CardIssueRequest;
import com.zbank.creditcard.dto.response.CardResponse;
import com.zbank.creditcard.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping("/issue")
    public ResponseEntity<CardResponse> issueCard(@Valid @RequestBody CardIssueRequest request) {
        log.info("POST /api/v1/cards/issue — applicationId={}", request.getApplicationId());
        CardResponse response = cardService.issueCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/{customerId}")
    public ResponseEntity<CardResponse> getCard(@PathVariable Long customerId) {
        log.info("GET /api/v1/cards/{}", customerId);
        CardResponse response = cardService.getCardByCustomer(customerId);
        return ResponseEntity.ok(response);
    }
}