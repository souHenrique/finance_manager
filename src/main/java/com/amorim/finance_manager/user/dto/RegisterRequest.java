package com.amorim.finance_manager.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record RegisterRequest(
        @NotBlank(message = "Nome é obrigatório")
        String name,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

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
