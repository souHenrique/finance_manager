package com.amorim.finance_manager.transaction.repository;

import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
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

    @Query("""
        SELECT COALESCE(SUM(transaction.amount), 0)
        FROM Transaction transaction
        WHERE transaction.userId = :userId
            AND transaction.categoryId = :categoryId
            AND transaction.competenceDate >= :periodStart
            AND transaction.competenceDate < :periodEndExclusive
            AND transaction.type IN :includedTypes
            AND transaction.status <> :excludedStatus
        """)
    BigDecimal sumForBudget(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEndExclusive") LocalDate periodEndExclusive,
            @Param("includedTypes") Collection<TransactionType> includedTypes,
            @Param("excludedStatus") TransactionStatus excludedStatus
    );
}
