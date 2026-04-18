package com.hcl.zbankcard.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hcl.zbankcard.config.JwtUtil;
import com.hcl.zbankcard.dto.LoginRequest;
import com.hcl.zbankcard.dto.LoginResponse;
import com.hcl.zbankcard.entity.CardPin;
import com.hcl.zbankcard.entity.CreditCard;
import com.hcl.zbankcard.entity.CreditCardApplication;
import com.hcl.zbankcard.entity.Customer;
import com.hcl.zbankcard.exception.BusinessException;
import com.hcl.zbankcard.repository.CardPinRepository;
import com.hcl.zbankcard.repository.CreditCardApplicationRepository;
import com.hcl.zbankcard.repository.CreditCardRepository;
import com.hcl.zbankcard.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardAuthService {

	private final CustomerRepository customerRepository;
	private final CreditCardApplicationRepository applicationRepository;
	private final CreditCardRepository creditCardRepository;
	private final CardPinRepository cardPinRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	public LoginResponse login(LoginRequest request) {

		// 1. Find customer by email
		Customer customer = customerRepository
				.findByEmailAndDeletedFalse(request.getEmail().toLowerCase().trim())
				.orElseThrow(() -> new BusinessException("Invalid email or password"));

		// 2. Validate password
		if (customer.getPasswordHash() == null
				|| !passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
			throw new BusinessException("Invalid email or password");
		}

		// 3. Generate JWT (subject = email)
		String token = jwtUtil.generateToken(customer.getEmail());

		// 4. Fetch all applications
		List<CreditCardApplication> applications =
				applicationRepository.findByCustomerIdAndDeletedFalse(customer.getId());


		return LoginResponse.builder()
				.token(token)
				.build();
	}

	private String maskCardNumber(String cardNumber) {
		return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
	}
}