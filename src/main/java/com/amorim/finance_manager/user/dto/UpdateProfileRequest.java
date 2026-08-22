package com.amorim.finance_manager.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record UpdateProfileRequest(
        @Size(max = 120, message = "Nome deve possuir no máximo 120 caracteres")
        String name,

        @Email(message = "E-mail inválido")
        @Size(max = 320, message = "E-mail deve possuir no máximo 320 caracteres")
        String email
) {
    public UpdateProfileRequest {
        name = name == null ? null : name.trim();

        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
