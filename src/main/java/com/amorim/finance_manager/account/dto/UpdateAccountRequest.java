package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(

        @Size(max = 120)
        String name,

        AccountType type,

        @Size(max = 160)
        String institution,

        AccountStatus status
) {
    public UpdateAccountRequest {
        name = name == null ? null : name.trim();
        institution = institution == null
                ? null
                : institution.trim();
    }
}
