package com.hcl.zbankcard.service.impl;

import com.hcl.zbankcard.dto.request.CreditCardApplicationRequest;
import com.hcl.zbankcard.dto.request.CustomerInfoRequest;
import com.hcl.zbankcard.dto.request.DocumentInfoRequest;
import com.hcl.zbankcard.dto.request.EmploymentInfoRequest;
import com.hcl.zbankcard.dto.response.CreditCardApplicationResponse;
import com.hcl.zbankcard.entity.*;
import com.hcl.zbankcard.entity.enums.*;
import com.hcl.zbankcard.exception.DuplicateCustomerException;
import com.hcl.zbankcard.repository.*;
import com.hcl.zbankcard.service.CardAllocationService;
import com.hcl.zbankcard.service.CardIssuanceResult;
import com.hcl.zbankcard.service.CreditRatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditCardApplicationServiceImpl")
class CreditCardApplicationServiceImplTest {

    @Mock private CustomerRepository              customerRepository;
    @Mock private CreditCardApplicationRepository applicationRepository;
    @Mock private CreditScoreRepository           creditScoreRepository;
    @Mock private EmploymentRepository            employmentRepository;
    @Mock private DocumentRepository              documentRepository;
    @Mock private CreditRatingService             creditRatingService;
    @Mock private CardAllocationService           cardAllocationService;

    @InjectMocks private CreditCardApplicationServiceImpl service;

    private CreditCardApplicationRequest request;
    private Customer savedCustomer;
    private Employment savedEmployment;
    private CreditCardApplication savedApplication;

    @BeforeEach
    void setUp() {
        request = buildRequest();
        savedCustomer = buildCustomer();
        savedEmployment = buildEmployment(savedCustomer);
        savedApplication = buildApplication(savedCustomer);

        // Default: no duplicate
        given(customerRepository.existsByEmail(any())).willReturn(false);
        given(customerRepository.existsByPhone(any())).willReturn(false);

        // Persist stubs
        given(customerRepository.save(any(Customer.class))).willReturn(savedCustomer);
        given(employmentRepository.save(any(Employment.class))).willReturn(savedEmployment);
        given(documentRepository.save(any(Document.class))).willAnswer(inv -> inv.getArgument(0));
        given(applicationRepository.save(any(CreditCardApplication.class))).willReturn(savedApplication);
    }

    // ── Happy path – APPROVED ───────────────────────────────────────────────────

    @Test
    @DisplayName("apply: should return APPROVED response with card details when score = 500")
    void apply_score500_returnsApprovedWithCardDetails() {
        CreditScore score = buildCreditScore(500, savedCustomer, savedApplication);
        given(creditScoreRepository.save(any())).willReturn(score);
        given(creditRatingService.evaluate(any(), any())).willReturn(score);

        CreditCard card = buildCreditCard(CardType.PLATINUM, new BigDecimal("40000"), savedCustomer, savedApplication);
        CardPin pin = new CardPin();
        CardIssuanceResult issuance = new CardIssuanceResult(card, pin, "7391");
        given(cardAllocationService.allocate(any(), any(), any(int.class))).willReturn(Optional.of(issuance));
        given(applicationRepository.save(any())).willAnswer(inv -> {
            CreditCardApplication a = inv.getArgument(0);
            return a;
        });

        CreditCardApplicationResponse response = service.apply(request);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(response.isAdditionalDocumentsRequired()).isFalse();
        assertThat(response.getCardDetails()).isNotNull();
        assertThat(response.getCardDetails().getCardType()).isEqualTo(CardType.PLATINUM);
        assertThat(response.getCardDetails().getFirstTimePin()).isEqualTo("7391");
    }

    // ── Happy path – UNDER_REVIEW (score 50) ────────────────────────────────────

    @Test
    @DisplayName("apply: should return UNDER_REVIEW response with no card when score = 50")
    void apply_score50_returnsUnderReviewWithAdditionalDocsFlag() {
        CreditScore score = buildCreditScore(50, savedCustomer, savedApplication);
        given(creditScoreRepository.save(any())).willReturn(score);
        given(creditRatingService.evaluate(any(), any())).willReturn(score);
        given(cardAllocationService.allocate(any(), any(), any(int.class))).willReturn(Optional.empty());
        given(applicationRepository.save(any())).willAnswer(inv -> {
            CreditCardApplication a = inv.getArgument(0);
            return a;
        });

        CreditCardApplicationResponse response = service.apply(request);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(response.isAdditionalDocumentsRequired()).isTrue();
        assertThat(response.getCardDetails()).isNull();
    }

    // ── Duplicate validation ─────────────────────────────────────────────────────

