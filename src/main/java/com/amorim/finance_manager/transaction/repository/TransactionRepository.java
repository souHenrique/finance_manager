package com.amorim.finance_manager.transaction.repository;

import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    List<Transaction> findAllByInvoiceIdAndUserIdOrderByCompetenceDateAscCreatedAtAsc(UUID invoiceId, UUID userId);

    List<Transaction> findAllByInstallmentGroupIdAndCreditCardIdAndUserIdOrderByInstallmentNumberAsc(
            UUID installmentGroupId,
            UUID creditCardId,
            UUID userId
    );

    boolean existsByInvoiceIdAndUserIdAndTypeAndStatus(
            UUID invoiceId,
            UUID userId,
            TransactionType type,
            TransactionStatus status
    );
}
