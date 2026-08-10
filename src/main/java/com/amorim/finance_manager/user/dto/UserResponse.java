package com.amorim.finance_manager.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        OffsetDateTime createdAt
) {
}
