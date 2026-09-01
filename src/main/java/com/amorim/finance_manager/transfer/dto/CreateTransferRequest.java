package com.amorim.finance_manager.transfer.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransferRequest(

        @NotNull(message = "Conta de origem é obrigatória")
        UUID sourceAccountId,

        @NotNull(message = "Conta de destino é obrigatória")
        UUID destinationAccountId,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Valor deve possuir no máximo 17 dígitos inteiros e 2 decimais"
        )
        BigDecimal amount,

        @NotNull(message = "Data é obrigatória")
        LocalDate date,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 255, message = "Descrição deve possuir no máximo 255 caracteres")
        String description
) {
    public CreateTransferRequest {
        if (description != null) {
            description = description.trim();
        }
    }
}
