package com.amorim.finance_manager.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoicePaymentResponse(
        UUID invoiceId,
        BigDecimal totalAmount,
        BigDecimal creditAppliedAmount,
        BigDecimal cashPaidAmount,
        UUID paymentTransactionId,
        Instant paidAt
) {
}
