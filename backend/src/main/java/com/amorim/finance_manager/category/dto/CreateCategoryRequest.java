package com.amorim.finance_manager.category.dto;

import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.entity.CategoryIcon;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Dados para criação de uma categoria ou subcategoria")
public record CreateCategoryRequest(

        @Schema(description = "Nome da categoria", example = "Alimentação")
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120)
        String name,

        @Schema(
                description = "Ícone genérico da categoria; TAG é usado quando ausente",
                example = "FOOD"
        )
        CategoryIcon icon,

        @Schema(description = "Tipo financeiro da categoria", example = "EXPENSE")
        @NotNull(message = "Tipo é obrigatório")
        CategoryType type,

        @Schema(
                description = "Identificador da categoria pai; ausente para categorias principais",
                example = "c487c4cf-d948-4ba8-a85f-e36bb798c928",
                format = "uuid"
        )
        UUID parentCategoryId
) {
    public CreateCategoryRequest(
            String name,
            CategoryType type,
            UUID parentCategoryId
    ) {
        this(name, null, type, parentCategoryId);
    }

    public CreateCategoryRequest {
        name = name == null ? null : name.trim();
    }
}
