package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.entity.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados opcionais para atualização de uma conta")
public record UpdateAccountRequest(

        @Schema(description = "Novo nome da conta", example = "Reserva de emergência")
        @Size(max = 120)
        String name,

        @Schema(description = "Novo tipo da conta", example = "SAVINGS")
        AccountType type,

        @Schema(description = "Nova instituição financeira", example = "Banco Exemplo")
        @Size(max = 160)
        String institution
) {
    public UpdateAccountRequest {
        name = name == null ? null : name.trim();
        institution = institution == null
                ? null
                : institution.trim();
    }
}
