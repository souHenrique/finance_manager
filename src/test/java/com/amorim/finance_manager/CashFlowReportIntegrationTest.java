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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@Transactional
class CashFlowReportIntegrationTest {

    private static final String BASE = "/api/v1/reports/cash/";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 3);
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 31);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private JwtService jwtService;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private EntityManager entityManager;

    private TestUser userA;
    private TestUser userB;

    @BeforeEach
    void setUp() {
        userA = createUser("Cash User A");
        userB = createUser("Cash User B");
    }

    static Stream<Arguments> reportTypes() {
        return Stream.of("daily", "weekly").flatMap(period -> Arrays.stream(TransactionType.values())
                .map(type -> Arguments.of(period, type)));
    }

    @ParameterizedTest
    @MethodSource("reportTypes")
    void shouldClassifyEachTransactionTypeWithoutCountingPurchasesOrTransfers(
            String period, TransactionType type) throws Exception {
        // Card fixtures go through persistence: their creation flow is outside T19.
        // Purchases deliberately have effectiveDate, so excluding null dates alone cannot pass this test.
        save(userA, type, "27.12", DATE);

        JsonNode summary = summary(report(period, userA, DATE), period);
        String inflows = type == TransactionType.INCOME ? "27.12" : "0.00";
        String outflows = type == TransactionType.EXPENSE || type == TransactionType.CREDIT_CARD_PAYMENT
                ? "27.12" : "0.00";
        String invoices = type == TransactionType.CREDIT_CARD_PAYMENT ? "27.12" : "0.00";
        assertTotals(summary, inflows, outflows, invoices);
        assertThat(summary.path("incomeCategories").size()).isEqualTo(type == TransactionType.INCOME ? 1 : 0);
        assertThat(summary.path("expenseCategories").size()).isEqualTo(type == TransactionType.EXPENSE ? 1 : 0);
    }

    @ParameterizedTest
    @CsvSource({
            "INCOME,COMPLETED", "INCOME,PENDING", "INCOME,CANCELLED",
            "EXPENSE,COMPLETED", "EXPENSE,PENDING", "EXPENSE,CANCELLED",
            "CREDIT_CARD_PAYMENT,COMPLETED", "CREDIT_CARD_PAYMENT,PENDING", "CREDIT_CARD_PAYMENT,CANCELLED"
    })
    void shouldIncludeOnlyCompletedMovementsEvenWhenOtherStatusesHaveEffectiveDate(
            TransactionType type, TransactionStatus state) throws Exception {
        save(userA, type, "40.00", DATE, t -> t.setStatus(state));

        for (String period : List.of("daily", "weekly")) {
            boolean included = state == TransactionStatus.COMPLETED;
            assertTotals(summary(report(period, userA, DATE), period),
                    included && type == TransactionType.INCOME ? "40.00" : "0.00",
                    included && type != TransactionType.INCOME ? "40.00" : "0.00",
                    included && type == TransactionType.CREDIT_CARD_PAYMENT ? "40.00" : "0.00");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly"})
    void shouldExcludeCompletedMovementsWithoutEffectiveDate(String period) throws Exception {
        for (TransactionType type : List.of(TransactionType.INCOME, TransactionType.EXPENSE,
                TransactionType.CREDIT_CARD_PAYMENT)) {
            save(userA, type, "100.00", null);
        }

        assertTotals(summary(report(period, userA, DATE), period), "0.00", "0.00", "0.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly"})
    void shouldUseEffectiveDateInsteadOfCompetenceOrDueDate(String period) throws Exception {
        save(userA, TransactionType.INCOME, "200.00", DATE, t -> {
            t.setCompetenceDate(DATE.minusMonths(2));
            t.setDueDate(DATE.plusMonths(2));
        });
        save(userA, TransactionType.EXPENSE, "80.00", DATE, t -> t.setCompetenceDate(DATE.plusMonths(2)));
        save(userA, TransactionType.INCOME, "999.00", DATE.plusMonths(1));
        save(userA, TransactionType.EXPENSE, "999.00", DATE.minusMonths(1));

        assertTotals(summary(report(period, userA, DATE), period), "200.00", "80.00", "0.00");
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMethod.class, names = {"DEBIT", "PIX", "CASH", "TRANSFER", "OTHER"})
    void shouldClassifyIncomeAndExpenseByTypeNotPaymentMethod(PaymentMethod method) throws Exception {
        save(userA, TransactionType.INCOME, "100.10", DATE, t -> t.setPaymentMethod(method));
        save(userA, TransactionType.EXPENSE, "50.05", DATE, t -> {
            t.setPaymentMethod(method);
            if (method == PaymentMethod.CASH) t.setSourceAccountId(null);
        });

        assertTotals(summary(report("daily", userA, DATE), "daily"), "100.10", "50.05", "0.00");
    }

    @Test
    void shouldCountInvoicePaymentsExactlyOnceWithoutCountingPurchasesInExpenseCategories() throws Exception {
        save(userA, TransactionType.INCOME, "5000.00", DATE);
        save(userA, TransactionType.EXPENSE, "300.00", DATE);
        save(userA, TransactionType.CREDIT_CARD_PAYMENT, "1200.00", DATE);
        save(userA, TransactionType.CREDIT_CARD_PURCHASE, "1200.00", DATE);
        save(userA, TransactionType.TRANSFER, "900.00", DATE);

        for (String period : List.of("daily", "weekly")) {
            JsonNode summary = summary(report(period, userA, DATE), period);
            assertTotals(summary, "5000.00", "1500.00", "1200.00");
            assertThat(summary.path("expenseCategories").size()).isEqualTo(1);
            assertMoney(summary.path("expenseCategories").get(0), "amount", "300.00");
        }
    }

    @Test
    void shouldExcludeDaysBeforeAndAfterTheDailyReport() throws Exception {
        save(userA, TransactionType.INCOME, "10.00", DATE.minusDays(1));
        save(userA, TransactionType.INCOME, "20.00", DATE);
        save(userA, TransactionType.INCOME, "30.00", DATE.plusDays(1));

        JsonNode report = report("daily", userA, DATE);

        assertThat(report.path("date").asString()).isEqualTo(DATE.toString());
        assertTotals(report.path("summary"), "20.00", "0.00", "0.00");
        assertFields(report, "date", "summary");
    }

    @Test
    void shouldIncludeBothWeekBoundariesAndCompareCurrentMinusPrevious() throws Exception {
        save(userA, TransactionType.INCOME, "400.00", MONDAY.minusWeeks(1));
        save(userA, TransactionType.EXPENSE, "100.00", MONDAY.minusDays(1));
        save(userA, TransactionType.INCOME, "600.00", MONDAY);
        save(userA, TransactionType.EXPENSE, "50.00", MONDAY.plusDays(6));
        save(userA, TransactionType.CREDIT_CARD_PAYMENT, "200.00", DATE);
        save(userA, TransactionType.INCOME, "9999.00", MONDAY.minusDays(8));
        save(userA, TransactionType.INCOME, "9999.00", MONDAY.plusDays(7));

        JsonNode report = report("weekly", userA, DATE);

        assertFields(report, "currentWeek", "previousWeek", "comparison");
        assertPeriod(report.path("currentWeek"), MONDAY, MONDAY.plusDays(6));
        assertPeriod(report.path("previousWeek"), MONDAY.minusWeeks(1), MONDAY.minusDays(1));
        assertTotals(report.path("currentWeek").path("summary"), "600.00", "250.00", "200.00");
        assertTotals(report.path("previousWeek").path("summary"), "400.00", "100.00", "0.00");
        assertMoney(report.path("comparison"), "inflowsDifference", "200.00");
        assertMoney(report.path("comparison"), "outflowsDifference", "150.00");
        assertMoney(report.path("comparison"), "netDifference", "50.00");
    }

    @ParameterizedTest
    @CsvSource({
            "2026-08-31,2026-08-31,2026-09-06",
            "2026-09-06,2026-08-31,2026-09-06",
            "2026-01-01,2025-12-29,2026-01-04",
            "2024-02-29,2024-02-26,2024-03-03"
    })
    void shouldHandleWeekReferencesAndCalendarBoundaries(LocalDate date, LocalDate start, LocalDate end)
            throws Exception {
        save(userA, TransactionType.INCOME, "10.00", start);
        save(userA, TransactionType.EXPENSE, "2.00", end);

        JsonNode report = report("weekly", userA, date);

        assertPeriod(report.path("currentWeek"), start, end);
        assertPeriod(report.path("previousWeek"), start.minusWeeks(1), start.minusDays(1));
        assertTotals(report.path("currentWeek").path("summary"), "10.00", "2.00", "0.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly"})
    void shouldReturnOkWithZeroTotalsAndEmptyCategoriesWithoutEligibleData(String period) throws Exception {
        save(userB, TransactionType.INCOME, "999.00", DATE);

        JsonNode report = report(period, userA, DATE);
        JsonNode summary = summary(report, period);
        assertTotals(summary, "0.00", "0.00", "0.00");
        assertThat(summary.path("incomeCategories").isEmpty()).isTrue();
        assertThat(summary.path("expenseCategories").isEmpty()).isTrue();
        if (period.equals("weekly")) {
            assertTotals(report.path("previousWeek").path("summary"), "0.00", "0.00", "0.00");
            assertMoney(report.path("comparison"), "netDifference", "0.00");
        }
    }

    @Test
    void shouldCompareAnEmptyCurrentWeekWithPreviousWeekActivity() throws Exception {
        save(userA, TransactionType.INCOME, "100.00", MONDAY.minusDays(1));
        save(userA, TransactionType.EXPENSE, "30.00", MONDAY.minusDays(1));

        JsonNode report = report("weekly", userA, DATE);

        assertTotals(report.path("currentWeek").path("summary"), "0.00", "0.00", "0.00");
        assertTotals(report.path("previousWeek").path("summary"), "100.00", "30.00", "0.00");
        assertMoney(report.path("comparison"), "inflowsDifference", "-100.00");
        assertMoney(report.path("comparison"), "outflowsDifference", "-30.00");
        assertMoney(report.path("comparison"), "netDifference", "-70.00");
    }

    @Test
    void shouldGroupByIdKeepSubcategoriesSeparateAndRetainInactiveCategories() throws Exception {
        Category parent = categoryRepository.findById(userA.expenseCategoryId()).orElseThrow();
        parent.setStatus(CategoryStatus.INACTIVE);
        categoryRepository.saveAndFlush(parent);
        Category child = createCategory(userA.id(), "Child", CategoryType.EXPENSE);
        child.setParentCategoryId(parent.getId());
        categoryRepository.saveAndFlush(child);
        Category sameName = createCategory(userA.id(), parent.getName(), CategoryType.EXPENSE);
        save(userA, TransactionType.EXPENSE, "10.00", MONDAY);
        save(userA, TransactionType.EXPENSE, "20.00", DATE);
        save(userA, TransactionType.EXPENSE, "50.00", DATE, t -> t.setCategoryId(child.getId()));
        save(userA, TransactionType.EXPENSE, "5.00", DATE, t -> t.setCategoryId(sameName.getId()));

        JsonNode summary = summary(report("weekly", userA, DATE), "weekly");
        JsonNode categories = summary.path("expenseCategories");

        assertTotals(summary, "0.00", "85.00", "0.00");
        assertThat(categoryIds(categories)).containsExactly(child.getId(), parent.getId(), sameName.getId());
        assertMoney(categories.get(0), "amount", "50.00");
        assertMoney(categories.get(1), "amount", "30.00");
        assertThat(categories.get(1).path("name").asString()).isEqualTo(parent.getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly"})
    void shouldNeverIncludeOtherUsersTotalsOrCategoryNamesEvenWithAnInjectedUserId(String period) throws Exception {
        save(userA, TransactionType.INCOME, "100.00", DATE);
        save(userA, TransactionType.EXPENSE, "40.00", DATE);
        save(userA, TransactionType.INCOME, "50.00", MONDAY.minusDays(1));
        for (LocalDate date : List.of(DATE, MONDAY.minusDays(1))) {
            save(userB, TransactionType.INCOME, "9999.00", date);
            save(userB, TransactionType.EXPENSE, "8888.00", date);
            save(userB, TransactionType.CREDIT_CARD_PAYMENT, "7777.00", date);
        }

        JsonNode report = read(mockMvc.perform(request(period, userA, DATE)
                .queryParam("userId", userB.id().toString())));

        assertTotals(summary(report, period), "100.00", "40.00", "0.00");
        assertThat(report.toString()).doesNotContain(userB.id().toString(),
                userB.incomeCategoryId().toString(), userB.expenseCategoryId().toString(), "Cash User B");
        if (period.equals("weekly")) {
            assertTotals(report.path("previousWeek").path("summary"), "50.00", "0.00", "0.00");
        }
    }

    @Test
    void shouldNotResolveAnotherUsersCategoryNameEvenForAnInconsistentStoredAssociation() throws Exception {
        // Defense-in-depth fixture: normal write endpoints reject this cross-user association.
        save(userA, TransactionType.EXPENSE, "25.00", DATE,
                t -> t.setCategoryId(userB.expenseCategoryId()));
        save(userA, TransactionType.EXPENSE, "10.00", DATE, t -> t.setCategoryId(null));

        JsonNode categories = report("daily", userA, DATE).path("summary").path("expenseCategories");

        assertThat(categories.size()).isEqualTo(2);
        assertThat(categories.get(0).path("name").asString()).isEqualTo("Categoria indisponível");
        assertThat(categories.get(1).path("name").asString()).isEqualTo("Sem categoria");
        assertThat(categories.get(1).path("categoryId").isNull()).isTrue();
        assertThat(categories.toString()).doesNotContain("Cash User B");
    }

    @Test
    void shouldAggregateAllMovementsRatherThanOnlyOneTransactionPage() throws Exception {
        for (int index = 0; index < 125; index++) {
            save(userA, TransactionType.INCOME, "1.01", DATE);
        }

        for (String period : List.of("daily", "weekly")) {
            JsonNode summary = summary(report(period, userA, DATE), period);
            assertTotals(summary, "126.25", "0.00", "0.00");
            assertMoney(summary.path("incomeCategories").get(0), "amount", "126.25");
        }
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"INCOME", "EXPENSE"})
    void shouldReflectAmountEditsAndCancellationWithoutStaleReportTotals(TransactionType type) throws Exception {
        UUID transactionId = createViaApi(type, TransactionStatus.COMPLETED, "100.00");
        String field = type == TransactionType.INCOME ? "inflows" : "outflows";
        assertMoney(report("daily", userA, DATE).path("summary"), field, "100.00");

        updateViaApi(transactionId, Map.of("amount", new BigDecimal("250.00")));

        assertMoney(report("daily", userA, DATE).path("summary"), field, "250.00");
        assertMoney(summary(report("weekly", userA, DATE), "weekly"), field, "250.00");
        mockMvc.perform(post("/api/v1/transactions/{id}/cancel", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token()))
                .andExpect(status().isOk());

        for (String period : List.of("daily", "weekly")) {
            assertTotals(summary(report(period, userA, DATE), period), "0.00", "0.00", "0.00");
        }
        entityManager.flush();
        entityManager.clear();
        assertThat(transactionRepository.findById(transactionId).orElseThrow().getStatus())
                .isEqualTo(TransactionStatus.CANCELLED);
        assertThat(accountRepository.findById(userA.accountId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldMoveAMovementBetweenWeeksAfterChangingEffectiveDate() throws Exception {
        UUID transactionId = createViaApi(TransactionType.INCOME, TransactionStatus.COMPLETED, "100.00");

        updateViaApi(transactionId, Map.of("effectiveDate", MONDAY.minusDays(1).toString()));

        assertTotals(report("daily", userA, DATE).path("summary"), "0.00", "0.00", "0.00");
        assertTotals(report("daily", userA, MONDAY.minusDays(1)).path("summary"), "100.00", "0.00", "0.00");
        JsonNode weekly = report("weekly", userA, DATE);
        assertTotals(weekly.path("currentWeek").path("summary"), "0.00", "0.00", "0.00");
        assertTotals(weekly.path("previousWeek").path("summary"), "100.00", "0.00", "0.00");
        assertMoney(weekly.path("comparison"), "netDifference", "-100.00");
    }

    @Test
    void shouldFollowPendingCompletedAndPendingAgainWithoutCountingAnUnpaidMovement() throws Exception {
        UUID transactionId = createViaApi(TransactionType.EXPENSE, TransactionStatus.PENDING, "90.00");
        assertTotals(report("daily", userA, DATE).path("summary"), "0.00", "0.00", "0.00");

        updateViaApi(transactionId, Map.of("status", "COMPLETED", "effectiveDate", DATE.toString()));
        assertTotals(report("daily", userA, DATE).path("summary"), "0.00", "90.00", "0.00");

        updateViaApi(transactionId, Map.of("status", "PENDING"));
        assertTotals(report("daily", userA, DATE).path("summary"), "0.00", "0.00", "0.00");
    }

    @Test
    void shouldMoveTheAmountToTheNewCategoryAfterEditingTheTransaction() throws Exception {
        UUID transactionId = createViaApi(TransactionType.EXPENSE, TransactionStatus.COMPLETED, "80.00");
        Category other = createCategory(userA.id(), "New category", CategoryType.EXPENSE);

        updateViaApi(transactionId, Map.of("categoryId", other.getId().toString()));

        JsonNode summary = report("daily", userA, DATE).path("summary");
        assertTotals(summary, "0.00", "80.00", "0.00");
        assertThat(categoryIds(summary.path("expenseCategories"))).containsExactly(other.getId());
    }

    @Test
    void shouldRemainNeutralAfterCreatingAndCancellingATransferViaTheApi() throws Exception {
        String response = mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sourceAccountId", userA.accountId(), "destinationAccountId", userA.otherAccountId(),
                                "amount", new BigDecimal("70.00"), "date", DATE.toString(), "description", "Transfer"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = objectMapper.readTree(response).path("id").asString();
        assertTotals(report("daily", userA, DATE).path("summary"), "0.00", "0.00", "0.00");

        mockMvc.perform(post("/api/v1/transactions/{id}/cancel", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token()))
                .andExpect(status().isOk());

        assertTotals(summary(report("weekly", userA, DATE), "weekly"), "0.00", "0.00", "0.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly"})
    void shouldNotChangeAccountsOrTransactionsWhenReadingAReport(String period) throws Exception {
        Transaction income = save(userA, TransactionType.INCOME, "300.00", DATE);
        Transaction expense = save(userA, TransactionType.EXPENSE, "90.00", DATE);
        List<UUID> transactions = List.of(income.getId(), expense.getId());
        List<UUID> accounts = List.of(userA.accountId(), userA.otherAccountId());
        // Compare two database snapshots; PostgreSQL normalizes Instant precision on persistence.
        entityManager.flush();
        entityManager.clear();
        Map<UUID, JsonNode> before = new LinkedHashMap<>();
        transactions.forEach(id -> before.put(id, objectMapper.valueToTree(transactionRepository.findById(id).orElseThrow())));
        accounts.forEach(id -> before.put(id, objectMapper.valueToTree(accountRepository.findById(id).orElseThrow())));
        long transactionCount = transactionRepository.count();
        long accountCount = accountRepository.count();
        entityManager.clear();

        report(period, userA, DATE);

        entityManager.flush();
        entityManager.clear();
        for (UUID id : transactions) {
            JsonNode actual = objectMapper.valueToTree(transactionRepository.findById(id).orElseThrow());
            assertThat(actual).isEqualTo(before.get(id));
        }
        for (UUID id : accounts) {
            JsonNode actual = objectMapper.valueToTree(accountRepository.findById(id).orElseThrow());
            assertThat(actual).isEqualTo(before.get(id));
        }
        assertThat(transactionRepository.count()).isEqualTo(transactionCount);
        assertThat(accountRepository.count()).isEqualTo(accountCount);
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly"})
    void shouldRejectMissingDateWithAFieldValidationError(String period) throws Exception {
        ResultActions response = mockMvc.perform(get(BASE + period)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token()));

        assertApiError(response, period, "VALIDATION_ERROR");
        response.andExpect(jsonPath("$.fieldErrors[0].field").value("date"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("A data é obrigatória"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-date", "03/09/2026", "2026-02-30", "2026-13-01", ""})
    void shouldRejectMalformedDatesForBothEndpoints(String date) throws Exception {
        for (String period : List.of("daily", "weekly")) {
            ResultActions response = mockMvc.perform(get(BASE + period)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token()).queryParam("date", date));
            assertApiError(response, period, "VALIDATION_ERROR");
            response.andExpect(jsonPath("$.fieldErrors[0].field").value("date"));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "daily,0000-12-31", "daily,+10000-01-01",
            "weekly,0000-12-31", "weekly,+10000-01-01",
            "weekly,0001-01-01", "weekly,9999-12-31"
    })
    void shouldMapInvalidReferenceOrComputedPeriodToTheStableErrorCode(String period, String date) throws Exception {
        ResultActions response = mockMvc.perform(get(BASE + period)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token()).queryParam("date", date));

        assertApiError(response, period, "INVALID_REPORT_PERIOD");
        response.andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly"})
    void shouldRequireAValidJwt(String period) throws Exception {
        for (String token : List.of("", "Bearer invalid-token")) {
            MockHttpServletRequestBuilder request = get(BASE + period).queryParam("date", DATE.toString());
            if (!token.isEmpty()) request.header(HttpHeaders.AUTHORIZATION, token);
            mockMvc.perform(request).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.path").value(BASE + period));
        }
    }

    private JsonNode report(String period, TestUser user, LocalDate date) throws Exception {
        return read(mockMvc.perform(request(period, user, date)));
    }

    private MockHttpServletRequestBuilder request(String period, TestUser user, LocalDate date) {
        return get(BASE + period).header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token())
                .queryParam("date", date.toString());
    }

    private JsonNode read(ResultActions response) throws Exception {
        return objectMapper.readTree(response.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode summary(JsonNode report, String period) {
        return period.equals("daily") ? report.path("summary") : report.path("currentWeek").path("summary");
    }

    private void assertTotals(JsonNode summary, String inflows, String outflows, String invoices) {
        assertFields(summary, "inflows", "outflows", "net", "invoicePayments", "incomeCategories", "expenseCategories");
        assertMoney(summary, "inflows", inflows);
        assertMoney(summary, "outflows", outflows);
        assertMoney(summary, "net", new BigDecimal(inflows).subtract(new BigDecimal(outflows)).toPlainString());
        assertMoney(summary, "invoicePayments", invoices);
        assertThat(summary.path("incomeCategories").isArray()).isTrue();
        assertThat(summary.path("expenseCategories").isArray()).isTrue();
    }

    private void assertMoney(JsonNode node, String field, String expected) {
        assertThat(node.path(field).isNumber()).as(field).isTrue();
        assertThat(node.path(field).decimalValue()).as(field).isEqualByComparingTo(expected);
    }

    private void assertFields(JsonNode node, String... fields) {
        assertThat(node.properties().stream().map(Map.Entry::getKey).toList()).containsExactlyInAnyOrder(fields);
    }

    private void assertPeriod(JsonNode period, LocalDate start, LocalDate end) {
        assertFields(period, "startDate", "endDate", "summary");
        assertThat(period.path("startDate").asString()).isEqualTo(start.toString());
        assertThat(period.path("endDate").asString()).isEqualTo(end.toString());
    }

    private List<UUID> categoryIds(JsonNode categories) {
        List<UUID> ids = new ArrayList<>();
        for (JsonNode category : categories) {
            assertFields(category, "categoryId", "name", "amount");
            ids.add(UUID.fromString(category.path("categoryId").asString()));
        }
        return ids;
    }

    private void assertApiError(ResultActions response, String period, String code) throws Exception {
        String json = response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.path").value(BASE + period))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode error = objectMapper.readTree(json);
        assertFields(error, "timestamp", "status", "code", "message", "path", "fieldErrors");
        assertThat(Instant.parse(error.path("timestamp").asString())).isNotNull();
        assertThat(error.path("message").asString()).isNotBlank();
    }

    private UUID createViaApi(TransactionType type, TransactionStatus state, String amount) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "Cash report lifecycle");
        body.put("amount", new BigDecimal(amount));
        body.put("competenceDate", DATE.minusMonths(1).toString());
        if (state == TransactionStatus.COMPLETED) body.put("effectiveDate", DATE.toString());
        body.put("type", type.name());
        body.put("status", state.name());
        body.put("paymentMethod", "PIX");
        body.put(type == TransactionType.INCOME ? "destinationAccountId" : "sourceAccountId", userA.accountId());
        body.put("categoryId", type == TransactionType.INCOME ? userA.incomeCategoryId() : userA.expenseCategoryId());
        String json = mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return UUID.fromString(objectMapper.readTree(json).path("id").asString());
    }

    private void updateViaApi(UUID id, Map<String, Object> body) throws Exception {
        mockMvc.perform(patch("/api/v1/transactions/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private Transaction save(TestUser owner, TransactionType type, String amount, LocalDate date) {
        return save(owner, type, amount, date, t -> { });
    }

    private Transaction save(TestUser owner, TransactionType type, String amount, LocalDate date,
                             Consumer<Transaction> customize) {
        Transaction transaction = new Transaction();
        transaction.setUserId(owner.id());
        transaction.setDescription("Cash fixture " + type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCompetenceDate(DATE);
        transaction.setEffectiveDate(date);
        transaction.setDueDate(DATE);
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
                transaction.setCreditCardId(UUID.randomUUID());
                transaction.setInvoiceId(UUID.randomUUID());
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
        user.setEmail("cash-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        User saved = userRepository.saveAndFlush(user);
        String token = jwtService.generateToken(userDetailsService.loadUserByUsername(saved.getEmail()));
        return new TestUser(saved.getId(), token,
                createAccount(saved.getId(), "Main").getId(),
                createAccount(saved.getId(), "Secondary").getId(),
                createCategory(saved.getId(), name + " income", CategoryType.INCOME).getId(),
                createCategory(saved.getId(), name + " expense", CategoryType.EXPENSE).getId());
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

    private record TestUser(UUID id, String token, UUID accountId, UUID otherAccountId,
                            UUID incomeCategoryId, UUID expenseCategoryId) {
    }
}
