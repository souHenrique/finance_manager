package com.amorim.finance_manager.shared.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException() {
        super("Categoria não encontrada");
    }
}
