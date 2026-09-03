package com.amorim.finance_manager.report.projection;

import com.amorim.finance_manager.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CashFlowAggregate(
        LocalDate effectiveDate,
        TransactionType type,
        UUID categoryId,
        BigDecimal amount
) {
}
