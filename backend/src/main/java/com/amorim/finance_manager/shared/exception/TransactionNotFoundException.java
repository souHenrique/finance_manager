package com.amorim.finance_manager.shared.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException() {
        super("Transação não encontrada");
    }
}
