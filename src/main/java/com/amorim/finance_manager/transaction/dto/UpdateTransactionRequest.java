package com.amorim.finance_manager.transaction.dto;

import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateTransactionRequest(

        @Size(min = 1, max = 255)
        String description,

        @DecimalMin("0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        LocalDate competenceDate,
        LocalDate effectiveDate,
        TransactionStatus status,
        PaymentMethod paymentMethod,
        UUID sourceAccountId,
        UUID destinationAccountId,
        UUID categoryId
) {
    public UpdateTransactionRequest {
        if (description != null) {
            description = description.trim();
        }
    }
}