    @Test
    @DisplayName("apply: should throw DuplicateCustomerException when email already exists")
    void apply_duplicateEmail_throwsDuplicateCustomerException() {
        given(customerRepository.existsByEmail(any())).willReturn(true);

        assertThatThrownBy(() -> service.apply(request))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("apply: should throw DuplicateCustomerException when phone already exists")
    void apply_duplicatePhone_throwsDuplicateCustomerException() {
        given(customerRepository.existsByEmail(any())).willReturn(false);
        given(customerRepository.existsByPhone(any())).willReturn(true);

        assertThatThrownBy(() -> service.apply(request))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("phone");
    }

    // ── Response structure ───────────────────────────────────────────────────────

    @Test
    @DisplayName("apply: response should include customer summary and credit rating")
    void apply_responseContainsCustomerAndCreditRatingSummary() {
        CreditScore score = buildCreditScore(150, savedCustomer, savedApplication);
        given(creditScoreRepository.save(any())).willReturn(score);
        given(creditRatingService.evaluate(any(), any())).willReturn(score);

        CreditCard card = buildCreditCard(CardType.VISA, new BigDecimal("10000"), savedCustomer, savedApplication);
        CardIssuanceResult issuance = new CardIssuanceResult(card, new CardPin(), "5678");
        given(cardAllocationService.allocate(any(), any(), any(int.class))).willReturn(Optional.of(issuance));
        given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        CreditCardApplicationResponse response = service.apply(request);

        assertThat(response.getCustomer()).isNotNull();
        assertThat(response.getCustomer().getEmail()).isEqualTo(savedCustomer.getEmail());
        assertThat(response.getCreditRating()).isNotNull();
        assertThat(response.getCreditRating().getScore()).isEqualTo(150);
        assertThat(response.getApplicationNumber()).isNotBlank();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private CreditCardApplicationRequest buildRequest() {
        CustomerInfoRequest cust = new CustomerInfoRequest();
        cust.setName("Jane Doe");
        cust.setEmail("jane@example.com");
        cust.setPhone("9876543210");
        cust.setDob(LocalDate.of(1990, 1, 1));
        cust.setAddress("123 Main Street, City");

        EmploymentInfoRequest emp = new EmploymentInfoRequest();
        emp.setEmployerName("ACME Corp");
        emp.setEmploymentType(EmploymentType.SALARIED);
        emp.setSalary(new BigDecimal("250000"));
        emp.setDesignation("Engineer");

        DocumentInfoRequest doc = new DocumentInfoRequest();
        doc.setDocType(DocumentType.AADHAAR);
        doc.setDocNumber("1234-5678-9012");

        CreditCardApplicationRequest req = new CreditCardApplicationRequest();
        req.setCustomerInfo(cust);
        req.setEmploymentInfo(emp);
        req.setDocumentInfo(doc);
        return req;
    }

    private Customer buildCustomer() {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName("Jane Doe");
        c.setEmail("jane@example.com");
        c.setPhone("9876543210");
        return c;
    }

    private Employment buildEmployment(Customer customer) {
        Employment e = new Employment();
        e.setId(UUID.randomUUID());
        e.setCustomer(customer);
        e.setSalary(new BigDecimal("250000"));
        e.setEmploymentType(EmploymentType.SALARIED);
        return e;
    }

    private CreditCardApplication buildApplication(Customer customer) {
        CreditCardApplication app = new CreditCardApplication();
        app.setId(UUID.randomUUID());
        app.setApplicationNumber("ZBNK-20260418-000001");
        app.setCustomer(customer);
        app.setStatus(ApplicationStatus.PENDING);
        app.setAppliedAt(LocalDateTime.now());
        return app;
    }

    private CreditScore buildCreditScore(int score, Customer customer, CreditCardApplication application) {
        CreditScore cs = new CreditScore();
        cs.setId(UUID.randomUUID());
        cs.setScore(score);
        cs.setScoreSource("Z-Bank Internal (Calculated)");
        cs.setCardCount(0);
        cs.setCustomer(customer);
        cs.setApplication(application);
        return cs;
    }

    private CreditCard buildCreditCard(CardType cardType, BigDecimal limit,
            Customer customer, CreditCardApplication application) {
        CreditCard c = new CreditCard();
        c.setId(UUID.randomUUID());
        c.setCardNumber("4111111111111111");
        c.setCardType(cardType);
        c.setCreditLimit(limit);
        c.setStatus(CardStatus.ACTIVE);
        c.setIssuedAt(LocalDateTime.now());
        c.setCustomer(customer);
        c.setApplication(application);
        return c;
    }
}
