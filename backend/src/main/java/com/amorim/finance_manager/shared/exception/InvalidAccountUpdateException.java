package com.amorim.finance_manager.shared.exception;

public class InvalidAccountUpdateException extends RuntimeException {
    public InvalidAccountUpdateException(String message) {
        super(message);
    }
}
