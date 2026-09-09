package com.amorim.finance_manager.report.repository;

import com.amorim.finance_manager.report.projection.CompetenceAggregate;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CompetenceReportRepository extends Repository<Transaction, UUID> {

    @Query("""
            select new com.amorim.finance_manager.report.projection.CompetenceAggregate(
                transaction.type,
                transaction.categoryId,
                sum(transaction.amount)
            )
            from Transaction transaction
            where transaction.userId = :userId
              and transaction.status = :status
              and transaction.competenceDate >= :startDate
              and transaction.competenceDate <= :endDate
              and transaction.type in :includedTypes
            group by transaction.type, transaction.categoryId
            """)
    List<CompetenceAggregate> aggregate(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") TransactionStatus status,
            @Param("includedTypes") Collection<TransactionType> includedTypes
    );
}
