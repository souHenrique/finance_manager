package com.amorim.finance_manager.shared.exception;

public class CreditCardPurchaseAlreadyRefundedException extends RuntimeException {
    public CreditCardPurchaseAlreadyRefundedException() {
        super("A compra já possui estorno registrado.");
    }
}
