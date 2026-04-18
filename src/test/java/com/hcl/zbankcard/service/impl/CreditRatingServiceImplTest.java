package com.hcl.zbankcard.service.impl;

import com.hcl.zbankcard.entity.*;
import com.hcl.zbankcard.repository.CreditCardRepository;
import com.hcl.zbankcard.repository.CreditScoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditRatingServiceImpl")
class CreditRatingServiceImplTest {

    @Mock private CreditScoreRepository creditScoreRepository;
    @Mock private CreditCardRepository  creditCardRepository;

    @InjectMocks private CreditRatingServiceImpl service;

    // ── evaluate() – historical score path ─────────────────────────────────────

    @Test
    @DisplayName("evaluate: should return historical score when one exists on record")
    void evaluate_whenHistoricalScoreExists_returnsExistingScore() {
        UUID customerId = UUID.randomUUID();
        Customer customer = buildCustomer(customerId, null);
        CreditCardApplication application = buildApplication(customer);

        CreditScore historical = new CreditScore();
        historical.setScore(500);
        historical.setCardCount(0);

        given(creditScoreRepository.findTopByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(customerId))
                .willReturn(Optional.of(historical));

        CreditScore result = service.evaluate(customer, application);

        assertThat(result.getScore()).isEqualTo(500);
        assertThat(result.getScoreSource()).contains("Historical");
        verifyNoInteractions(creditCardRepository);
    }

    // ── evaluate() – calculated score path ─────────────────────────────────────

    @Test
    @DisplayName("evaluate: should return score 300 when customer holds 2 or more cards")
    void evaluate_whenTwoOrMoreCards_returnsScore300() {
        UUID customerId = UUID.randomUUID();
        Customer customer = buildCustomer(customerId, employmentWith(new BigDecimal("250000")));
        CreditCardApplication application = buildApplication(customer);

        given(creditScoreRepository.findTopByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(customerId))
                .willReturn(Optional.empty());
        given(creditCardRepository.countByCustomerIdAndDeletedFalse(customerId)).willReturn(2L);

        CreditScore result = service.evaluate(customer, application);

        assertThat(result.getScore()).isEqualTo(CreditRatingServiceImpl.SCORE_MULTI_CARD);
        assertThat(result.getCardCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("evaluate: should return score 500 when salary is above $200,000 and fewer than 2 cards")
    void evaluate_whenSalaryAbove200k_returnsScore500() {
        UUID customerId = UUID.randomUUID();
        Customer customer = buildCustomer(customerId, employmentWith(new BigDecimal("200001")));
        CreditCardApplication application = buildApplication(customer);

        given(creditScoreRepository.findTopByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(customerId))
                .willReturn(Optional.empty());
        given(creditCardRepository.countByCustomerIdAndDeletedFalse(customerId)).willReturn(0L);

        CreditScore result = service.evaluate(customer, application);

        assertThat(result.getScore()).isEqualTo(CreditRatingServiceImpl.SCORE_HIGH_SALARY);
    }

    @Test
    @DisplayName("evaluate: should return score 150 when salary is between $50,001 and $200,000")
    void evaluate_whenSalaryMidRange_returnsScore150() {
        UUID customerId = UUID.randomUUID();
        Customer customer = buildCustomer(customerId, employmentWith(new BigDecimal("100000")));
        CreditCardApplication application = buildApplication(customer);

        given(creditScoreRepository.findTopByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(customerId))
                .willReturn(Optional.empty());
        given(creditCardRepository.countByCustomerIdAndDeletedFalse(customerId)).willReturn(1L);

        CreditScore result = service.evaluate(customer, application);

        assertThat(result.getScore()).isEqualTo(CreditRatingServiceImpl.SCORE_MID_SALARY);
    }

    @Test
    @DisplayName("evaluate: should return score 50 when salary is $50,000 or below")
    void evaluate_whenSalaryAtOrBelow50k_returnsScore50() {
        UUID customerId = UUID.randomUUID();
        Customer customer = buildCustomer(customerId, employmentWith(new BigDecimal("50000")));
        CreditCardApplication application = buildApplication(customer);

        given(creditScoreRepository.findTopByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(customerId))
                .willReturn(Optional.empty());
        given(creditCardRepository.countByCustomerIdAndDeletedFalse(customerId)).willReturn(0L);

        CreditScore result = service.evaluate(customer, application);

        assertThat(result.getScore()).isEqualTo(CreditRatingServiceImpl.SCORE_LOW_SALARY);
    }

    @Test
    @DisplayName("evaluate: should return score 50 when no employment information is available")
    void evaluate_whenNoEmployment_returnsScore50() {
        UUID customerId = UUID.randomUUID();
        Customer customer = buildCustomer(customerId, null); // no employment
        CreditCardApplication application = buildApplication(customer);

        given(creditScoreRepository.findTopByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(customerId))
                .willReturn(Optional.empty());
        given(creditCardRepository.countByCustomerIdAndDeletedFalse(customerId)).willReturn(0L);

        CreditScore result = service.evaluate(customer, application);

        assertThat(result.getScore()).isEqualTo(CreditRatingServiceImpl.SCORE_LOW_SALARY);
    }

    // ── computeScore() unit tests (pure logic) ──────────────────────────────────

    @Test
    @DisplayName("computeScore: card count >= 2 overrides salary, returning 300")
    void computeScore_cardCountOverridesSalary() {
        Employment highSalaryEmp = employmentWith(new BigDecimal("500000"));
        assertThat(service.computeScore(2, highSalaryEmp)).isEqualTo(300);
        assertThat(service.computeScore(3, highSalaryEmp)).isEqualTo(300);
    }

    @Test
    @DisplayName("computeScore: salary exactly $200,000 falls into mid-range band (score 150)")
    void computeScore_salaryExactly200k_returns150() {
        assertThat(service.computeScore(0, employmentWith(new BigDecimal("200000")))).isEqualTo(150);
    }

    @Test
    @DisplayName("computeScore: salary exactly $50,000 falls into low band (score 50)")
    void computeScore_salaryExactly50k_returns50() {
        assertThat(service.computeScore(0, employmentWith(new BigDecimal("50000")))).isEqualTo(50);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private Customer buildCustomer(UUID id, Employment employment) {
        Customer c = new Customer();
        c.setId(id);
        c.setEmployment(employment);
        return c;
    }

    private CreditCardApplication buildApplication(Customer customer) {
        CreditCardApplication app = new CreditCardApplication();
        app.setId(UUID.randomUUID());
        app.setApplicationNumber("TEST-APP-001");
        app.setCustomer(customer);
        return app;
    }

    private Employment employmentWith(BigDecimal salary) {
        Employment emp = new Employment();
        emp.setSalary(salary);
        return emp;
    }
}
