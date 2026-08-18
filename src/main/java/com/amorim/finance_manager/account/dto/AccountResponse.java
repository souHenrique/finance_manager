package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.enums.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String bankName,
        String currency,
        BigDecimal currentBalance,
        String color,
        String icon,
        Boolean active
) {
}
