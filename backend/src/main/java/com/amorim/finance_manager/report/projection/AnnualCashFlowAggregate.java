package com.amorim.finance_manager.report.projection;

import com.amorim.finance_manager.transaction.entity.TransactionType;

import java.math.BigDecimal;

public record AnnualCashFlowAggregate(
        Integer month,
        TransactionType type,
        BigDecimal amount
) {
}
