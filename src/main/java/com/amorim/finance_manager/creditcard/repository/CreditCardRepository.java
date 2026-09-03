package com.amorim.finance_manager.creditcard.repository;

import com.amorim.finance_manager.creditcard.entity.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {

    List<CreditCard> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<CreditCard> findByIdAndUserId(UUID id, UUID userId);
}
