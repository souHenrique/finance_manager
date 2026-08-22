package com.amorim.finance_manager.shared.exception;

public class InvalidProfileUpdateException extends RuntimeException {
    public InvalidProfileUpdateException(String message) {
        super(message);
    }
}
