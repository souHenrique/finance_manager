package com.amorim.finance_manager.shared.exception;

public class InvalidTransactionStatusException extends RuntimeException {
    public InvalidTransactionStatusException(String message) {
        super(message);
    }
}
