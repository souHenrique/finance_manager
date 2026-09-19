package com.amorim.finance_manager.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth-cookie")
public record AuthCookieProperties(
        boolean secure,
        String sameSite
) {

    public AuthCookieProperties {
        sameSite = sameSite == null || sameSite.isBlank() ? "Lax" : sameSite;
    }
}
