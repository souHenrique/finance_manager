package com.amorim.finance_manager.user.service;

import com.amorim.finance_manager.security.JwtService;
import com.amorim.finance_manager.shared.exception.InvalidCredentialsException;
import com.amorim.finance_manager.user.dto.AuthResponse;
import com.amorim.finance_manager.user.dto.LoginRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
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

            return new AuthResponse(
                    token,
                    "Bearer",
                    jwtService.getExpirationSeconds()
            );
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }
    }
}
