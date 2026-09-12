package com.amorim.finance_manager.invoice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CloseInvoiceRequest(
        @NotNull
        @PositiveOrZero
        Long expectedVersion
) {
}
