package com.amorim.finance_manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

@Schema(description = "Credenciais para autenticação do usuário")
public record LoginRequest(

        @Schema(description = "E-mail cadastrado do usuário", example = "henrique@example.com", format = "email")
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "SenhaSegura123", format = "password")
        @NotBlank(message = "Senha é obrigatória")
        String password
) {
    public LoginRequest {
        email = email == null
                ? null
                :email.trim().toLowerCase(Locale.ROOT);
    }
}
