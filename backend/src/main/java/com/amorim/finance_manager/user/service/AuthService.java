package com.amorim.finance_manager.user.service;

import com.amorim.finance_manager.security.JwtService;
import com.amorim.finance_manager.shared.exception.InvalidCredentialsException;
import com.amorim.finance_manager.user.dto.AuthResponse;
import com.amorim.finance_manager.user.dto.LoginRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String token = jwtService.generateToken(userDetails);

            log.info("event=auth.login.success");

            return new AuthResponse(
                    token,
                    "Bearer",
                    jwtService.getExpirationSeconds()
            );
        } catch (AuthenticationException exception) {
            log.warn("event=auth.login.failure reason=invalid_credentials");
            throw new InvalidCredentialsException("Credenciais inválidas");
        }
    }
}
