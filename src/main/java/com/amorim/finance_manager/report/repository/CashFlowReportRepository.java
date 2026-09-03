package com.amorim.finance_manager.report.repository;

import com.amorim.finance_manager.report.projection.CashFlowAggregate;
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

public interface CashFlowReportRepository extends Repository<Transaction, UUID> {

    @Query("""
            select new com.amorim.finance_manager.report.projection.CashFlowAggregate(
                t.effectiveDate,
                t.type,
                t.categoryId,
                sum(t.amount)
            )
            from Transaction t
            where t.userId = :userId
              and t.status = :status
              and t.effectiveDate is not null
              and t.effectiveDate >= :startDate
              and t.effectiveDate <= :endDate
              and t.type in :types
            group by t.effectiveDate, t.type, t.categoryId
            """)
    List<CashFlowAggregate> aggregate(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status")TransactionStatus status,
            @Param("types")Collection<TransactionType> types
    );
}
