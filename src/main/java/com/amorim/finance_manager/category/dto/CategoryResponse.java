package com.amorim.finance_manager.category.dto;

import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Dados de uma categoria ou subcategoria")
public record CategoryResponse(

        @Schema(
                description = "Identificador da categoria",
                example = "c487c4cf-d948-4ba8-a85f-e36bb798c928",
                format = "uuid"
        )
        UUID id,

        @Schema(description = "Nome da categoria", example = "Alimentação")
        String name,

        @Schema(description = "Tipo financeiro da categoria", example = "EXPENSE")
        CategoryType type,

        @Schema(
                description = "Identificador da categoria pai; ausente para categorias principais",
                example = "57b1879c-a98e-4718-b66d-47f970ab6709",
                format = "uuid"
        )
        UUID parentCategoryId,

        @Schema(description = "Status da categoria", example = "ACTIVE")
        CategoryStatus status,

        @Schema(
                description = "Data e hora de criação da categoria",
                example = "2026-09-02T12:00:00Z",
                format = "date-time"
        )
        Instant createdAt,

        @Schema(
                description = "Data e hora da última atualização",
                example = "2026-09-02T12:30:00Z",
                format = "date-time"
        )
        Instant updatedAt
) {
}
