package com.amorim.finance_manager.creditcard.dto;

import com.amorim.finance_manager.creditcard.entity.CreditCardRefundTreatment;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreditCardRefundResponse(
        UUID id,
        UUID creditCardId,
        UUID selectedTransactionId,
        UUID installmentGroupId,
        String reason,
        BigDecimal totalAmount,
        BigDecimal limitRestoredAmount,
        BigDecimal paidCompensationAmount,
        Instant createdAt,
        List<Item> items
) {

    public record Item(
            UUID id,
            UUID originalTransactionId,
            UUID originalInvoiceId,
            InvoiceStatus originalInvoiceStatus,
            BigDecimal amount,
            CreditCardRefundTreatment treatment,
            UUID creditId
    ) {
    }
}
