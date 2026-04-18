package com.hcl.zbankcard.repository;

import com.hcl.zbankcard.entity.CreditCardApplication;
import com.hcl.zbankcard.entity.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditCardApplicationRepository extends JpaRepository<CreditCardApplication, UUID> {

    Optional<CreditCardApplication> findByApplicationNumberAndDeletedFalse(String applicationNumber);

    List<CreditCardApplication> findByCustomerIdAndDeletedFalse(UUID customerId);

    List<CreditCardApplication> findByCustomerIdAndStatusAndDeletedFalse(UUID customerId, ApplicationStatus status);

    Optional<CreditCardApplication> findByIdAndDeletedFalse(UUID id);

    boolean existsByApplicationNumber(String applicationNumber);
}
