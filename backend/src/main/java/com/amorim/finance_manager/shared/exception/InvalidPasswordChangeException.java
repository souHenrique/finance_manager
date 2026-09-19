package com.amorim.finance_manager.shared.exception;

public class InvalidPasswordChangeException extends RuntimeException {

    public InvalidPasswordChangeException(String message) {
        super(message);
    }
}
