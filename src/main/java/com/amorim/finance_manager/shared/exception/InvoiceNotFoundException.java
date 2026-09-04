package com.amorim.finance_manager.shared.exception;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException() {
        super("Fatura não encontrada");
    }
}
