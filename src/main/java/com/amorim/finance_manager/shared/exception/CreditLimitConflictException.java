package com.amorim.finance_manager.shared.exception;

public class CreditLimitConflictException extends RuntimeException {

    public CreditLimitConflictException() {
        super("O novo limite não pode ser menor que o limite já comprometido");
    }
    public CreditLimitConflictException(String message) {
        super(message);
    }
}
