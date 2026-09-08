package com.amorim.finance_manager.invoice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PayInvoiceRequest(
        UUID sourceAccountId,

        @NotNull
        Long expectedVersion
) {
}
