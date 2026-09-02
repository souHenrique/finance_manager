package com.amorim.finance_manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token JWT gerado após autenticação")
public record AuthResponse(

        @Schema(
                description = "Token JWT utilizado para autenticar as próximas requisições",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        String token,

        @Schema(description = "Tipo do token retornado", example = "Bearer")
        String tokenType,

        @Schema(description = "Tempo restante de validade do token em segundos", example = "3600")
        long expiresIn
) {
}
