package com.amorim.finance_manager.shared.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException() {
        super("Conta não encontrada");
    }
}
