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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class TransactionMutationIntegrationTest {

    private static final LocalDate TRANSACTION_DATE =
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
    void shouldEditCompletedIncomeAmountWithoutDuplicatingPreviousImpact() throws Exception {
        TestUser user = createUser("Edit Amount User");
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
                TRANSACTION_DATE
        );

        mockMvc.perform(
                        patch("/api/v1/transactions/{id}", transactionId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("amount", new BigDecimal("80.00"))))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(80.00));

        assertBalance(account.getId(), "180.00");
        assertThat(transactionRepository.findById(transactionId).orElseThrow().getAmount())
                .isEqualByComparingTo("80.00");
    }

    @Test
    void shouldMoveCompletedExpenseImpactToAnotherAccount() throws Exception {
        TestUser user = createUser("Change Account User");
        Account originalAccount = createAccount(user.id(), "Original", "100.00", AccountStatus.ACTIVE);
        Account newAccount = createAccount(user.id(), "New", "200.00", AccountStatus.ACTIVE);
        Category category = createCategory(user.id(), "Food", CategoryType.EXPENSE);
        UUID transactionId = createTransaction(
                user.token(),
                TransactionType.EXPENSE,
                TransactionStatus.COMPLETED,
                PaymentMethod.DEBIT,
                new BigDecimal("40.00"),
                originalAccount.getId(),
                null,
                category.getId(),
                TRANSACTION_DATE
        );

        mockMvc.perform(
                        patch("/api/v1/transactions/{id}", transactionId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("sourceAccountId", newAccount.getId().toString())))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceAccountId").value(newAccount.getId().toString()));

        assertBalance(originalAccount.getId(), "100.00");
        assertBalance(newAccount.getId(), "160.00");
    }

    @Test
    void shouldChangeCompletedTransactionToPendingAndReverseImpact() throws Exception {
        TestUser user = createUser("Completed To Pending User");
        Account account = createAccount(user.id(), "Checking", "100.00", AccountStatus.ACTIVE);
        Category category = createCategory(user.id(), "Bills", CategoryType.EXPENSE);
        UUID transactionId = createTransaction(
                user.token(),
                TransactionType.EXPENSE,
                TransactionStatus.COMPLETED,
                PaymentMethod.PIX,
                new BigDecimal("30.00"),
                account.getId(),
                null,
                category.getId(),
                TRANSACTION_DATE
        );

        mockMvc.perform(
                        patch("/api/v1/transactions/{id}", transactionId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("status", "PENDING")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.effectiveDate").doesNotExist());

        assertBalance(account.getId(), "100.00");
    }

    @Test
    void shouldChangePendingTransactionToCompletedAndApplyImpactOnce() throws Exception {
        TestUser user = createUser("Pending To Completed User");
        Account account = createAccount(user.id(), "Checking", "100.00", AccountStatus.ACTIVE);
        Category category = createCategory(user.id(), "Bills", CategoryType.EXPENSE);
        UUID transactionId = createTransaction(
                user.token(),
                TransactionType.EXPENSE,
                TransactionStatus.PENDING,
                PaymentMethod.PIX,
                new BigDecimal("30.00"),
                account.getId(),
                null,
                category.getId(),
                null
        );

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("status", "COMPLETED");
        update.put("effectiveDate", TRANSACTION_DATE.toString());

        mockMvc.perform(
                        patch("/api/v1/transactions/{id}", transactionId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(update))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.effectiveDate").value(TRANSACTION_DATE.toString()));

        assertBalance(account.getId(), "70.00");
    }

    @Test
    void shouldChangeCategoryWithoutChangingNetBalance() throws Exception {
        TestUser user = createUser("Change Category User");
        Account account = createAccount(user.id(), "Wallet", "100.00", AccountStatus.ACTIVE);
        Category oldCategory = createCategory(user.id(), "Salary", CategoryType.INCOME);
        Category newCategory = createCategory(user.id(), "Bonus", CategoryType.INCOME);
        UUID transactionId = createTransaction(
                user.token(),
                TransactionType.INCOME,
                TransactionStatus.COMPLETED,
                PaymentMethod.CASH,
                new BigDecimal("50.00"),
                null,
                account.getId(),
                oldCategory.getId(),
                TRANSACTION_DATE
        );

        mockMvc.perform(
                        patch("/api/v1/transactions/{id}", transactionId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("categoryId", newCategory.getId().toString())))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(newCategory.getId().toString()));

        assertBalance(account.getId(), "150.00");
    }

    @Test
    void shouldCancelCompletedIncomeAndPreserveTransaction() throws Exception {
        TestUser user = createUser("Cancel Income User");
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
                TRANSACTION_DATE
        );

        cancel(user.token(), transactionId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertBalance(account.getId(), "100.00");
        assertThat(transactionRepository.findById(transactionId)).isPresent();
    }

    @Test
    void shouldCancelCompletedExpenseAndRestoreBalance() throws Exception {
        TestUser user = createUser("Cancel Expense User");
        Account account = createAccount(user.id(), "Checking", "100.00", AccountStatus.ACTIVE);
        Category category = createCategory(user.id(), "Food", CategoryType.EXPENSE);
        UUID transactionId = createTransaction(
                user.token(),
                TransactionType.EXPENSE,
                TransactionStatus.COMPLETED,
                PaymentMethod.DEBIT,
                new BigDecimal("40.00"),
                account.getId(),
                null,
                category.getId(),
                TRANSACTION_DATE
        );

        cancel(user.token(), transactionId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertBalance(account.getId(), "100.00");
    }

    @Test
    void shouldCancelTransferAndRestoreBothAccounts() throws Exception {
        TestUser user = createUser("Cancel Transfer User");
        Account source = createAccount(user.id(), "Source", "100.00", AccountStatus.ACTIVE);
        Account destination = createAccount(user.id(), "Destination", "50.00", AccountStatus.ACTIVE);
        UUID transactionId = createTransfer(
                user.token(),
                source.getId(),
                destination.getId(),
                new BigDecimal("40.00")
        );

        cancel(user.token(), transactionId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertBalance(source.getId(), "100.00");
        assertBalance(destination.getId(), "50.00");
    }

    @Test
    void shouldRejectDuplicateCancellationWithoutChangingBalanceAgain() throws Exception {
        TestUser user = createUser("Duplicate Cancel User");
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
                TRANSACTION_DATE
        );

        cancel(user.token(), transactionId).andExpect(status().isOk());

        cancel(user.token(), transactionId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSACTION_ALREADY_CANCELLED"));

        assertBalance(account.getId(), "100.00");
        assertThat(transactionRepository.findById(transactionId).orElseThrow().getStatus())
                .isEqualTo(TransactionStatus.CANCELLED);
    }

    @Test
    void shouldHideTransactionFromAnotherUserDuringUpdateAndCancellation() throws Exception {
        TestUser owner = createUser("Transaction Owner");
        TestUser otherUser = createUser("Other User");
        Account account = createAccount(owner.id(), "Wallet", "100.00", AccountStatus.ACTIVE);
        Category category = createCategory(owner.id(), "Salary", CategoryType.INCOME);
        UUID transactionId = createTransaction(
                owner.token(),
                TransactionType.INCOME,
                TransactionStatus.COMPLETED,
                PaymentMethod.CASH,
                new BigDecimal("50.00"),
                null,
                account.getId(),
                category.getId(),
                TRANSACTION_DATE
        );

        mockMvc.perform(
                        patch("/api/v1/transactions/{id}", transactionId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(otherUser.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("amount", new BigDecimal("80.00"))))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));

        cancel(otherUser.token(), transactionId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));

        assertBalance(account.getId(), "150.00");
        assertThat(transactionRepository.findById(transactionId).orElseThrow().getStatus())
                .isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldRollbackOldImpactAndEntityChangesWhenNewAccountIsInactive() throws Exception {
        TestUser user = createUser("Rollback Edit User");
        Account originalAccount = createAccount(user.id(), "Original", "100.00", AccountStatus.ACTIVE);
        Account inactiveAccount = createAccount(user.id(), "Inactive", "25.00", AccountStatus.INACTIVE);
        Category category = createCategory(user.id(), "Salary", CategoryType.INCOME);
        UUID transactionId = createTransaction(
                user.token(),
                TransactionType.INCOME,
                TransactionStatus.COMPLETED,
                PaymentMethod.CASH,
                new BigDecimal("50.00"),
                null,
                originalAccount.getId(),
                category.getId(),
                TRANSACTION_DATE
        );

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("amount", new BigDecimal("80.00"));
        update.put("destinationAccountId", inactiveAccount.getId().toString());

        mockMvc.perform(
                        patch("/api/v1/transactions/{id}", transactionId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(update))
                )
                .andExpect(status().isConflict());

        assertBalance(originalAccount.getId(), "150.00");
        assertBalance(inactiveAccount.getId(), "25.00");

        Transaction unchanged = transactionRepository.findById(transactionId).orElseThrow();
        assertThat(unchanged.getAmount()).isEqualByComparingTo("50.00");
        assertThat(unchanged.getDestinationAccountId()).isEqualTo(originalAccount.getId());
        assertThat(unchanged.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldRejectUpdateAndCancellationWithoutJwt() throws Exception {
        UUID transactionId = UUID.randomUUID();

        mockMvc.perform(
                        patch("/api/v1/transactions/{id}", transactionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("amount", new BigDecimal("80.00"))))
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/transactions/{id}/cancel", transactionId))
                .andExpect(status().isUnauthorized());
    }

    private UUID createTransaction(
            String token,
            TransactionType type,
            TransactionStatus transactionStatus,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            UUID sourceAccountId,
            UUID destinationAccountId,
            UUID categoryId,
            LocalDate effectiveDate
    ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "Transaction mutation test");
        body.put("amount", amount);
        body.put("competenceDate", TRANSACTION_DATE.toString());
        body.put("type", type.name());
        body.put("status", transactionStatus.name());
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

        MvcResult result = mockMvc.perform(
                        post("/api/v1/transactions")
                                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(body))
                )
                .andExpect(status().isCreated())
                .andReturn();

        return responseId(result);
    }

    private UUID createTransfer(
            String token,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount
    ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceAccountId", sourceAccountId.toString());
        body.put("destinationAccountId", destinationAccountId.toString());
        body.put("amount", amount);
        body.put("date", TRANSACTION_DATE.toString());
        body.put("description", "Transfer mutation test");

        MvcResult result = mockMvc.perform(
                        post("/api/v1/transfers")
                                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(body))
                )
                .andExpect(status().isCreated())
                .andReturn();

        return responseId(result);
    }

    private org.springframework.test.web.servlet.ResultActions cancel(
            String token,
            UUID transactionId
    ) throws Exception {
        return mockMvc.perform(
                post("/api/v1/transactions/{id}/cancel", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        );
    }

    private UUID responseId(MvcResult result) throws UnsupportedEncodingException {
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return UUID.fromString(response.get("id").asText());
    }

    private TestUser createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("transaction-mutation-" + UUID.randomUUID() + "@example.com");
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
            AccountStatus accountStatus
    ) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName(name);
        account.setType(AccountType.CHECKING);
        account.setInstitution("Test Bank");
        account.setInitialBalance(new BigDecimal(balance));
        account.setCurrentBalance(new BigDecimal(balance));
        account.setStatus(accountStatus);

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

    private void assertBalance(UUID accountId, String expectedBalance) {
        assertThat(accountRepository.findById(accountId).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo(expectedBalance);
    }

    private String json(Object body) {
        return objectMapper.writeValueAsString(body);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record TestUser(UUID id, String token) {
    }
}
