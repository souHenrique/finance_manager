package com.amorim.finance_manager.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados opcionais para atualização de um orçamento")
public record UpdateBudgetRequest(

        @Schema(
                description = "Nova categoria de despesa",
                example = "57b1879c-a98e-4718-b66d-47f970ab6709",
                format = "uuid"
        )
        UUID categoryId,

        @Schema(description = "Novo mês de referência", example = "10")
        @Min(value = 1, message = "Mês deve estar entre 1 e 12")
        @Max(value = 12, message = "Mês deve estar entre 1 e 12")
        Integer month,

        @Schema(description = "Novo ano de referência", example = "2026")
        @Min(value = 1, message = "Ano deve ser positivo")
        @Max(value = 9999, message = "Ano deve ser menor ou igual a 9999")
        Integer year,

        @Schema(description = "Novo limite financeiro", example = "1800.00")
        @DecimalMin(
                value = "0.01",
                message = "Limite do orçamento deve ser maior que zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Limite deve possuir no máximo 17 dígitos inteiros e 2 decimais"
        )
        BigDecimal amountLimit
) {
}
