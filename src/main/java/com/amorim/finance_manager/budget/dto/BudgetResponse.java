package com.amorim.finance_manager.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Dados de um orçamento mensal")
public record BudgetResponse(

        @Schema(
                description = "Identificador do orçamento",
                example = "c487c4cf-d948-4ba8-a85f-e36bb798c928",
                format = "uuid"
        )
        UUID id,

        @Schema(
                description = "Identificador da categoria de despesa",
                example = "57b1879c-a98e-4718-b66d-47f970ab6709",
                format = "uuid"
        )
        UUID categoryId,

        @Schema(description = "Mês de referência", example = "9")
        Integer month,

        @Schema(description = "Ano de referência", example = "2026")
        Integer year,

        @Schema(description = "Limite financeiro do orçamento", example = "1500.00")
        BigDecimal amountLimit,

        @Schema(
                description = "Data e hora de criação",
                example = "2026-09-08T14:00:00Z",
                format = "date-time"
        )
        Instant createdAt,

        @Schema(
                description = "Data e hora da última atualização",
                example = "2026-09-08T14:30:00Z",
                format = "date-time"
        )
        Instant updatedAt
) {
}
