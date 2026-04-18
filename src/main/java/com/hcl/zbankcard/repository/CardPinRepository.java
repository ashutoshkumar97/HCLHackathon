package com.hcl.zbankcard.repository;

import com.hcl.zbankcard.entity.CardPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardPinRepository extends JpaRepository<CardPin, UUID> {

    Optional<CardPin> findByCreditCardIdAndDeletedFalse(UUID cardId);

    Optional<CardPin> findByIdAndDeletedFalse(UUID id);
    
    Optional<CardPin> findByCreditCard_CardNumber(String cardNumber);
}
