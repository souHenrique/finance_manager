package com.amorim.finance_manager;

import com.amorim.finance_manager.security.AuthRateLimiter;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.auth-rate-limit.login.max-attempts=2",
        "app.auth-rate-limit.register.max-attempts=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class AuthRateLimitIntegrationTest {

    private static final String CLIENT_IP = "203.0.113.10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthRateLimiter rateLimiter;

    @BeforeEach
    void cleanState() {
        rateLimiter.clear();
        userRepository.deleteAll();
    }

    @Test
    void shouldRateLimitLoginAttemptsByClientIp() throws Exception {
        createUser();
        String body = """
                {
                  "email": "walter.white@example.com",
                  "password": "SenhaIncorreta123!"
                }
                """;

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(request -> {
                                request.setRemoteAddr(CLIENT_IP);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(CLIENT_IP);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    void shouldRateLimitRegistrationAttemptsByClientIp() throws Exception {
        String invalidBody = """
                {
                  "name": "Walter White",
                  "email": "walter.white@example.com",
                  "password": "curta"
                }
                """;

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(request -> {
                                request.setRemoteAddr(CLIENT_IP);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(request -> {
                            request.setRemoteAddr(CLIENT_IP);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));
    }

    private void createUser() {
        User user = new User();
        user.setName("Walter White");
        user.setEmail("walter.white@example.com");
        user.setPasswordHash(passwordEncoder.encode("SenhaSegura123!"));
        userRepository.saveAndFlush(user);
    }
}
