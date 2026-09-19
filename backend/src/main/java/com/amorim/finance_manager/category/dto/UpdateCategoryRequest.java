package com.amorim.finance_manager.category.dto;

import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryIcon;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados opcionais para atualização de uma categoria")
public record UpdateCategoryRequest(

        @Schema(description = "Novo nome da categoria")
        @Size(max = 120)
        String name,

        @Schema(description = "Novo ícone genérico da categoria", example = "SHOPPING")
        CategoryIcon icon,

        @Schema(description = "Novo status da categoria", example = "INACTIVE")
        CategoryStatus status
) {
    public UpdateCategoryRequest(String name, CategoryStatus status) {
        this(name, null, status);
    }

    public UpdateCategoryRequest {
        name = name == null ? null : name.trim();
    }
}
