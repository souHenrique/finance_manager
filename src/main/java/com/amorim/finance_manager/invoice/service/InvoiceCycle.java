package com.amorim.finance_manager.invoice.service;

import java.time.LocalDate;

public record InvoiceCycle(
        Integer referenceMonth,
        Integer referenceYear,
        LocalDate closingDate,
        LocalDate dueDate
) {
}
