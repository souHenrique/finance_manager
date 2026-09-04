package com.amorim.finance_manager.shared.exception;

public class CreditCardNotFoundException extends RuntimeException {
    public CreditCardNotFoundException() {
        super("Cartão de crédito não encontrado");
    }
}
