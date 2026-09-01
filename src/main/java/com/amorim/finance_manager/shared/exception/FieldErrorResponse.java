package com.amorim.finance_manager.shared.exception;

public record FieldErrorResponse(
        String field,
        String message
) {
}
