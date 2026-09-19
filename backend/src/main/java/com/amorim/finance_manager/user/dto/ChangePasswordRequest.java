package com.amorim.finance_manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para alteração da senha do usuário autenticado")
public record ChangePasswordRequest(

        @Schema(description = "Senha atual do usuário", example = "SenhaSegura123", format = "password")
        @NotBlank(message = "Senha atual é obrigatória")
        String currentPassword,

        @Schema(description = "Nova senha do usuário", example = "NovaSenhaSegura456", format = "password")
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 8, max = 72, message = "Nova senha deve possuir entre 8 e 72 caracteres")
        String newPassword
) {
}
