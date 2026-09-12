package com.amorim.finance_manager.shared.exception;

public class InvalidBudgetUpdateException extends RuntimeException {
    public InvalidBudgetUpdateException(String message) {
        super(message);
    }
}
