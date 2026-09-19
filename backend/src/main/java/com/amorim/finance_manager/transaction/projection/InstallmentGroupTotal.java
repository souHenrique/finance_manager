package com.amorim.finance_manager.transaction.projection;

import java.math.BigDecimal;
import java.util.UUID;

public record InstallmentGroupTotal(
        UUID installmentGroupId,
        BigDecimal totalAmount
) {
}
