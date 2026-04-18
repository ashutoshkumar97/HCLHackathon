package com.hcl.zbankcard.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hcl.zbankcard.dto.ApiResponse;
import com.hcl.zbankcard.dto.LoginRequest;
import com.hcl.zbankcard.dto.LoginResponse;
import com.hcl.zbankcard.service.CardAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class CardAuthController {

	CardAuthService cardAuthService;

	public CardAuthController(CardAuthService cardAuthService) {
		this.cardAuthService = cardAuthService;
	}

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {

		LoginResponse response = cardAuthService.login(request);

		String message = response.isFirstLogin()
				? "Login successful. You have cards with first-time PIN — please update them."
				: "Login successful";

		return new ApiResponse<>(200, "SUCCESS", message, response);
	}
}