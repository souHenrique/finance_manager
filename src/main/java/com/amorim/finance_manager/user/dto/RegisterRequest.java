package com.amorim.finance_manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

@Schema(description = "Dados para cadastro de um usuário")
public record RegisterRequest(

        @Schema(description = "Nome do usuário", example = "Henrique Amorim")
        @NotBlank(message = "Nome é obrigatório")
        String name,

        @Schema(description = "E-mail utilizado para identificação e login", example = "henrique@example.com", format = "email")
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "SenhaSegura123", format = "password")
        @NotBlank(message = "Senha é obrigatória")
        String password
) {
    public RegisterRequest {
        name = name == null ? null : name.trim();
        email = email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }
}
