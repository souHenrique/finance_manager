package com.amorim.finance_manager.transaction.dto;

import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String description,
        BigDecimal amount,
        LocalDate competenceDate,
        LocalDate effectiveDate,
        LocalDate dueDate,
        TransactionType type,
        TransactionStatus status,
        PaymentMethod paymentMethod,
        UUID sourceAccountId,
        UUID destinationAccountId,
        UUID categoryId,
        UUID creditCardId,
        UUID invoiceId,
        UUID installmentGroupId,
        Integer installmentNumber,
        Integer installmentCount,
        Instant createdAt,
        Instant updatedAt

) {
}
