package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.repository.AccountRepository;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class AccountIntegrationTest {

    private static final String PASSWORD =
            "IntegrationPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        /*
         * Accounts devem ser removidas antes dos usuários
         * por causa da foreign key user_id.
         */
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateAccountWithCurrentBalanceEqualToInitialBalance()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID accountId = createAccount(
                token,
                "Checking Account",
                "CHECKING",
                "Example Bank",
                new BigDecimal("1500.75")
        );

        Account account = accountRepository
                .findById(accountId)
                .orElseThrow();

        assertThat(account.getUserId())
                .isEqualTo(user.id());

        assertThat(account.getInitialBalance())
                .isEqualByComparingTo("1500.75");

        assertThat(account.getCurrentBalance())
                .isEqualByComparingTo("1500.75");

        assertThat(account.getType().name())
                .isEqualTo("CHECKING");
    }

    @Test
    void shouldListOnlyAuthenticatedUserAccounts()
            throws Exception {

        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");

        String tokenA = login(userA);
        String tokenB = login(userB);

        createAccount(
                tokenA,
                "Account A",
                "CHECKING",
                "Bank A",
                new BigDecimal("100.00")
        );

        createAccount(
                tokenB,
                "Account B",
                "SAVINGS",
                "Bank B",
                new BigDecimal("200.00")
        );

        mockMvc.perform(
                        get("/api/v1/accounts")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenA
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name")
                        .value("Account A"))
                .andExpect(jsonPath("$[0].type")
                        .value("CHECKING"));
    }

    @Test
    void shouldReturnAccountDetailsForOwner()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID accountId = createAccount(
                token,
                "Savings Account",
                "SAVINGS",
                "Example Bank",
                new BigDecimal("2500.50")
        );

        MvcResult result = mockMvc.perform(
                        get("/api/v1/accounts/{id}", accountId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(accountId.toString()))
                .andExpect(jsonPath("$.name")
                        .value("Savings Account"))
                .andExpect(jsonPath("$.type")
                        .value("SAVINGS"))
                .andExpect(jsonPath("$.currentBalance")
                        .value(2500.50))
                .andReturn();

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(new BigDecimal(
                body.get("initialBalance").asText()
        ))
                .isEqualByComparingTo("2500.50");
    }

    @Test
    void shouldRejectDetailsForAnotherUser()
            throws Exception {

        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");

        String tokenA = login(userA);
        String tokenB = login(userB);

        UUID accountIdA = createAccount(
                tokenA,
                "Account A",
                "CHECKING",
                "Bank A",
                new BigDecimal("100.00")
        );

        mockMvc.perform(
                        get("/api/v1/accounts/{id}", accountIdA)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenB
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectUpdateOfAnotherUsersAccount()
            throws Exception {

        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");

        String tokenA = login(userA);
        String tokenB = login(userB);

        UUID accountIdB = createAccount(
                tokenB,
                "Account B",
                "CHECKING",
                "Bank B",
                new BigDecimal("500.00")
        );

        String body = """
                {
                  "name": "Unauthorized Update"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/accounts/{id}", accountIdB)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenA
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateAccountMetadata()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID accountId = createAccount(
                token,
                "Old Name",
                "CHECKING",
                "Old Bank",
                new BigDecimal("1000.00")
        );

        String body = """
                {
                  "name": "Updated Name",
                  "institution": "Updated Bank"
                }
                """;

        MvcResult result = mockMvc.perform(
                        patch("/api/v1/accounts/{id}", accountId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Updated Name"))
                .andExpect(jsonPath("$.institution")
                        .value("Updated Bank"))
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(new BigDecimal(
                response.get("initialBalance").asText()
        ))
                .isEqualByComparingTo("1000.00");

        assertThat(new BigDecimal(
                response.get("currentBalance").asText()
        ))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldKeepBalancesUnchangedWhenUpdatingMetadata()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID accountId = createAccount(
                token,
                "Account",
                "CHECKING",
                "Bank",
                new BigDecimal("1234.56")
        );

        Account beforeUpdate = accountRepository
                .findById(accountId)
                .orElseThrow();

        String body = """
                {
                  "name": "Updated Account"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/accounts/{id}", accountId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk());

        Account afterUpdate = accountRepository
                .findById(accountId)
                .orElseThrow();

        assertThat(afterUpdate.getInitialBalance())
                .isEqualByComparingTo(
                        beforeUpdate.getInitialBalance()
                );

        assertThat(afterUpdate.getCurrentBalance())
                .isEqualByComparingTo(
                        beforeUpdate.getCurrentBalance()
                );
    }

    @Test
    void shouldInactivateAccountAndKeepBalancesUnchanged()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID accountId = createAccount(
                token,
                "Account",
                "CHECKING",
                "Bank",
                new BigDecimal("2000.00")
        );

        String body = """
                {
                  "status": "INACTIVE"
                }
                """;

        MvcResult result = mockMvc.perform(
                        patch("/api/v1/accounts/{id}/status", accountId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("INACTIVE"))
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(new BigDecimal(
                response.get("initialBalance").asText()
        ))
                .isEqualByComparingTo("2000.00");

        assertThat(new BigDecimal(
                response.get("currentBalance").asText()
        ))
                .isEqualByComparingTo("2000.00");

        Account account = accountRepository
                .findById(accountId)
                .orElseThrow();

        assertThat(account.getStatus())
                .isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    void shouldListInactiveAccount()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID accountId = createAccount(
                token,
                "Inactive Account",
                "SAVINGS",
                "Bank",
                new BigDecimal("500.00")
        );

        mockMvc.perform(
                        patch("/api/v1/accounts/{id}/status", accountId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INACTIVE\"}")
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/v1/accounts")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id")
                        .value(accountId.toString()))
                .andExpect(jsonPath("$[0].status")
                        .value("INACTIVE"));
    }

    @Test
    void shouldRejectStatusUpdateForAnotherUsersAccount()
            throws Exception {

        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");

        String tokenA = login(userA);
        String tokenB = login(userB);

        UUID accountIdA = createAccount(
                tokenA,
                "Account A",
                "CHECKING",
                "Bank A",
                new BigDecimal("100.00")
        );

        mockMvc.perform(
                        patch("/api/v1/accounts/{id}/status", accountIdA)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenB
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INACTIVE\"}")
                )
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        get("/api/v1/accounts/{id}", accountIdA)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenA
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"));
    }

    @Test
    void shouldNotChangeBalancesThroughMetadataPatch()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID accountId = createAccount(
                token,
                "Account",
                "CHECKING",
                "Bank",
                new BigDecimal("1234.56")
        );

        String body = """
                {
                  "name": "Updated Account",
                  "initialBalance": 999999.99,
                  "currentBalance": 999999.99
                }
                """;

        MvcResult result = mockMvc.perform(
                        patch("/api/v1/accounts/{id}", accountId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Updated Account"))
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(new BigDecimal(
                response.get("initialBalance").asText()
        ))
                .isEqualByComparingTo("1234.56");

        assertThat(new BigDecimal(
                response.get("currentBalance").asText()
        ))
                .isEqualByComparingTo("1234.56");
    }

    @Test
    void shouldRejectStatusUpdateWithoutJwt()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/v1/accounts/{id}/status",
                                UUID.randomUUID()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INACTIVE\"}")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidStatusValueWithAuthenticatedRequest()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID accountId = createAccount(
                token,
                "Account",
                "CHECKING",
                "Bank",
                new BigDecimal("100.00")
        );

        mockMvc.perform(
                        patch("/api/v1/accounts/{id}/status", accountId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INVALID\"}")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRequestsWithoutJwt()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/accounts")
                )
                .andExpect(status().isUnauthorized());

        String body = """
                {
                  "name": "Account",
                  "type": "CHECKING",
                  "institution": "Bank",
                  "initialBalance": 100.00
                }
                """;

        mockMvc.perform(
                        post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isUnauthorized());
    }

    private TestUser registerUser(String name)
            throws Exception {

        String email =
                "integration."
                        + UUID.randomUUID()
                        + "@example.com";

        String body = """
                {
                  "name": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(name, email, PASSWORD);

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        UUID userId = UUID.fromString(
                response.get("id").asText()
        );

        return new TestUser(
                userId,
                email,
                PASSWORD
        );
    }

    private String login(TestUser user)
            throws Exception {

        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(
                user.email(),
                user.password()
        );

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

    private UUID createAccount(
            String token,
            String name,
            String type,
            String institution,
            BigDecimal initialBalance
    ) throws Exception {

        String body = """
                {
                  "name": "%s",
                  "type": "%s",
                  "institution": "%s",
                  "initialBalance": %s
                }
                """.formatted(
                name,
                type,
                institution,
                initialBalance.toPlainString()
        );

        MvcResult result = mockMvc.perform(
                        post("/api/v1/accounts")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.initialBalance")
                        .exists())
                .andExpect(jsonPath("$.currentBalance")
                        .exists())
                .andReturn();

        return UUID.fromString(
                objectMapper
                        .readTree(
                                result.getResponse()
                                        .getContentAsString()
                        )
                        .get("id")
                        .asText()
        );
    }

    private record TestUser(
            UUID id,
            String email,
            String password
    ) {
    }
}
