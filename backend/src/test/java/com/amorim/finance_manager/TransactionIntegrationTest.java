package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class TransactionIntegrationTest {

    private static final LocalDate COMPETENCE_DATE =
            LocalDate.of(2026, 8, 31);

    private static final LocalDate EFFECTIVE_DATE =
            LocalDate.of(2026, 8, 31);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
    void shouldCreateCompletedIncomeAndIncreaseBalance() throws Exception {
        TestUser user = createUser("Income User");
        Account account = createAccount(user.id(), "Wallet", "100.00", AccountStatus.ACTIVE);
        Category category = createCategory(user.id(), "Salary", CategoryType.INCOME);

        UUID transactionId = createTransaction(
                user.token(),
                TransactionType.INCOME,
                TransactionStatus.COMPLETED,
                PaymentMethod.CASH,
                new BigDecimal("50.00"),
                null,
                account.getId(),
                category.getId(),
                EFFECTIVE_DATE
        );

        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();

        assertThat(updatedAccount.getCurrentBalance()).isEqualByComparingTo("150.00");
        assertThat(transaction.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.getDestinationAccountId()).isEqualTo(account.getId());
    }

    @Test
    void shouldCreateCompletedExpenseAndDecreaseBalance() throws Exception {
        TestUser user = createUser("Expense User");
        Account account = createAccount(user.id(), "Checking", "100.00", AccountStatus.ACTIVE);
        Category category = createCategory(user.id(), "Food", CategoryType.EXPENSE);

        UUID transactionId = createTransaction(
                user.token(),
                TransactionType.EXPENSE,
                TransactionStatus.COMPLETED,
                PaymentMethod.DEBIT,
                new BigDecimal("150.00"),
                account.getId(),
                null,
                category.getId(),
                EFFECTIVE_DATE
        );

        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();

        assertThat(updatedAccount.getCurrentBalance()).isEqualByComparingTo("-50.00");
        assertThat(transaction.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(transaction.getSourceAccountId()).isEqualTo(account.getId());
    }

    @Test
    void shouldDebitBalanceForSentPix() throws Exception {
        TestUser user = createUser("Sent Pix User");
        Account account = createAccount(user.id(), "Checking", "200.00", AccountStatus.ACTIVE);
        Category category = createCategory(user.id(), "Services", CategoryType.EXPENSE);

        createTransaction(
                user.token(),
                TransactionType.EXPENSE,
                TransactionStatus.COMPLETED,
                PaymentMethod.PIX,
                new BigDecimal("35.25"),
                account.getId(),
                null,
                category.getId(),
                EFFECTIVE_DATE
        );

        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updatedAccount.getCurrentBalance()).isEqualByComparingTo("164.75");
    }

    @Test
    void shouldCreditBalanceForReceivedPix() throws Exception {
        TestUser user = createUser("Received Pix User");
        Account account = createAccount(user.id(), "Checking", "200.00", AccountStatus.ACTIVE);
        Category category = createCategory(user.id(), "Refund", CategoryType.INCOME);

        createTransaction(
                user.token(),
                TransactionType.INCOME,
                TransactionStatus.COMPLETED,
                PaymentMethod.PIX,
                new BigDecimal("35.25"),
                null,
                account.getId(),
                category.getId(),
                EFFECTIVE_DATE
        );

        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updatedAccount.getCurrentBalance()).isEqualByComparingTo("235.25");
    }

    @Test
    void shouldPersistPendingTransactionWithoutChangingBalance() throws Exception {
        TestUser user = createUser("Pending User");
        Account account = createAccount(user.id(), "Checking", "100.00", AccountStatus.ACTIVE);
        Category category = createCategory(user.id(), "Pending Bill", CategoryType.EXPENSE);

        UUID transactionId = createTransaction(
                user.token(),
                TransactionType.EXPENSE,
                TransactionStatus.PENDING,
                PaymentMethod.PIX,
                new BigDecimal("40.00"),
                account.getId(),
                null,
                category.getId(),
                null
        );

        Account unchangedAccount = accountRepository.findById(account.getId()).orElseThrow();
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();

        assertThat(unchangedAccount.getCurrentBalance()).isEqualByComparingTo("100.00");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(transaction.getEffectiveDate()).isNull();
    }

    @Test
    void shouldRejectIncompatibleCategory() throws Exception {
        TestUser user = createUser("Category User");
        Account account = createAccount(user.id(), "Checking", "100.00", AccountStatus.ACTIVE);
        Category expenseCategory = createCategory(user.id(), "Food", CategoryType.EXPENSE);

        mockMvc.perform(
                        post("/api/v1/transactions")
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transactionBody(
                                        TransactionType.INCOME,
                                        TransactionStatus.COMPLETED,
                                        PaymentMethod.PIX,
                                        new BigDecimal("20.00"),
                                        null,
                                        account.getId(),
                                        expenseCategory.getId(),
                                        EFFECTIVE_DATE
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CATEGORY_TYPE_MISMATCH"));

        assertThat(transactionRepository.count()).isZero();
        assertThat(accountRepository.findById(account.getId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRejectPendingTransactionForInactiveAccount() throws Exception {
        TestUser user = createUser("Inactive Account User");
        Account account = createAccount(user.id(), "Inactive", "100.00", AccountStatus.INACTIVE);
        Category category = createCategory(user.id(), "Bills", CategoryType.EXPENSE);

        mockMvc.perform(
                        post("/api/v1/transactions")
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transactionBody(
                                        TransactionType.EXPENSE,
                                        TransactionStatus.PENDING,
                                        PaymentMethod.DEBIT,
                                        new BigDecimal("20.00"),
                                        account.getId(),
                                        null,
                                        category.getId(),
                                        null
                                ))
                )
                .andExpect(status().isConflict());

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectAccountOwnedByAnotherUser() throws Exception {
        TestUser userA = createUser("User A");
        TestUser userB = createUser("User B");
        Account accountFromB = createAccount(userB.id(), "B Account", "100.00", AccountStatus.ACTIVE);
        Category categoryFromA = createCategory(userA.id(), "Food", CategoryType.EXPENSE);

        mockMvc.perform(
                        post("/api/v1/transactions")
                                .header(HttpHeaders.AUTHORIZATION, bearer(userA.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transactionBody(
                                        TransactionType.EXPENSE,
                                        TransactionStatus.COMPLETED,
                                        PaymentMethod.PIX,
                                        new BigDecimal("20.00"),
                                        accountFromB.getId(),
                                        null,
                                        categoryFromA.getId(),
                                        EFFECTIVE_DATE
                                ))
                )
                .andExpect(status().isNotFound());

        assertThat(transactionRepository.count()).isZero();
        assertThat(accountRepository.findById(accountFromB.getId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRejectCategoryOwnedByAnotherUser() throws Exception {
        TestUser userA = createUser("User A");
        TestUser userB = createUser("User B");
        Account accountFromA = createAccount(userA.id(), "A Account", "100.00", AccountStatus.ACTIVE);
        Category categoryFromB = createCategory(userB.id(), "Food", CategoryType.EXPENSE);

        mockMvc.perform(
                        post("/api/v1/transactions")
                                .header(HttpHeaders.AUTHORIZATION, bearer(userA.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transactionBody(
                                        TransactionType.EXPENSE,
                                        TransactionStatus.COMPLETED,
                                        PaymentMethod.PIX,
                                        new BigDecimal("20.00"),
                                        accountFromA.getId(),
                                        null,
                                        categoryFromB.getId(),
                                        EFFECTIVE_DATE
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        assertThat(transactionRepository.count()).isZero();
        assertThat(accountRepository.findById(accountFromA.getId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReturnOnlyTransactionOwnedByAuthenticatedUser() throws Exception {
        TestUser userA = createUser("User A");
        TestUser userB = createUser("User B");
        Account account = createAccount(userA.id(), "A Account", "100.00", AccountStatus.ACTIVE);
        Category category = createCategory(userA.id(), "Salary", CategoryType.INCOME);

        UUID transactionId = createTransaction(
                userA.token(),
                TransactionType.INCOME,
                TransactionStatus.COMPLETED,
                PaymentMethod.TRANSFER,
                new BigDecimal("20.00"),
                null,
                account.getId(),
                category.getId(),
                EFFECTIVE_DATE
        );

        mockMvc.perform(
                        get("/api/v1/transactions/{id}", transactionId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userA.token()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.type").value("INCOME"));

        mockMvc.perform(
                        get("/api/v1/transactions/{id}", transactionId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userB.token()))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
    }

    @Test
    void shouldRejectTransactionEndpointsWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/v1/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isUnauthorized());
    }

    private UUID createTransaction(
            String token,
            TransactionType type,
            TransactionStatus status,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            UUID sourceAccountId,
            UUID destinationAccountId,
            UUID categoryId,
            LocalDate effectiveDate
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/transactions")
                                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transactionBody(
                                        type,
                                        status,
                                        paymentMethod,
                                        amount,
                                        sourceAccountId,
                                        destinationAccountId,
                                        categoryId,
                                        effectiveDate
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(status.name()))
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return UUID.fromString(response.get("id").asText());
    }

    private String transactionBody(
            TransactionType type,
            TransactionStatus status,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            UUID sourceAccountId,
            UUID destinationAccountId,
            UUID categoryId,
            LocalDate effectiveDate
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "Integration transaction");
        body.put("amount", amount);
        body.put("competenceDate", COMPETENCE_DATE.toString());
        body.put("type", type.name());
        body.put("status", status.name());
        body.put("paymentMethod", paymentMethod.name());
        body.put("categoryId", categoryId.toString());

        if (effectiveDate != null) {
            body.put("effectiveDate", effectiveDate.toString());
        }

        if (sourceAccountId != null) {
            body.put("sourceAccountId", sourceAccountId.toString());
        }

        if (destinationAccountId != null) {
            body.put("destinationAccountId", destinationAccountId.toString());
        }

        return objectMapper.writeValueAsString(body);
    }

    private TestUser createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("transaction-" + UUID.randomUUID() + "@example.com");
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

    private Category createCategory(
            UUID userId,
            String name,
            CategoryType type
    ) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setType(type);
        category.setStatus(CategoryStatus.ACTIVE);

        return categoryRepository.saveAndFlush(category);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record TestUser(UUID id, String token) {
    }
}
