package com.amorim.finance_manager.creditcard.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCreditCardRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120)
        String name,

        @NotNull(message = "Limite de crédito é obrigatório")
        @DecimalMin(
                value = "0.01",
                message = "Limite de crédito deve ser maior que zero"
        )
        @Digits(integer = 17, fraction = 2)
        BigDecimal creditLimit,

        @NotNull(message = "Dia de fechamento é obrigatório")
        @Min(value = 1, message = "Dia de fechamento deve estar entre 1 e 31")
        @Max(value = 31, message = "Dia de fechamento deve estar entre 1 e 31")
        Integer closingDay,

        @NotNull(message = "Dia de vencimento é obrigatório")
        @Min(value = 1, message = "Dia de vencimento deve estar entre 1 e 31")
        @Max(value = 31, message = "Dia de vencimento deve estar entre 1 e 31")
        Integer dueDay,

        @NotNull(message = "Conta padrão é obrigatória")
        UUID defaultAccountId
) {
    public CreateCreditCardRequest {
        name = name == null ? null : name.trim();
    }
}
