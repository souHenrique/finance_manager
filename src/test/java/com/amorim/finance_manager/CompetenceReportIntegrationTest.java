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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@Transactional
class CompetenceReportIntegrationTest {

    private static final String ENDPOINT = "/api/v1/reports/competence";
    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 30);

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

    private TestUser userA;
    private TestUser userB;

    @BeforeEach
    void setUp() {
        userA = createUser("Competence User A");
        userB = createUser("Competence User B");
    }

    @Test
    void shouldCountCardPurchaseOnceWithoutCountingInvoicePaymentAsNewExpense() throws Exception {
        save(
                userA,
                TransactionType.CREDIT_CARD_PURCHASE,
                "1000.00",
                LocalDate.of(2026, 9, 10),
                transaction -> transaction.setEffectiveDate(null)
        );
        save(
                userA,
                TransactionType.CREDIT_CARD_PAYMENT,
                "1000.00",
                LocalDate.of(2026, 9, 20),
                transaction -> transaction.setEffectiveDate(LocalDate.of(2026, 9, 20))
        );

        JsonNode report = report(userA, START, END);

        assertMoney(report, "totalIncome", "0.00");
        assertMoney(report, "totalExpenses", "1000.00");
        assertMoney(report, "result", "-1000.00");
        assertThat(report.path("expenseCategories")).hasSize(1);
        assertMoney(report.path("expenseCategories").get(0), "amount", "1000.00");
    }

    @Test
    void shouldUseInclusiveCompetencePeriodStatusTypesCategoriesAndAuthenticatedUser() throws Exception {
        save(userA, TransactionType.INCOME, "1500.00", START, transaction ->
                transaction.setEffectiveDate(END.plusMonths(1)));
        save(userA, TransactionType.EXPENSE, "200.00", END, transaction ->
                transaction.setEffectiveDate(START.minusMonths(1)));
        save(userA, TransactionType.CREDIT_CARD_PURCHASE, "300.00", START.plusDays(10));

        save(userA, TransactionType.CREDIT_CARD_PAYMENT, "900.00", START.plusDays(15));
        save(userA, TransactionType.TRANSFER, "800.00", START.plusDays(15));
        save(userA, TransactionType.ADJUSTMENT, "700.00", START.plusDays(15));
        save(userA, TransactionType.INCOME, "600.00", START.plusDays(15), transaction ->
                transaction.setStatus(TransactionStatus.PENDING));
        save(userA, TransactionType.EXPENSE, "500.00", START.plusDays(15), transaction ->
                transaction.setStatus(TransactionStatus.CANCELLED));
        save(userA, TransactionType.INCOME, "400.00", START.minusDays(1));
        save(userA, TransactionType.EXPENSE, "400.00", END.plusDays(1));
        save(userB, TransactionType.INCOME, "9999.00", START.plusDays(1));
        save(userB, TransactionType.EXPENSE, "9999.00", START.plusDays(1));

        JsonNode report = report(userA, START, END);

        assertThat(report.properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder(
                        "startDate",
                        "endDate",
                        "totalIncome",
                        "totalExpenses",
                        "result",
                        "incomeCategories",
                        "expenseCategories"
                );
        assertThat(report.path("startDate").asString()).isEqualTo(START.toString());
        assertThat(report.path("endDate").asString()).isEqualTo(END.toString());
        assertMoney(report, "totalIncome", "1500.00");
        assertMoney(report, "totalExpenses", "500.00");
        assertMoney(report, "result", "1000.00");
        assertThat(report.path("incomeCategories")).hasSize(1);
        assertThat(report.path("incomeCategories").get(0).path("name").asString())
                .isEqualTo(userA.incomeCategoryName());
        assertThat(report.path("expenseCategories")).hasSize(1);
        assertThat(report.path("expenseCategories").get(0).path("name").asString())
                .isEqualTo(userA.expenseCategoryName());
        assertMoney(report.path("expenseCategories").get(0), "amount", "500.00");
        assertThat(report.toString()).doesNotContain(
                userB.id().toString(),
                userB.incomeCategoryName(),
                userB.expenseCategoryName()
        );
    }

    @Test
    void shouldReturnZeroTotalsAndEmptyCategoriesWithoutEligibleMovements() throws Exception {
        JsonNode report = report(userA, START, END);

        assertMoney(report, "totalIncome", "0.00");
        assertMoney(report, "totalExpenses", "0.00");
        assertMoney(report, "result", "0.00");
        assertThat(report.path("incomeCategories")).isEmpty();
        assertThat(report.path("expenseCategories")).isEmpty();
    }

    @Test
    void shouldRejectMissingAndReversedPeriods() throws Exception {
        mockMvc.perform(
                        get(ENDPOINT)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userA))
                                .queryParam("endDate", END.toString())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("startDate"));

        mockMvc.perform(
                        get(ENDPOINT)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userA))
                                .queryParam("startDate", END.toString())
                                .queryParam("endDate", START.toString())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REPORT_PERIOD"));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(
                        get(ENDPOINT)
                                .queryParam("startDate", START.toString())
                                .queryParam("endDate", END.toString())
                )
                .andExpect(status().isUnauthorized());
    }

    private JsonNode report(TestUser user, LocalDate startDate, LocalDate endDate) throws Exception {
        String json = mockMvc.perform(
                        get(ENDPOINT)
                                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                                .queryParam("startDate", startDate.toString())
                                .queryParam("endDate", endDate.toString())
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readTree(json);
    }

    private Transaction save(
            TestUser owner,
            TransactionType type,
            String amount,
            LocalDate competenceDate
    ) {
        return save(owner, type, amount, competenceDate, transaction -> { });
    }

    private Transaction save(
            TestUser owner,
            TransactionType type,
            String amount,
            LocalDate competenceDate,
            Consumer<Transaction> customize
    ) {
        Transaction transaction = new Transaction();
        transaction.setUserId(owner.id());
        transaction.setDescription("Competence report fixture " + type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCompetenceDate(competenceDate);
        transaction.setEffectiveDate(competenceDate);
        transaction.setDueDate(competenceDate);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setPaymentMethod(PaymentMethod.PIX);

        switch (type) {
            case INCOME -> {
                transaction.setDestinationAccountId(owner.accountId());
                transaction.setCategoryId(owner.incomeCategoryId());
            }
            case EXPENSE -> {
                transaction.setSourceAccountId(owner.accountId());
                transaction.setCategoryId(owner.expenseCategoryId());
            }
            case CREDIT_CARD_PURCHASE -> {
                transaction.setPaymentMethod(PaymentMethod.CREDIT_CARD);
                transaction.setCategoryId(owner.expenseCategoryId());
            }
            case CREDIT_CARD_PAYMENT -> transaction.setSourceAccountId(owner.accountId());
            case TRANSFER -> {
                transaction.setSourceAccountId(owner.accountId());
                transaction.setDestinationAccountId(owner.otherAccountId());
                transaction.setPaymentMethod(PaymentMethod.TRANSFER);
            }
            case ADJUSTMENT -> transaction.setDestinationAccountId(owner.accountId());
        }

        customize.accept(transaction);
        return transactionRepository.saveAndFlush(transaction);
    }

    private TestUser createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("competence-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        User saved = userRepository.saveAndFlush(user);
        String token = jwtService.generateToken(
                userDetailsService.loadUserByUsername(saved.getEmail())
        );
        Account mainAccount = createAccount(saved.getId(), "Main");
        Account otherAccount = createAccount(saved.getId(), "Secondary");
        Category incomeCategory = createCategory(saved.getId(), name + " income", CategoryType.INCOME);
        Category expenseCategory = createCategory(saved.getId(), name + " expense", CategoryType.EXPENSE);

        return new TestUser(
                saved.getId(),
                token,
                mainAccount.getId(),
                otherAccount.getId(),
                incomeCategory.getId(),
                incomeCategory.getName(),
                expenseCategory.getId(),
                expenseCategory.getName()
        );
    }

    private Account createAccount(UUID userId, String name) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName(name);
        account.setType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setInitialBalance(new BigDecimal("1000.00"));
        account.setCurrentBalance(new BigDecimal("1000.00"));
        return accountRepository.saveAndFlush(account);
    }

    private Category createCategory(UUID userId, String name, CategoryType type) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setType(type);
        category.setStatus(CategoryStatus.ACTIVE);
        return categoryRepository.saveAndFlush(category);
    }

    private String bearer(TestUser user) {
        return "Bearer " + user.token();
    }

    private void assertMoney(JsonNode node, String field, String expected) {
        assertThat(node.path(field).isNumber()).as(field).isTrue();
        assertThat(node.path(field).decimalValue()).as(field).isEqualByComparingTo(expected);
    }

    private record TestUser(
            UUID id,
            String token,
            UUID accountId,
            UUID otherAccountId,
            UUID incomeCategoryId,
            String incomeCategoryName,
            UUID expenseCategoryId,
            String expenseCategoryName
    ) {
    }
}
