package com.amorim.finance_manager.shared.exception;

public class InvalidInvoicePaymentException extends RuntimeException {
    public InvalidInvoicePaymentException(String message) {
        super(message);
    }
}
