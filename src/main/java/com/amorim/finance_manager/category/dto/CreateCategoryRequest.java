package com.amorim.finance_manager.category.dto;

import com.amorim.finance_manager.category.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120)
        String name,

        @NotNull(message = "Tipo é obrigatório")
        CategoryType type,

        UUID parentCategoryId
) {
    public CreateCategoryRequest {
        name = name == null ? null : name.trim();
    }
}
