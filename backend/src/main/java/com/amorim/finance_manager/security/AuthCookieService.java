package com.amorim.finance_manager.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    public static final String SESSION_COOKIE_NAME = "nummo_session";
    private static final String COOKIE_PATH = "/api";

    private final AuthCookieProperties properties;

    public AuthCookieService(AuthCookieProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie createSessionCookie(String token, long expiresInSeconds) {
        return baseCookie(token)
                .maxAge(Duration.ofSeconds(expiresInSeconds))
                .build();
    }

    public ResponseCookie clearSessionCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(SESSION_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(COOKIE_PATH);
    }
}
