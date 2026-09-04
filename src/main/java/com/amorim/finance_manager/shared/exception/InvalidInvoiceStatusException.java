package com.amorim.finance_manager.shared.exception;

public class InvalidInvoiceStatusException extends RuntimeException {
    public InvalidInvoiceStatusException() {
        super("Apenas faturas abertas podem receber novas compras");
    }
}
