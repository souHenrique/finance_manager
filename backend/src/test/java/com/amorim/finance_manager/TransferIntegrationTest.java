package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.security.JwtService;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import com.amorim.finance_manager.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class TransferIntegrationTest {

    private static final LocalDate TRANSFER_DATE =
            LocalDate.of(2026, 9, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    transactions,
                    categories,
                    accounts,
                    users
                CASCADE
                """
        );
    }

    @Test
    void shouldTransferBalanceAndPersistSingleNeutralTransaction() throws Exception {
        TestUser user = createUser("Transfer User");
        Account source = createAccount(user.id(), "Source", "100.00", AccountStatus.ACTIVE);
        Account destination = createAccount(user.id(), "Destination", "50.00", AccountStatus.ACTIVE);

        UUID transactionId = createTransfer(
                user.token(),
                source.getId(),
                destination.getId(),
                new BigDecimal("40.00")
        );

        Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account updatedDestination = accountRepository.findById(destination.getId()).orElseThrow();
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();
        List<Transaction> transactions = transactionRepository.findAll();

        assertThat(updatedSource.getCurrentBalance()).isEqualByComparingTo("60.00");
        assertThat(updatedDestination.getCurrentBalance()).isEqualByComparingTo("90.00");

        assertThat(transactions).hasSize(1);
        assertThat(transaction.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.getPaymentMethod()).isEqualTo(PaymentMethod.TRANSFER);
        assertThat(transaction.getSourceAccountId()).isEqualTo(source.getId());
        assertThat(transaction.getDestinationAccountId()).isEqualTo(destination.getId());
        assertThat(transaction.getCategoryId()).isNull();
        assertThat(transaction.getCompetenceDate()).isEqualTo(TRANSFER_DATE);
        assertThat(transaction.getEffectiveDate()).isEqualTo(TRANSFER_DATE);

        assertThat(transactions)
                .noneMatch(item -> item.getType() == TransactionType.INCOME)
                .noneMatch(item -> item.getType() == TransactionType.EXPENSE);
    }

    @Test
    void shouldAllowSourceAccountToBecomeNegative() throws Exception {
        TestUser user = createUser("Negative Balance User");
        Account source = createAccount(user.id(), "Source", "100.00", AccountStatus.ACTIVE);
        Account destination = createAccount(user.id(), "Destination", "50.00", AccountStatus.ACTIVE);

        createTransfer(
                user.token(),
                source.getId(),
                destination.getId(),
                new BigDecimal("120.00")
        );

        assertThat(accountRepository.findById(source.getId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("-20.00");
        assertThat(accountRepository.findById(destination.getId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("170.00");
    }

    @Test
    void shouldRejectTransferBetweenSameAccount() throws Exception {
        TestUser user = createUser("Same Account User");
        Account account = createAccount(user.id(), "Account", "100.00", AccountStatus.ACTIVE);

        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferBody(
                                        account.getId(),
                                        account.getId(),
                                        new BigDecimal("20.00")
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSFER"));

        assertThat(accountRepository.findById(account.getId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("100.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectSourceAccountOwnedByAnotherUser() throws Exception {
        TestUser userA = createUser("User A");
        TestUser userB = createUser("User B");
        Account sourceFromB = createAccount(userB.id(), "B Source", "100.00", AccountStatus.ACTIVE);
        Account destinationFromA = createAccount(userA.id(), "A Destination", "50.00", AccountStatus.ACTIVE);

        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header(HttpHeaders.AUTHORIZATION, bearer(userA.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferBody(
                                        sourceFromB.getId(),
                                        destinationFromA.getId(),
                                        new BigDecimal("40.00")
                                ))
                )
                .andExpect(status().isNotFound());

        assertBalances(sourceFromB.getId(), "100.00", destinationFromA.getId(), "50.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRollbackDebitWhenDestinationBelongsToAnotherUser() throws Exception {
        TestUser userA = createUser("User A");
        TestUser userB = createUser("User B");
        Account sourceFromA = createAccount(userA.id(), "A Source", "100.00", AccountStatus.ACTIVE);
        Account destinationFromB = createAccount(userB.id(), "B Destination", "50.00", AccountStatus.ACTIVE);

        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header(HttpHeaders.AUTHORIZATION, bearer(userA.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferBody(
                                        sourceFromA.getId(),
                                        destinationFromB.getId(),
                                        new BigDecimal("40.00")
                                ))
                )
                .andExpect(status().isNotFound());

        assertBalances(sourceFromA.getId(), "100.00", destinationFromB.getId(), "50.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectInactiveSourceAccount() throws Exception {
        TestUser user = createUser("Inactive Source User");
        Account source = createAccount(user.id(), "Inactive Source", "100.00", AccountStatus.INACTIVE);
        Account destination = createAccount(user.id(), "Destination", "50.00", AccountStatus.ACTIVE);

        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferBody(
                                        source.getId(),
                                        destination.getId(),
                                        new BigDecimal("40.00")
                                ))
                )
                .andExpect(status().isConflict());

        assertBalances(source.getId(), "100.00", destination.getId(), "50.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRollbackSourceDebitWhenDestinationCreditFails() throws Exception {
        TestUser user = createUser("Rollback User");
        Account source = createAccount(user.id(), "Source", "100.00", AccountStatus.ACTIVE);
        Account destination = createAccount(user.id(), "Inactive Destination", "50.00", AccountStatus.INACTIVE);

        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferBody(
                                        source.getId(),
                                        destination.getId(),
                                        new BigDecimal("40.00")
                                ))
                )
                .andExpect(status().isConflict());

        assertBalances(source.getId(), "100.00", destination.getId(), "50.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectZeroAndNegativeAmounts() throws Exception {
        TestUser user = createUser("Invalid Amount User");
        Account source = createAccount(user.id(), "Source", "100.00", AccountStatus.ACTIVE);
        Account destination = createAccount(user.id(), "Destination", "50.00", AccountStatus.ACTIVE);

        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferBody(
                                        source.getId(),
                                        destination.getId(),
                                        BigDecimal.ZERO
                                ))
                )
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferBody(
                                        source.getId(),
                                        destination.getId(),
                                        new BigDecimal("-1.00")
                                ))
                )
                .andExpect(status().isBadRequest());

        assertBalances(source.getId(), "100.00", destination.getId(), "50.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectTransferWithoutJwt() throws Exception {
        mockMvc.perform(
                        post("/api/v1/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferBody(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        new BigDecimal("20.00")
                                ))
                )
                .andExpect(status().isUnauthorized());
    }

    private UUID createTransfer(
            String token,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/transfers")
                                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferBody(
                                        sourceAccountId,
                                        destinationAccountId,
                                        amount
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentMethod").value("TRANSFER"))
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return UUID.fromString(response.get("id").asText());
    }

    private String transferBody(
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceAccountId", sourceAccountId.toString());
        body.put("destinationAccountId", destinationAccountId.toString());
        body.put("amount", amount);
        body.put("date", TRANSFER_DATE.toString());
        body.put("description", "Integration transfer");

        return objectMapper.writeValueAsString(body);
    }

    private TestUser createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("transfer-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");

        User saved = userRepository.saveAndFlush(user);
        String token = jwtService.generateToken(
                userDetailsService.loadUserByUsername(saved.getEmail())
        );

        return new TestUser(saved.getId(), token);
    }

    private Account createAccount(
            UUID userId,
            String name,
            String balance,
            AccountStatus status
    ) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName(name);
        account.setType(AccountType.CHECKING);
        account.setInstitution("Test Bank");
        account.setInitialBalance(new BigDecimal(balance));
        account.setCurrentBalance(new BigDecimal(balance));
        account.setStatus(status);

        return accountRepository.saveAndFlush(account);
    }

    private void assertBalances(
            UUID sourceAccountId,
            String expectedSourceBalance,
            UUID destinationAccountId,
            String expectedDestinationBalance
    ) {
        assertThat(accountRepository.findById(sourceAccountId).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo(expectedSourceBalance);
        assertThat(accountRepository.findById(destinationAccountId).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo(expectedDestinationBalance);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record TestUser(UUID id, String token) {
    }
}
