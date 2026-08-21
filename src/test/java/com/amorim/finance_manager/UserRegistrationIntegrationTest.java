package com.amorim.finance_manager;

import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserInPostgresql() throws Exception {
        String body = """
            {
              "name": "Henrique",
              "email": "henrique@example.com",
              "password": "SenhaSegura123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Henrique"))
                .andExpect(jsonPath("$.email")
                        .value("henrique@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        String body = """
            {
              "name": "Henrique",
              "email": "henrique@example.com",
              "password": "SenhaSegura123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        String body = """
            {
              "name": "Henrique",
              "email": "email-invalido",
              "password": "SenhaSegura123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        String body = """
            {
              "name": "   ",
              "email": "henrique@example.com",
              "password": "SenhaSegura123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void shouldRejectBlankPassword()throws Exception {
        String body = """
            {
              "name": "Henrique",
              "email": "henrique@example.com",
              "password": "   "
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void shouldNormalizeEmail() throws Exception {
        String body = """
            {
              "name": "Henrique",
              "email": "  HENRIQUE@Example.COM  ",
              "password": "SenhaSegura123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email")
                        .value("henrique@example.com"));

        User savedUser = userRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        assertThat(savedUser.getEmail())
                .isEqualTo("henrique@example.com");
    }

    @Test
    void shouldStorePasswordAsBcrypt() throws Exception {
        String rawPassword = "SenhaSegura123";

        String body = """
            {
              "name": "Henrique",
              "email": "henrique@example.com",
              "password": "%s"
            }
            """.formatted(rawPassword);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        User savedUser = userRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        assertThat(savedUser.getPasswordHash())
                .startsWith("$2");

        assertThat(passwordEncoder.matches(
                rawPassword,
                savedUser.getPasswordHash()
        )).isTrue();

        assertThat(savedUser.getPasswordHash())
                .isNotEqualTo(rawPassword);
    }
}
