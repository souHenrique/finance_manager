package com.amorim.finance_manager.shared.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("E-mail já cadastrado");
    }
}
