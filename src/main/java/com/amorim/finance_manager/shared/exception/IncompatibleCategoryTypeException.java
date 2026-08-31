package com.amorim.finance_manager.shared.exception;

public class IncompatibleCategoryTypeException extends RuntimeException {
    public IncompatibleCategoryTypeException() {
        super("Tipo de categoria incompatível");
    }
}
