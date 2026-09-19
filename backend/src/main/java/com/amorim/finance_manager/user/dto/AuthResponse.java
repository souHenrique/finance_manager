package com.amorim.finance_manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do login. A sessão é enviada no cookie HttpOnly nummo_session.")
public record AuthResponse(

        @Schema(description = "Tempo de validade da sessão em segundos", example = "3600")
        long expiresIn
) {
}
