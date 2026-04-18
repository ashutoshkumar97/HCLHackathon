package com.hcl.zbankcard.repository;

import com.hcl.zbankcard.entity.Employment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmploymentRepository extends JpaRepository<Employment, UUID> {

    Optional<Employment> findByCustomerIdAndDeletedFalse(UUID customerId);

    Optional<Employment> findByIdAndDeletedFalse(UUID id);
}
