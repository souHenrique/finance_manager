package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.entity.AccountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120)
        String name,

        @NotNull(message = "Tipo é obrigatório")
        AccountType type,

        @Size(max = 160)
        String institution,

        @NotNull(message = "Saldo inicial é obrigatório")
        @Digits(integer = 19, fraction = 2)
        BigDecimal initialBalance
) {
        public CreateAccountRequest {
                name = name == null ? null : name.trim();
                institution = institution == null ? null : institution.trim();
        }
}
