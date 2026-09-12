package com.amorim.finance_manager.shared.exception;

public class InvalidBalanceAmountException extends RuntimeException {
    public InvalidBalanceAmountException(String message) {
        super(message);
    }
}
