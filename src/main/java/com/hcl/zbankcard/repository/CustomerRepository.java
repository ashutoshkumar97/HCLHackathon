package com.hcl.zbankcard.repository;

import com.hcl.zbankcard.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmailAndDeletedFalse(String email);

    Optional<Customer> findByPhoneAndDeletedFalse(String phone);

    Optional<Customer> findByIdAndDeletedFalse(UUID id);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
