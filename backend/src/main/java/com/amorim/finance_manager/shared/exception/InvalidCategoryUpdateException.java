package com.amorim.finance_manager.shared.exception;

public class InvalidCategoryUpdateException extends RuntimeException {
    public InvalidCategoryUpdateException(String message) {
        super(message);
    }
}
