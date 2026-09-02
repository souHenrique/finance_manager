package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.entity.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados necessários para criar uma conta financeira")
public record CreateAccountRequest(

        @Schema(
                description = "Nome utilizado para identificar a conta",
                example = "Conta principal"
        )
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120)
        String name,

        @Schema(
                description = "Tipo da conta",
                example = "CHECKING"
        )
        @NotNull(message = "Tipo é obrigatório")
        AccountType type,

        @Schema(
                description = "Instituição financeira da conta",
                example = "Banco Exemplo"
        )
        @Size(max = 160)
        String institution,

        @Schema(
                description = "Saldo existente no momento da criação da conta",
                example = "1500.00"
        )
        @NotNull(message = "Saldo inicial é obrigatório")
        @Digits(integer = 19, fraction = 2)
        BigDecimal initialBalance
) {
        public CreateAccountRequest {
                name = name == null ? null : name.trim();
                institution = institution == null ? null : institution.trim();
        }
}
