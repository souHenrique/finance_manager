package com.amorim.finance_manager.creditcard.dto;

import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCreditCardRequest(

        @Size(max = 120)
        String name,

        @DecimalMin(
                value = "0.01",
                message = "Limite de crédito deve ser maior que zero"
        )
        @Digits(integer = 17, fraction = 2)
        BigDecimal creditLimit,

        @Min(1)
        @Max(31)
        Integer closingDay,

        @Min(1)
        @Max(31)
        Integer dueDay,

        UUID defaultAccountId,

        CreditCardStatus status
) {
    public UpdateCreditCardRequest {
        name = name == null ? null : name.trim();
    }
}
