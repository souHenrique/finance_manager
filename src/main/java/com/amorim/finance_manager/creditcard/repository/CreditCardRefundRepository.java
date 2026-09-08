package com.amorim.finance_manager.creditcard.repository;

import com.amorim.finance_manager.creditcard.entity.CreditCardRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditCardRefundRepository extends JpaRepository<CreditCardRefund, UUID> {

    Optional<CreditCardRefund> findByIdAndUserId(UUID id, UUID userId);
}
