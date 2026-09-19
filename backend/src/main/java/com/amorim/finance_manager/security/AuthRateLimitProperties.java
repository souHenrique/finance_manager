package com.amorim.finance_manager.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth-rate-limit")
public record AuthRateLimitProperties(
        Rule login,
        Rule register,
        int maxTrackedClients
) {

    public AuthRateLimitProperties {
        login = login == null ? new Rule(5, Duration.ofMinutes(1)) : login;
        register = register == null ? new Rule(5, Duration.ofHours(1)) : register;

        if (maxTrackedClients < 1) {
            maxTrackedClients = 10_000;
        }
    }

    public record Rule(int maxAttempts, Duration window) {

        public Rule {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("Rate limit attempts must be greater than zero");
            }

            if (window == null || window.isNegative() || window.isZero()) {
                throw new IllegalArgumentException("Rate limit window must be greater than zero");
            }
        }
    }
}
