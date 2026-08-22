package com.amorim.finance_manager;

import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class UserProfileIntegrationTest {

    private static final String PASSWORD = "SenhaSegura123";

    private static final String USER_A_EMAIL =
            "user-a@example.com";

    private static final String USER_B_EMAIL =
            "user-b@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnAuthenticatedUserProfile() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("User A"))
                .andExpect(jsonPath("$.email").value(USER_A_EMAIL))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldNotReturnAnotherUsersProfile() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        registerUser(
                "User B",
                USER_B_EMAIL,
                PASSWORD
        );

        String tokenA = login(
                USER_A_EMAIL,
                PASSWORD
        );

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenA
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_A_EMAIL))
                .andExpect(jsonPath("$.name").value("User A"));
    }

    @Test
    void shouldUpdateCurrentUserName() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        String body = """
                {
                  "name": "Updated User"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.email").value(USER_A_EMAIL));

        User savedUser = userRepository
                .findByEmail(USER_A_EMAIL)
                .orElseThrow();

        assertThat(savedUser.getName())
                .isEqualTo("Updated User");
    }

    @Test
    void shouldUpdateCurrentUserEmail() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        String body = """
                {
                  "email": "UPDATED@EXAMPLE.COM"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("updated@example.com"));

        assertThat(
                userRepository.findByEmail("updated@example.com")
        ).isPresent();

        assertThat(
                userRepository.findByEmail(USER_A_EMAIL)
        ).isEmpty();
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        registerUser(
                "User B",
                USER_B_EMAIL,
                PASSWORD
        );

        String tokenA = login(
                USER_A_EMAIL,
                PASSWORD
        );

        String body = """
                {
                  "email": "user-b@example.com"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenA
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("EMAIL_ALREADY_EXISTS"));

        User userA = userRepository
                .findByEmail(USER_A_EMAIL)
                .orElseThrow();

        assertThat(userA.getEmail())
                .isEqualTo(USER_A_EMAIL);
    }

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {
        mockMvc.perform(
                        get("/api/v1/users/me")
                )
                .andExpect(status().isUnauthorized());

        String body = """
                {
                  "name": "Attempt Without Token"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotModifyProtectedFields() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        User originalUser = userRepository
                .findByEmail(USER_A_EMAIL)
                .orElseThrow();

        String originalPasswordHash =
                originalUser.getPasswordHash();

        Instant originalCreatedAt =
                originalUser.getCreatedAt();

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        String body = """
                {
                  "name": "Updated Name",
                  "id": "00000000-0000-0000-0000-000000000000",
                  "passwordHash": "fake-password",
                  "createdAt": "2000-01-01T00:00:00Z",
                  "updatedAt": "2000-01-01T00:00:00Z"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(originalUser.getId().toString()))
                .andExpect(jsonPath("$.passwordHash")
                        .doesNotExist());

        User updatedUser = userRepository
                .findById(originalUser.getId())
                .orElseThrow();

        assertThat(updatedUser.getId())
                .isEqualTo(originalUser.getId());

        assertThat(updatedUser.getPasswordHash())
                .isEqualTo(originalPasswordHash);

        assertThat(updatedUser.getCreatedAt())
                .isEqualTo(originalCreatedAt);

        assertThat(updatedUser.getName())
                .isEqualTo("Updated Name");
    }

    @Test
    void shouldRejectEmptyPatch() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        String body = """
                {
                  "email": "invalid-email"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    private void registerUser(
            String name,
            String email,
            String password
    ) throws Exception {

        String body = """
                {
                  "name": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(name, email, password);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated());
    }

    private String login(
            String email,
            String password
    ) throws Exception {

        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper
                .readTree(
                        result.getResponse()
                                .getContentAsString()
                )
                .get("token")
                .asText();
    }
}
