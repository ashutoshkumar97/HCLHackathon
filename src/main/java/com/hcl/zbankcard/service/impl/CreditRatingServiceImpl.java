package com.hcl.zbankcard.service.impl;

import com.hcl.zbankcard.entity.CreditCardApplication;
import com.hcl.zbankcard.entity.CreditScore;
import com.hcl.zbankcard.entity.Customer;
import com.hcl.zbankcard.entity.Employment;
import com.hcl.zbankcard.repository.CreditCardRepository;
import com.hcl.zbankcard.repository.CreditScoreRepository;
import com.hcl.zbankcard.service.CreditRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Evaluates a customer's credit score using the following priority rules:
 *
 * 1. If a historical credit score exists for the customer → return it as-is. 2.
 * Otherwise calculate from card count and annual salary: - Existing cards >= 2
 * → score = 300 - Salary > $200,000 → score = 500 - $50,000 < salary <=
 * $200,000 → score = 150 - Salary <= $50,000 → score = 50
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditRatingServiceImpl implements CreditRatingService {

	static final int SCORE_HIGH_SALARY = 500;
	static final int SCORE_MULTI_CARD = 300;
	static final int SCORE_MID_SALARY = 150;
	static final int SCORE_LOW_SALARY = 50;

	static final BigDecimal SALARY_HIGH = new BigDecimal("200000");
	static final BigDecimal SALARY_MEDIUM = new BigDecimal("50000");

	private static final String SOURCE_HISTORICAL = "Z-Bank Internal (Historical Score)";
	private static final String SOURCE_CALCULATED = "Z-Bank Internal (Calculated)";

	private final CreditScoreRepository creditScoreRepository;
	private final CreditCardRepository creditCardRepository;

	@Override
	public CreditScore evaluate(Customer customer, CreditCardApplication application) {
		log.info("Evaluating credit score for customer: {}", customer.getId());

		Optional<CreditScore> existing= creditScoreRepository.findTopByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(customer.getId());
					if(existing.isPresent()) {
					CreditScore score = existing.get();
					log.info("Historical credit score {} found for customer {}", score.getScore(), customer.getId());
					return CreditScore.builder().customer(customer).application(application).score(score.getScore())
							.scoreSource(SOURCE_HISTORICAL).cardCount(score.getCardCount()).build();
					}else {
						return calculateNewScore(customer, application);
					}
	}

	private CreditScore calculateNewScore(Customer customer, CreditCardApplication application) {
		int existingCardCount = (int) creditCardRepository.countByCustomerIdAndDeletedFalse(customer.getId());
		int score = computeScore(existingCardCount, customer.getEmployment());

		log.info("Calculated score {} for customer {} (cardCount={})", score, customer.getId(), existingCardCount);

		return CreditScore.builder().customer(customer).application(application).score(score)
				.scoreSource(SOURCE_CALCULATED).cardCount(existingCardCount).build();
	}

	/**
	 * Pure scoring logic — package-private for unit testing. Card count takes
	 * priority over salary rules.
	 */
	int computeScore(int cardCount, Employment employment) {
		if (cardCount >= 2) {
			return SCORE_MULTI_CARD; // 300 – too many existing cards
		}

		BigDecimal salary = Optional.ofNullable(employment).map(Employment::getSalary).orElse(BigDecimal.ZERO);

		if (salary.compareTo(SALARY_HIGH) > 0)
			return SCORE_HIGH_SALARY; // > 200,000
		if (salary.compareTo(SALARY_MEDIUM) > 0)
			return SCORE_MID_SALARY; // 50,001 – 200,000
		return SCORE_LOW_SALARY; // <= 50,000
	}
}
