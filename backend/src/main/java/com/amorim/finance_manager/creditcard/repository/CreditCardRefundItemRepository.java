package com.amorim.finance_manager.creditcard.repository;

import com.amorim.finance_manager.creditcard.entity.CreditCardRefundItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreditCardRefundItemRepository extends JpaRepository<CreditCardRefundItem, UUID> {

    List<CreditCardRefundItem> findAllByRefundIdOrderByOriginalTransactionIdAsc(UUID refundId);

    boolean existsByOriginalTransactionIdIn(List<UUID> originalTransactionIds);
}
