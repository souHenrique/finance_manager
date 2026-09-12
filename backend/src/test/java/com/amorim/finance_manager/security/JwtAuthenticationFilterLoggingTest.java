package com.amorim.finance_manager.security;

import ch.qos.logback.classic.Level;
import com.amorim.finance_manager.testsupport.LogCapture;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterLoggingTest {

    private static final String TOKEN = "eyJ-sensitive-rejected-token";

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldLogRejectedTokenWithoutExposingAuthorizationOrJwt() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/accounts");
        MockHttpServletResponse response =
                new MockHttpServletResponse();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);

        when(jwtService.extractSubject(TOKEN))
                .thenThrow(new JwtException("Rejected token: " + TOKEN));

        try (LogCapture logs = LogCapture.forClassAtLevel(
                JwtAuthenticationFilter.class,
                Level.DEBUG
        )) {
            filter.doFilter(request, response, filterChain);

            assertThat(logs.messages())
                    .contains(
                            "event=auth.token.rejected reason=JwtException"
                                    + " path=/api/v1/accounts"
                    )
                    .allSatisfy(message -> {
                        assertThat(message).doesNotContain(TOKEN);
                        assertThat(message).doesNotContain("Authorization");
                        assertThat(message).doesNotContain("Bearer");
                        assertThat(message).doesNotContain("Rejected token");
                    });
        }

        verify(filterChain).doFilter(request, response);
    }
}
