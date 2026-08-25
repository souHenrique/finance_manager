package com.amorim.finance_manager.shared.exception;

public class InactiveAccountException extends RuntimeException {
    public InactiveAccountException() {
        super("A conta está inativa e não pode receber movimentações");
    }
}
