package com.amorim.finance_manager.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration}") long expirationMillis
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, 0);
    }

    public String generateToken(UserDetails userDetails, int authenticationVersion) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("authenticationVersion", authenticationVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusMillis(expirationMillis)
                ))
                .signWith(signingKey)
                .compact();
    }

    public boolean isValid(String token, UserDetails userDetails) {
        return isValid(token, userDetails, 0);
    }

    public boolean isValid(String token, UserDetails userDetails, int authenticationVersion) {
        try {
            Claims claims = parse(token).getPayload();
            Number tokenAuthenticationVersion = claims.get("authenticationVersion", Number.class);

            return Objects.equals(
                    claims.getSubject(),
                    userDetails.getUsername()
            ) && (tokenAuthenticationVersion == null
                    ? authenticationVersion == 0
                    : tokenAuthenticationVersion.intValue() == authenticationVersion);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return parse(token)
                .getPayload()
                .getSubject();
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }
}
