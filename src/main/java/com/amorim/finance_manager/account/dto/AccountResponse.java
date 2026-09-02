package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Dados de uma conta financeira")
public record AccountResponse(

        @Schema(description = "Identificador da conta", example = "0f6d7313-77f8-4b48-a63d-5338dd95461e")
        UUID id,

        @Schema(description = "Nome da conta", example = "Conta principal")
        String name,

        @Schema(description = "Tipo da conta", example = "CHECKING")
        AccountType type,

        @Schema(description = "Instituição financeira", example = "Banco Exemplo")
        String institution,

        @Schema(description = "Saldo informado na criação", example = "1500.00")
        BigDecimal initialBalance,

        @Schema(description = "Saldo atual, que pode ser negativo", example = "1320.50")
        BigDecimal currentBalance,

        @Schema(description = "Status da conta", example = "ACTIVE")
        AccountStatus status,

        @Schema(description = "Versão utilizada no controle de concorrência otimista", example = "0")
        Long version,

        @Schema(description = "Data e hora de criação", example = "2026-09-02T12:00:00Z")
        Instant createdAt,

        @Schema(description = "Data e hora da última atualização", example = "2026-09-02T12:30:00Z")
        Instant updatedAt
) {
}
