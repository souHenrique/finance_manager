package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String institution,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        AccountStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
