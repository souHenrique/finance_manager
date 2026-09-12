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
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusMillis(expirationMillis)
                ))
                .signWith(signingKey)
                .compact();
    }

    public boolean isValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parse(token).getPayload();

            return Objects.equals(
                    claims.getSubject(),
                    userDetails.getUsername()
            );
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
