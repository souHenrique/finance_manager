package com.amorim.finance_manager.shared.exception;

public class InvalidCreditCardStatusException extends RuntimeException {
    public InvalidCreditCardStatusException() {
        super("Apenas cartões ativos podem receber novas compras");
    }
}
