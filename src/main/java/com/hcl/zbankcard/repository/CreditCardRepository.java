package com.hcl.zbankcard.repository;

import com.hcl.zbankcard.entity.CreditCard;
import com.hcl.zbankcard.entity.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {

    Optional<CreditCard> findByApplicationIdAndDeletedFalse(UUID applicationId);

    List<CreditCard> findByCustomerIdAndDeletedFalse(UUID customerId);

    List<CreditCard> findByCustomerIdAndStatusAndDeletedFalse(UUID customerId, CardStatus status);

    Optional<CreditCard> findByIdAndDeletedFalse(UUID id);

    boolean existsByCardNumber(String cardNumber);

    Optional<CreditCard> findByCardNumberAndDeletedFalse(String cardNumber);

    long countByCustomerIdAndDeletedFalse(UUID customerId);
}
