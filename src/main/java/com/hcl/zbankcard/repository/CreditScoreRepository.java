package com.hcl.zbankcard.repository;

import com.hcl.zbankcard.entity.CreditScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditScoreRepository extends JpaRepository<CreditScore, UUID> {

    Optional<CreditScore> findByApplicationIdAndDeletedFalse(UUID applicationId);

    List<CreditScore> findByCustomerIdAndDeletedFalse(UUID customerId);

    Optional<CreditScore> findByIdAndDeletedFalse(UUID id);
}
