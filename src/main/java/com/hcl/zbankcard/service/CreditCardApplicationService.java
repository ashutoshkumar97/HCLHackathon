package com.hcl.zbankcard.service;

import com.hcl.zbankcard.dto.request.CreditCardApplicationRequest;
import com.hcl.zbankcard.dto.response.CreditCardApplicationResponse;

public interface CreditCardApplicationService {

    /**
     * Processes a new credit card application end-to-end:
     * <ol>
     *   <li>Validates no duplicate customer (email / phone)</li>
     *   <li>Persists Customer, Employment, Document records</li>
     *   <li>Creates the {@link com.hcl.zbankcard.entity.CreditCardApplication}</li>
     *   <li>Fetches the applicant's credit rating</li>
     *   <li>Derives final application status from the credit score</li>
     * </ol>
     *
     * @param request the incoming application payload
     * @return response containing application details and credit rating summary
     */
    CreditCardApplicationResponse apply(CreditCardApplicationRequest request);
}
