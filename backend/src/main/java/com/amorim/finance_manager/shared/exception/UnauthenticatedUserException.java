package com.amorim.finance_manager.shared.exception;

public class UnauthenticatedUserException extends RuntimeException {
    public UnauthenticatedUserException() {
        super("Usuário não autenticado");
    }
}
