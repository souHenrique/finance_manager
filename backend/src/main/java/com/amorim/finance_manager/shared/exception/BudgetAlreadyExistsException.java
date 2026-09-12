package com.amorim.finance_manager.shared.exception;

public class BudgetAlreadyExistsException extends RuntimeException {
    public BudgetAlreadyExistsException() {
        super("Já existe um orçamento para a categoria e o período informados");
    }
}
