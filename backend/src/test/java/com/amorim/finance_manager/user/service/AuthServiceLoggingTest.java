package com.amorim.finance_manager.user.service;

import com.amorim.finance_manager.security.JwtService;
import com.amorim.finance_manager.shared.exception.InvalidCredentialsException;
import com.amorim.finance_manager.testsupport.LogCapture;
import com.amorim.finance_manager.user.dto.AuthResponse;
import com.amorim.finance_manager.user.dto.LoginRequest;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoggingTest {

    private static final String EMAIL = "audit-user@example.com";
    private static final String PASSWORD = "SensitivePassword123!";
    private static final String JWT = "eyJ-sensitive-jwt-value";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldLogSuccessfulAuthenticationWithoutSensitiveData() {
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);
        when(authentication.getPrincipal())
                .thenReturn(userDetails);
        when(userDetails.getUsername())
                .thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        when(user.getAuthenticationVersion())
                .thenReturn(0);
        when(jwtService.generateToken(userDetails, 0))
                .thenReturn(JWT);
        when(jwtService.getExpirationSeconds())
                .thenReturn(3600L);

        try (LogCapture logs = LogCapture.forClass(AuthService.class)) {
            AuthResponse response = authService.login(request);

            assertThat(response.token()).isEqualTo(JWT);
            assertThat(logs.messages())
                    .contains("event=auth.login.success");
            assertSensitiveDataIsAbsent(logs.messages());
        }
    }

    @Test
    void shouldLogFailedAuthenticationWithoutSensitiveData() {
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid password: " + PASSWORD));

        try (LogCapture logs = LogCapture.forClass(AuthService.class)) {
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);

            assertThat(logs.messages())
                    .contains("event=auth.login.failure reason=invalid_credentials");
            assertSensitiveDataIsAbsent(logs.messages());
        }
    }

    private void assertSensitiveDataIsAbsent(List<String> messages) {
        assertThat(messages)
                .allSatisfy(message -> {
                    assertThat(message).doesNotContain(PASSWORD);
                    assertThat(message).doesNotContain(JWT);
                    assertThat(message).doesNotContain("Authorization");
                    assertThat(message).doesNotContain("Bearer");
                });
    }
}
