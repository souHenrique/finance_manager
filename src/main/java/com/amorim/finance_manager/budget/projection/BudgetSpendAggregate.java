package com.amorim.finance_manager.budget.projection;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetSpendAggregate(
        UUID categoryId,
        BigDecimal spentAmount
) {
}
