package com.amorim.finance_manager.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Schema(description = "Resposta padronizada para erros da API")
public record ApiError(

        @Schema(
                description = "Data e hora em que o erro ocorreu",
                example = "2026-09-02T12:00:00Z",
                format = "date-time"
        )
        Instant timestamp,

        @Schema(description = "Código numérico do status HTTP", example = "404")
        int status,

        @Schema(description = "Código estável que identifica o erro", example = "ACCOUNT_NOT_FOUND")
        String code,

        @Schema(description = "Mensagem legível que descreve o erro", example = "Conta não encontrada")
        String message,

        @Schema(
                description = "Caminho da requisição que produziu o erro",
                example = "/api/v1/accounts/0f6d7313-77f8-4b48-a63d-5338dd95461e"
        )
        String path,

        @Schema(description = "Erros de validação associados aos campos enviados")
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
