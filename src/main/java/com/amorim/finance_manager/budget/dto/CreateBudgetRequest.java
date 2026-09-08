package com.amorim.finance_manager.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados para criação de um orçamento mensal")
public record CreateBudgetRequest(

        @Schema(
                description = "Identificador da categoria de despesa",
                example = "57b1879c-a98e-4718-b66d-47f970ab6709",
                format = "uuid"
        )
        @NotNull(message = "Categoria é obrigatória")
        UUID categoryId,

        @Schema(description = "Mês de referência", example = "9")
        @NotNull(message = "Mês é obrigatório")
        @Min(value = 1, message = "Mês deve estar entre 1 e 12")
        @Max(value = 12, message = "Mês deve estar entre 1 e 12")
        Integer month,

        @Schema(description = "Ano de referência", example = "2026")
        @NotNull(message = "Ano é obrigatório")
        @Min(value = 1, message = "Ano deve ser positivo")
        @Max(value = 9999, message = "Ano deve ser menor ou igual a 9999")
        Integer year,

        @Schema(description = "Limite financeiro do orçamento", example = "1500.00")
        @NotNull(message = "Limite do orçamento é obrigatório")
        @DecimalMin(value = "0.01", message = "Limite do orçamento deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "Limite deve possuir no máximo 17 dígitos inteiros e decimais")
        BigDecimal amountLimit
) {
}
