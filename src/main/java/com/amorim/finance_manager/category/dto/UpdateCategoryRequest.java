package com.amorim.finance_manager.category.dto;

import com.amorim.finance_manager.category.entity.CategoryStatus;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(

        @Size(max = 120)
        String name,

        CategoryStatus status
) {
    public UpdateCategoryRequest {
        name = name == null ? null : name.trim();
    }
}
