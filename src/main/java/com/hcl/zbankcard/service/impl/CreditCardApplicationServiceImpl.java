package com.hcl.zbankcard.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcl.zbankcard.dto.request.CreditCardApplicationRequest;
import com.hcl.zbankcard.dto.response.ApplicationStatusResponse;
import com.hcl.zbankcard.dto.response.CreditCardApplicationResponse;
import com.hcl.zbankcard.entity.CreditCard;
import com.hcl.zbankcard.entity.CreditCardApplication;
import com.hcl.zbankcard.entity.CreditScore;
import com.hcl.zbankcard.entity.Customer;
import com.hcl.zbankcard.entity.Document;
import com.hcl.zbankcard.entity.Employment;
import com.hcl.zbankcard.entity.enums.ApplicationStatus;
import com.hcl.zbankcard.exception.DuplicateCustomerException;
import com.hcl.zbankcard.exception.ResourceNotFoundException;
import com.hcl.zbankcard.repository.CreditCardApplicationRepository;
import com.hcl.zbankcard.repository.CreditCardRepository;
import com.hcl.zbankcard.repository.CreditScoreRepository;
import com.hcl.zbankcard.repository.CustomerRepository;
import com.hcl.zbankcard.repository.DocumentRepository;
import com.hcl.zbankcard.repository.EmploymentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hcl.zbankcard.service.CardAllocationService;
import com.hcl.zbankcard.service.CardIssuanceResult;
import com.hcl.zbankcard.service.CreditCardApplicationService;
import com.hcl.zbankcard.service.CreditRatingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the full credit card application flow:
 *
 * 1. Validate uniqueness (email + phone) 2. Persist Customer, Employment,
 * Document 3. Create Application (PENDING) 4. Evaluate credit score (historical
 * first, then calculated) 5. Allocate card tier based on score, generate card
 * number + first-time PIN 6. Finalise application status → APPROVED (card
 * issued) or UNDER_REVIEW (docs needed)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardApplicationServiceImpl implements CreditCardApplicationService {

	private final CustomerRepository customerRepository;
	private final CreditCardApplicationRepository applicationRepository;
	private final CreditScoreRepository creditScoreRepository;
	private final CreditCardRepository creditCardRepository;
	private final EmploymentRepository employmentRepository;
	private final DocumentRepository documentRepository;
	private final CreditRatingService creditRatingService;
	private final CardAllocationService cardAllocationService;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public CreditCardApplicationResponse apply(CreditCardApplicationRequest request) {
		log.info("Processing credit card application for email: {}", request.getCustomerInfo().getEmail());

		// Duplicate email and phone check
		validateNoDuplicateCustomer(request);

		// Store customer data
		Customer customer = persistCustomer(request);
		Employment employment = persistEmployment(request, customer);
		persistDocument(request, customer);

		// Set in-memory reference so credit rating can access salary within same
		// transaction
		customer.setEmployment(employment);

		// Step 3 – Create application in PENDING
		CreditCardApplication application = persistApplication(customer);

		// Step 4 – Evaluate credit score
		CreditScore creditScore = creditRatingService.evaluate(customer, application);
		creditScore = creditScoreRepository.save(creditScore);
		log.info("Credit score {} saved for application {}", creditScore.getScore(),
				application.getApplicationNumber());

		// Step 5 – Allocate card or flag for additional docs
		Optional<CardIssuanceResult> issuance = cardAllocationService.allocate(customer, application,
				creditScore.getScore());

		// Step 6 – Finalise application status
		application.setStatus(issuance.isPresent() ? ApplicationStatus.APPROVED : ApplicationStatus.UNDER_REVIEW);
		application = applicationRepository.save(application);

		log.info("Application {} finalised: status={}, score={}", application.getApplicationNumber(),
				application.getStatus(), creditScore.getScore());

		return buildResponse(application, customer, creditScore, issuance.orElse(null));
	}

	// ── private helpers ──────────────────────────────────────────────────────────

	private void validateNoDuplicateCustomer(CreditCardApplicationRequest req) {
		if (customerRepository.existsByEmail(req.getCustomerInfo().getEmail().toLowerCase().trim())) {
			throw new DuplicateCustomerException(
					"An account with email '" + req.getCustomerInfo().getEmail() + "' already exists.");
		}
		if (customerRepository.existsByPhone(req.getCustomerInfo().getPhone().trim())) {
			throw new DuplicateCustomerException(
					"An account with phone '" + req.getCustomerInfo().getPhone() + "' already exists.");
		}
	}

	private Customer persistCustomer(CreditCardApplicationRequest req) {
		var info = req.getCustomerInfo();
		String hashedPassword = passwordEncoder.encode(info.getPassword());
		return customerRepository
				.save(Customer.builder().name(info.getName().trim()).email(info.getEmail().toLowerCase().trim())
						.phone(info.getPhone().trim()).dob(info.getDob()).address(info.getAddress().trim())
						.passwordHash(hashedPassword).build());
	}

	private Employment persistEmployment(CreditCardApplicationRequest req, Customer customer) {
		var emp = req.getEmploymentInfo();
		return employmentRepository.save(Employment.builder().customer(customer)
				.employerName(emp.getEmployerName().trim()).employmentType(emp.getEmploymentType())
				.salary(emp.getSalary()).designation(emp.getDesignation().trim()).build());
	}

	private void persistDocument(CreditCardApplicationRequest req, Customer customer) {
		var doc = req.getDocumentInfo();
		documentRepository.save(Document.builder().customer(customer).docType(doc.getDocType())
				.docNumber(doc.getDocNumber().trim().toUpperCase()).expiryDate(doc.getExpiryDate()).build());
	}

	private CreditCardApplication persistApplication(Customer customer) {
		return applicationRepository
				.save(CreditCardApplication.builder().customer(customer).applicationNumber(generateApplicationNumber())
						.status(ApplicationStatus.PENDING).appliedAt(LocalDateTime.now()).build());
	}

	private String generateApplicationNumber() {
		String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String random = String.format("%06d", ThreadLocalRandom.current().nextInt(1, 1_000_000));
		return "ZBNK-" + date + "-" + random;
	}

	private CreditCardApplicationResponse buildResponse(CreditCardApplication application, Customer customer,
			CreditScore creditScore, CardIssuanceResult issuance) {

		var builder = CreditCardApplicationResponse.builder().applicationId(application.getId())
				.applicationNumber(application.getApplicationNumber()).status(application.getStatus())
				.appliedAt(application.getAppliedAt()).additionalDocumentsRequired(issuance == null)
				.customer(CreditCardApplicationResponse.CustomerSummary.builder().customerId(customer.getId())
						.name(customer.getName()).email(customer.getEmail()).phone(customer.getPhone()).build())
				.creditRating(CreditCardApplicationResponse.CreditRatingSummary.builder().score(creditScore.getScore())
						.scoreSource(creditScore.getScoreSource())
						.message(resolveStatusMessage(application.getStatus())).build());

		if (issuance != null) {
			CreditCard card = issuance.creditCard();
			builder.cardDetails(CreditCardApplicationResponse.CardIssuanceDetails.builder()
					.maskedCardNumber(maskCardNumber(card.getCardNumber())).cardType(card.getCardType())
					.creditLimit(card.getCreditLimit()).cardStatus(card.getStatus()).issuedAt(card.getIssuedAt())
					.firstTimePin(issuance.plainPin()).build());
		}

		return builder.build();
	}

	private String maskCardNumber(String cardNumber) {
		return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
	}

	private String resolveStatusMessage(ApplicationStatus status) {
		return switch (status) {
		case APPROVED -> "Congratulations! Your credit card application has been approved.";
		case UNDER_REVIEW -> "Additional documents are required. Our team will contact you within 2-3 business days.";
		case REJECTED -> "We are unable to approve your application at this time.";
		default -> "Your application has been received and is being processed.";
		};
	}

	@Override
	@Transactional(readOnly = true)
	public ApplicationStatusResponse getStatus(String applicationNumber) {
		CreditCardApplication application = applicationRepository
				.findByApplicationNumberAndDeletedFalse(applicationNumber)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Application not found: " + applicationNumber));

		Integer score = creditScoreRepository
				.findByApplicationIdAndDeletedFalse(application.getId())
				.map(cs -> cs.getScore())
				.orElse(null);

		Optional<CreditCard> cardOpt = creditCardRepository
				.findByApplicationIdAndDeletedFalse(application.getId());

		ApplicationStatusResponse.ApplicationStatusResponseBuilder builder = ApplicationStatusResponse.builder()
				.applicationId(application.getId())
				.applicationNumber(application.getApplicationNumber())
				.status(application.getStatus())
				.appliedAt(application.getAppliedAt())
				.additionalDocumentsRequired(application.getStatus() == ApplicationStatus.UNDER_REVIEW)
				.creditScore(score);

		cardOpt.ifPresent(card -> builder.cardDetails(
				ApplicationStatusResponse.CardDetails.builder()
						.maskedCardNumber(maskCardNumber(card.getCardNumber()))
						.cardType(card.getCardType())
						.creditLimit(card.getCreditLimit())
						.cardStatus(card.getStatus())
						.issuedAt(card.getIssuedAt())
						.build()));

		return builder.build();
	}
}
