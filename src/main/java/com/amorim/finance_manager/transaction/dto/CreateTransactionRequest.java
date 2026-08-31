package com.amorim.finance_manager.transaction.dto;

import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 255, message = "Descrição deve possuir no máximo 255 caracteres")
        String description,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(
                value = "0.01",
                message = "Valor deve ser maior que zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Valor deve possuir no máximo 17 dígitos inteiros e 2 decimais"
        )
        BigDecimal amount,

        @NotNull(message = "Data de competência é obrigatória")
        LocalDate competenceDate,

        LocalDate effectiveDate,

        LocalDate dueDate,

        @NotNull(message = "Tipo é obrigatório")
        TransactionType type,

        PaymentMethod paymentMethod,

        UUID sourceAccountId,

        UUID destinationAccountId,

        UUID categoryId,

        UUID creditCardId,

        UUID invoiceId,

        UUID installmentGroupId,

        @Positive(message = "Número da parcela deve ser maior que zero")
        Integer installmentNumber,

        @Positive(message = "Quantidade de parcelas deve ser maior que zero")
        Integer installmentCount

) {
    public CreateTransactionRequest {
        if (description != null) {
            description = description.trim();
        }
    }
}
