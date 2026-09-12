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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@Transactional
class MonthlyAnnualCashFlowReportIntegrationTest {

    private static final String BASE = "/api/v1/reports/cash/";

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
        userA = createUser("Monthly Annual User A");
        userB = createUser("Monthly Annual User B");
    }

    @Test
    void shouldReturnTheExactMonthlyPeriodWithTotalsAndCategories() throws Exception {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);
        save(userA, TransactionType.INCOME, "100.00", start);
        save(userA, TransactionType.INCOME, "50.00", end);
        save(userA, TransactionType.EXPENSE, "20.00", LocalDate.of(2026, 9, 15));
        save(userA, TransactionType.CREDIT_CARD_PAYMENT, "30.00", LocalDate.of(2026, 9, 20));
        save(userA, TransactionType.INCOME, "999.00", start.minusDays(1));
        save(userA, TransactionType.EXPENSE, "999.00", end.plusDays(1));
        save(userB, TransactionType.INCOME, "8888.00", start);

        JsonNode report = monthlyReport(userA, 2026, 9);

        assertFields(report, "year", "month", "startDate", "endDate", "summary");
        assertThat(report.path("year").asInt()).isEqualTo(2026);
        assertThat(report.path("month").asInt()).isEqualTo(9);
        assertThat(report.path("startDate").asString()).isEqualTo(start.toString());
        assertThat(report.path("endDate").asString()).isEqualTo(end.toString());
        JsonNode summary = report.path("summary");
        assertMonthlyTotals(summary, "150.00", "50.00", "30.00");
        assertThat(summary.path("incomeCategories")).hasSize(1);
        assertMoney(summary.path("incomeCategories").get(0), "amount", "150.00");
        assertThat(summary.path("expenseCategories")).hasSize(1);
        assertMoney(summary.path("expenseCategories").get(0), "amount", "20.00");
    }

    @Test
    void shouldReturnTwelveAnnualMonthsUsingEffectiveDateStatusTypeAndAuthenticatedUser() throws Exception {
        LocalDate january = LocalDate.of(2026, 1, 10);
        LocalDate february = LocalDate.of(2026, 2, 20);
        LocalDate december = LocalDate.of(2026, 12, 31);
        save(userA, TransactionType.INCOME, "100.00", january);
        save(userA, TransactionType.EXPENSE, "30.00", january);
        save(userA, TransactionType.CREDIT_CARD_PAYMENT, "20.00", february);
        save(userA, TransactionType.INCOME, "50.00", december);
        save(userA, TransactionType.TRANSFER, "700.00", january);
        save(userA, TransactionType.CREDIT_CARD_PURCHASE, "600.00", january);
        save(userA, TransactionType.ADJUSTMENT, "500.00", january);
        save(userA, TransactionType.INCOME, "400.00", january,
                transaction -> transaction.setStatus(TransactionStatus.PENDING));
        save(userA, TransactionType.EXPENSE, "300.00", january,
                transaction -> transaction.setStatus(TransactionStatus.CANCELLED));
        save(userB, TransactionType.INCOME, "9999.00", january);
        save(userA, TransactionType.INCOME, "8888.00", LocalDate.of(2025, 12, 31));
        save(userA, TransactionType.EXPENSE, "7777.00", LocalDate.of(2027, 1, 1));

        JsonNode report = annualReport(userA, 2026);

        assertFields(report, "year", "startDate", "endDate", "evolution");
        assertThat(report.path("year").asInt()).isEqualTo(2026);
        assertThat(report.path("startDate").asString()).isEqualTo("2026-01-01");
        assertThat(report.path("endDate").asString()).isEqualTo("2026-12-31");
        JsonNode evolution = report.path("evolution");
        assertThat(evolution.isArray()).isTrue();
        assertThat(evolution).hasSize(12);
        for (int index = 0; index < 12; index++) {
            assertFields(evolution.get(index), "month", "totals");
            assertThat(evolution.get(index).path("month").asInt()).isEqualTo(index + 1);
        }
        assertAnnualTotals(evolution.get(0).path("totals"), "100.00", "30.00");
        assertAnnualTotals(evolution.get(1).path("totals"), "0.00", "20.00");
        assertAnnualTotals(evolution.get(2).path("totals"), "0.00", "0.00");
        assertAnnualTotals(evolution.get(11).path("totals"), "50.00", "0.00");
    }

    @Test
    void shouldHandleFebruaryInALeapYearForTheMonthlyReport() throws Exception {
        LocalDate leapDay = LocalDate.of(2024, 2, 29);
        save(userA, TransactionType.INCOME, "29.00", leapDay);
        save(userA, TransactionType.INCOME, "1.00", leapDay.plusDays(1));

        JsonNode report = monthlyReport(userA, 2024, 2);

        assertThat(report.path("startDate").asString()).isEqualTo("2024-02-01");
        assertThat(report.path("endDate").asString()).isEqualTo("2024-02-29");
        assertMonthlyTotals(report.path("summary"), "29.00", "0.00", "0.00");
    }

    @Test
    void shouldReturnZeroMonthlyAndAnnualReportsWithoutEligibleData() throws Exception {
        JsonNode monthly = monthlyReport(userA, 2026, 9);
        assertMonthlyTotals(monthly.path("summary"), "0.00", "0.00", "0.00");
        assertThat(monthly.path("summary").path("incomeCategories")).isEmpty();
        assertThat(monthly.path("summary").path("expenseCategories")).isEmpty();

        JsonNode annual = annualReport(userA, 2026);
        assertThat(annual.path("evolution")).hasSize(12);
        for (JsonNode month : annual.path("evolution")) {
            assertAnnualTotals(month.path("totals"), "0.00", "0.00");
        }
    }

    @Test
    void shouldRejectMissingMonthlyAndAnnualPeriodsWithFieldErrors() throws Exception {
        ResultActions monthly = mockMvc.perform(get(BASE + "monthly")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token()));
        assertApiError(monthly, "monthly", "VALIDATION_ERROR");
        monthly.andExpect(jsonPath("$.fieldErrors[0].field").value("month"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("O mês é obrigatório"))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("year"))
                .andExpect(jsonPath("$.fieldErrors[1].message").value("O ano é obrigatório"));

        ResultActions annual = mockMvc.perform(get(BASE + "annual")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token()));
        assertApiError(annual, "annual", "VALIDATION_ERROR");
        annual.andExpect(jsonPath("$.fieldErrors[0].field").value("year"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("O ano é obrigatório"));
    }

    @ParameterizedTest
    @CsvSource({"0,9,year", "10000,9,year", "2026,0,month", "2026,13,month"})
    void shouldRejectMonthlyPeriodOutsideTheSupportedLimits(
            String year,
            String month,
            String invalidField
    ) throws Exception {
        ResultActions response = mockMvc.perform(get(BASE + "monthly")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                .queryParam("year", year)
                .queryParam("month", month));

        assertApiError(response, "monthly", "VALIDATION_ERROR");
        response.andExpect(jsonPath("$.fieldErrors[0].field").value(invalidField));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "10000"})
    void shouldRejectAnnualYearOutsideTheSupportedLimits(String year) throws Exception {
        ResultActions response = mockMvc.perform(get(BASE + "annual")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                .queryParam("year", year));

        assertApiError(response, "annual", "VALIDATION_ERROR");
        response.andExpect(jsonPath("$.fieldErrors[0].field").value("year"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"monthly", "annual"})
    void shouldRequireAValidJwt(String period) throws Exception {
        for (String token : List.of("", "Bearer invalid-token")) {
            MockHttpServletRequestBuilder request = get(BASE + period)
                    .queryParam("year", "2026");
            if (period.equals("monthly")) {
                request.queryParam("month", "9");
            }
            if (!token.isEmpty()) {
                request.header(HttpHeaders.AUTHORIZATION, token);
            }

            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.path").value(BASE + period));
        }
    }

    private JsonNode monthlyReport(TestUser user, int year, int month) throws Exception {
        return read(mockMvc.perform(get(BASE + "monthly")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token())
                .queryParam("year", Integer.toString(year))
                .queryParam("month", Integer.toString(month))));
    }

    private JsonNode annualReport(TestUser user, int year) throws Exception {
        return read(mockMvc.perform(get(BASE + "annual")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token())
                .queryParam("year", Integer.toString(year))));
    }

    private JsonNode read(ResultActions response) throws Exception {
        return objectMapper.readTree(response.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void assertMonthlyTotals(
            JsonNode summary,
            String inflows,
            String outflows,
            String invoices
    ) {
        assertFields(
                summary,
                "inflows",
                "outflows",
                "net",
                "invoicePayments",
                "incomeCategories",
                "expenseCategories"
        );
        assertMoney(summary, "inflows", inflows);
        assertMoney(summary, "outflows", outflows);
        assertMoney(
                summary,
                "net",
                new BigDecimal(inflows).subtract(new BigDecimal(outflows)).toPlainString()
        );
        assertMoney(summary, "invoicePayments", invoices);
    }

    private void assertAnnualTotals(JsonNode totals, String inflows, String outflows) {
        assertFields(totals, "inflows", "outflows", "net");
        assertMoney(totals, "inflows", inflows);
        assertMoney(totals, "outflows", outflows);
        assertMoney(
                totals,
                "net",
                new BigDecimal(inflows).subtract(new BigDecimal(outflows)).toPlainString()
        );
    }

    private void assertMoney(JsonNode node, String field, String expected) {
        assertThat(node.path(field).isNumber()).as(field).isTrue();
        assertThat(node.path(field).decimalValue()).as(field).isEqualByComparingTo(expected);
    }

    private void assertFields(JsonNode node, String... fields) {
        assertThat(node.properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder(fields);
    }

    private void assertApiError(ResultActions response, String period, String code) throws Exception {
        String json = response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.path").value(BASE + period))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode error = objectMapper.readTree(json);
        assertFields(error, "timestamp", "status", "code", "message", "path", "fieldErrors");
        assertThat(Instant.parse(error.path("timestamp").asString())).isNotNull();
        assertThat(error.path("message").asString()).isNotBlank();
    }

    private Transaction save(TestUser owner, TransactionType type, String amount, LocalDate date) {
        return save(owner, type, amount, date, transaction -> { });
    }

    private Transaction save(
            TestUser owner,
            TransactionType type,
            String amount,
            LocalDate date,
            Consumer<Transaction> customize
    ) {
        Transaction transaction = new Transaction();
        transaction.setUserId(owner.id());
        transaction.setDescription("Monthly/annual cash fixture " + type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCompetenceDate(LocalDate.of(2026, 9, 3));
        transaction.setEffectiveDate(date);
        transaction.setDueDate(LocalDate.of(2026, 9, 3));
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
            case TRANSFER -> {
                transaction.setSourceAccountId(owner.accountId());
                transaction.setDestinationAccountId(owner.otherAccountId());
                transaction.setPaymentMethod(PaymentMethod.TRANSFER);
            }
            case CREDIT_CARD_PURCHASE, CREDIT_CARD_PAYMENT -> {
                if (type == TransactionType.CREDIT_CARD_PURCHASE) {
                    transaction.setPaymentMethod(PaymentMethod.CREDIT_CARD);
                    transaction.setCategoryId(owner.expenseCategoryId());
                } else {
                    transaction.setSourceAccountId(owner.accountId());
                }
            }
            case ADJUSTMENT -> transaction.setDestinationAccountId(owner.accountId());
        }
        customize.accept(transaction);
        return transactionRepository.saveAndFlush(transaction);
    }

    private TestUser createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("monthly-annual-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        User saved = userRepository.saveAndFlush(user);
        String token = jwtService.generateToken(
                userDetailsService.loadUserByUsername(saved.getEmail())
        );
        return new TestUser(
                saved.getId(),
                token,
                createAccount(saved.getId(), "Main").getId(),
                createAccount(saved.getId(), "Secondary").getId(),
                createCategory(saved.getId(), name + " income", CategoryType.INCOME).getId(),
                createCategory(saved.getId(), name + " expense", CategoryType.EXPENSE).getId()
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

    private record TestUser(
            UUID id,
            String token,
            UUID accountId,
            UUID otherAccountId,
            UUID incomeCategoryId,
            UUID expenseCategoryId
    ) {
    }
}
