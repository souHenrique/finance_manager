package com.amorim.finance_manager.shared.exception;

public class TransactionAlreadyCancelledException extends RuntimeException {

    public TransactionAlreadyCancelledException() {
        super("Transação já está cancelada");
    }

    public TransactionAlreadyCancelledException(String message) {
        super(message);
    }
}
