package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank String name,
        @NotNull AccountType type,
        String bankName,
        @NotNull BigDecimal initialBalance,
        String color,
        String icon
) {
}