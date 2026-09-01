package com.amorim.finance_manager.shared.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorResponse> fieldErrors
) {
    public ApiError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ApiError of(HttpStatus status, ApiErrorCode code, String message, String path) {
        return new ApiError(Instant.now(), status.value(), code.name(), message, path, List.of());
    }

    public static ApiError of(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            String path,
            List<FieldErrorResponse> fieldErrors
    ) {
        return new ApiError(Instant.now(), status.value(), code.name(), message, path, fieldErrors);
    }
}
