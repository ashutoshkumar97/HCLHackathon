package com.hcl.zbankcard.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hcl.zbankcard.dto.request.ChangePinRequest;
import com.hcl.zbankcard.dto.request.LoginRequest;
import com.hcl.zbankcard.dto.response.LoginResponse;
import com.hcl.zbankcard.service.CardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

    private final CardService cardService;

    
    /**
     * POST /api/v1/cards/set-pin
     *
     * First-time PIN setup — only allowed once, when firstTimeLogin=true.
     * Supply the issuance PIN as currentPin, and choose a new 4-digit PIN.
     * HTTP 200 – PIN set successfully.
     * HTTP 400 – New PIN same as current, or firstTimeLogin=false.
     * HTTP 401 – Wrong current PIN or card locked.
     */
    @PostMapping("/set-pin")
    public ResponseEntity<Map<String, String>> setPin(@Valid @RequestBody ChangePinRequest request) {
        cardService.setPin(request);
        return ResponseEntity.ok(Map.of("message", "PIN set successfully. You can now log in with your new PIN."));
    }

    /**
     * POST /api/v1/cards/change-pin
     *
     * Change the PIN on an active card. Verifies the current PIN first.
     * HTTP 200 – PIN changed successfully.
     * HTTP 400 – New PIN same as current.
     * HTTP 401 – Wrong current PIN or card locked.
     */
    @PostMapping("/change-pin")
    public ResponseEntity<Map<String, String>> changePin(@Valid @RequestBody ChangePinRequest request) {
        cardService.changePin(request);
        return ResponseEntity.ok(Map.of("message", "PIN changed successfully."));
    }
}
