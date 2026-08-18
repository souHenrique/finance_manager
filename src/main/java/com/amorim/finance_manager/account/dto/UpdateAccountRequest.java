package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountRequest(
        @NotBlank String name,
        @NotNull AccountType type,
        String bankName,
        String color,
        String icon
) {
}
