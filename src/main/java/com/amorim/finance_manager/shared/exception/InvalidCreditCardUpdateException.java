package com.amorim.finance_manager.shared.exception;

public class InvalidCreditCardUpdateException extends RuntimeException {
    public InvalidCreditCardUpdateException(String message) {
        super(message);
    }
}
