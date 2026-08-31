package com.amorim.finance_manager.category.dto;

import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        CategoryType type,
        UUID parentCategoryId,
        CategoryStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
