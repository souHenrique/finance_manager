package com.amorim.finance_manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

        @Schema(
                description = "Senha entre 8 e 72 caracteres, com maiúscula, minúscula, número e caractere especial",
                example = "SenhaSegura123!",
                format = "password",
                minLength = 8,
                maxLength = 72
        )
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 72, message = "Senha deve possuir entre 8 e 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Senha deve conter ao menos uma letra maiúscula, uma minúscula, um número e um caractere especial"
        )
        String password
) {
    public RegisterRequest {
        name = name == null ? null : name.trim();
        email = email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }
}
