package com.amorim.finance_manager.shared.exception;

public class BudgetNotFoundException extends RuntimeException {
    public BudgetNotFoundException() {
        super("Orçamento não encontrado");
    }
}
