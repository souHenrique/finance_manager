package com.amorim.finance_manager.user.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn
) {
}
