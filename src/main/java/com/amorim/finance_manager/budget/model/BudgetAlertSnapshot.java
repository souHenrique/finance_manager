package com.amorim.finance_manager.budget.model;

import java.math.BigDecimal;

public record BudgetAlertSnapshot(
        BigDecimal spentAmount,
        BigDecimal usagePercentage,
        BudgetAlertStatus alertStatus
) {
}
