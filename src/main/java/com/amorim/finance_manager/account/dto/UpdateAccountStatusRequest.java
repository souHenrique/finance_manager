package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "Status é obrigatório")
        AccountStatus status
) {
}
