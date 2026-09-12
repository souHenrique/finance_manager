package com.amorim.finance_manager.shared.exception;

public class AccountBalanceConflictException extends RuntimeException {
    public AccountBalanceConflictException(Throwable cause) {
        super("A conta foi atualizada por outra operação. Tente novamente.", cause);
    }
}
