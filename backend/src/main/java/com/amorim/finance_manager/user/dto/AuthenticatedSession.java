package com.amorim.finance_manager.user.dto;

/**
 * Internal login result. The JWT is deliberately kept server-side so it can be
 * written to an HttpOnly cookie instead of being exposed in a JSON response.
 */
public record AuthenticatedSession(String token, long expiresIn) {
}
