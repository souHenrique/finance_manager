package com.amorim.finance_manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Dados públicos do usuário")
public record UserResponse(

        @Schema(
                description = "Identificador do usuário",
                example = "2a1fbc5b-cbb9-4879-b0c5-42f034d64261",
                format = "uuid"
        )
        UUID id,

        @Schema(description = "Nome do usuário", example = "Henrique Amorim")
        String name,

        @Schema(description = "E-mail do usuário", example = "henrique@example.com", format = "email")
        String email,

        @Schema(
                description = "Data e hora de criação do usuário",
                example = "2026-09-02T12:00:00Z",
                format = "date-time"
        )
        Instant createdAt,

        @Schema(
                description = "Data e hora da última atualização",
                example = "2026-09-02T12:30:00Z",
                format = "date-time"
        )
        Instant updatedAt
) {
}
