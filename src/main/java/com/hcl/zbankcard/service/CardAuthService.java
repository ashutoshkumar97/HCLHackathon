package com.hcl.zbankcard.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hcl.zbankcard.config.JwtUtil;
import com.hcl.zbankcard.dto.LoginRequest;
import com.hcl.zbankcard.dto.LoginResponse;
import com.hcl.zbankcard.entity.CardPin;
import com.hcl.zbankcard.exception.BusinessException;
import com.hcl.zbankcard.exception.ResourceNotFoundException;
import com.hcl.zbankcard.repository.CardPinRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardAuthService {

	CardPinRepository cardPinRepository;

	PasswordEncoder passwordEncoder;

	JwtUtil jwtUtil;

	public CardAuthService(CardPinRepository cardPinRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
		this.cardPinRepository = cardPinRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
	}

	private static final int MAX_FAILED_ATTEMPTS = 3;

	public LoginResponse login(LoginRequest request) {

		// 1. Fetch CardPin
		CardPin cardPin = cardPinRepository.findByCreditCard_CardNumber(request.getCardNumber())
				.orElseThrow(() -> new ResourceNotFoundException("Card not found"));

		// 2. Check if blocked
		if (cardPin.getFailedAttemptCount() >= MAX_FAILED_ATTEMPTS) {
			throw new BusinessException("Card is blocked due to multiple failed attempts");
		}

		// 3. Validate PIN
		if (!passwordEncoder.matches(request.getPin(), cardPin.getPinHash())) {

			cardPin.setFailedAttemptCount(cardPin.getFailedAttemptCount() + 1);
			cardPin.setLastFailedAttemptAt(LocalDateTime.now());
			cardPinRepository.save(cardPin);

			throw new BusinessException("Invalid PIN");
		}

		// 4. Reset failed attempts on success
		cardPin.setFailedAttemptCount(0);
		cardPin.setLastLogin(LocalDateTime.now());

		// 5. Generate token (ALWAYS)
		String token = jwtUtil.generateToken(cardPin.getCreditCard().getCardNumber());

		// 6. First login check
		if (cardPin.isFirstTimeLogin()) {
			cardPinRepository.save(cardPin);
			return new LoginResponse(true, token); // ✅ token included
		}

		// 7. Normal login
		cardPinRepository.save(cardPin);

		return new LoginResponse(false, token);
	}
}