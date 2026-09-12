package com.amorim.finance_manager.shared.exception;

public class InvalidCreditCardRefundException extends RuntimeException {
    public InvalidCreditCardRefundException(String message) {
        super(message);
    }
}
