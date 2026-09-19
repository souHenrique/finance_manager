package com.amorim.finance_manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para alteração da senha do usuário autenticado")
public record ChangePasswordRequest(

        @Schema(description = "Senha atual do usuário", example = "SenhaSegura123!", format = "password")
        @NotBlank(message = "Senha atual é obrigatória")
        String currentPassword,

        @Schema(
                description = "Nova senha entre 8 e 72 caracteres, com maiúscula, minúscula, número e caractere especial",
                example = "NovaSenhaSegura456!",
                format = "password",
                minLength = 8,
                maxLength = 72
        )
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 8, max = 72, message = "Nova senha deve possuir entre 8 e 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Nova senha deve conter ao menos uma letra maiúscula, uma minúscula, um número e um caractere especial"
        )
        String newPassword
) {
}
