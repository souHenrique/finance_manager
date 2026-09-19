package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
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
import org.junit.jupiter.params.provider.EnumSource;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;

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
class TransactionSearchIntegrationTest {

    private static final String ENDPOINT = "/api/v1/transactions";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 15);

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
    private CreditCardRepository creditCardRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    private TestUser userA;
    private TestUser userB;

    @BeforeEach
    void setUp() {
        userA = createUser("Search User A");
        userB = createUser("Search User B");
    }

    @Test
    void shouldListAllStatusesWithoutFiltersAndOnlyForAuthenticatedUser() throws Exception {
        List<Transaction> expected = new ArrayList<>();
        for (TransactionStatus state : TransactionStatus.values()) {
            expected.add(save(userA, state.name(), "50.00", transaction -> setStatus(transaction, state)));
            save(userB, state.name(), "50.00", transaction -> setStatus(transaction, state));
        }

        JsonNode page = search(userA);

        assertThat(ids(page)).containsExactlyInAnyOrderElementsOf(transactionIds(expected));
        assertPage(page, 0, 20, 3, 1, true, true);
        for (JsonNode item : page.path("content")) {
            JsonNode transaction = item.path("transaction");
            assertThat(transaction.path("userId").isMissingNode()).isTrue();
            assertThat(transaction.path("description").asString()).isNotBlank();
            assertThat(transaction.path("amount").decimalValue()).isEqualByComparingTo("50.00");
            assertThat(item.path("displayAmount").decimalValue()).isEqualByComparingTo("50.00");
            assertThat(transaction.path("createdAt").asString()).isNotBlank();
            assertThat(transaction.path("updatedAt").asString()).isNotBlank();
        }
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"INCOME", "EXPENSE", "TRANSFER"})
    void shouldFilterByTransactionType(TransactionType type) throws Exception {
        Transaction expense = save(userA, "Expense", "10.00");
        Transaction income = save(userA, "Income", "20.00", transaction -> makeIncome(transaction, userA));
        Transaction transfer = save(userA, "Transfer", "30.00", transaction -> makeTransfer(transaction, userA));
        Transaction expected = switch (type) {
            case INCOME -> income;
            case EXPENSE -> expense;
            case TRANSFER -> transfer;
            default -> throw new IllegalArgumentException("Unexpected test type");
        };

        assertOnly(search(userA, "type", type.name()), expected);
    }

    @ParameterizedTest
    @EnumSource(TransactionStatus.class)
    void shouldFilterByEachStatus(TransactionStatus state) throws Exception {
        Transaction expected = null;
        for (TransactionStatus candidate : TransactionStatus.values()) {
            Transaction transaction = save(userA, candidate.name(), "10.00", t -> setStatus(t, candidate));
            if (candidate == state) {
                expected = transaction;
            }
        }

        assertOnly(search(userA, "status", state.name()), expected);
    }

    @Test
    void shouldCombineFiltersWithAnd() throws Exception {
        Transaction expected = save(userA, "Mercado central", "150.00");
        save(userA, "Mercado pending", "150.00", t -> setStatus(t, TransactionStatus.PENDING));
        save(userA, "Mercado income", "150.00", t -> makeIncome(t, userA));
        save(userA, "Mercado expensive", "250.00");
        save(userA, "Farmacia", "150.00");
        save(userA, "Mercado another month", "150.00", t -> t.setCompetenceDate(DATE.minusMonths(1)));
        save(userA, "Mercado another account", "150.00", t -> t.setSourceAccountId(userA.otherAccountId()));
        Category otherCategory = createCategory(userA.id(), "Other expense", CategoryType.EXPENSE);
        save(userA, "Mercado another category", "150.00", t -> t.setCategoryId(otherCategory.getId()));
        save(userB, "Mercado central", "150.00");

        assertOnly(search(userA,
                "type", "EXPENSE", "status", "COMPLETED",
                "categoryId", userA.expenseCategoryId().toString(),
                "accountId", userA.accountId().toString(),
                "startDate", "2026-09-01", "endDate", "2026-09-30",
                "minAmount", "100.00", "maxAmount", "200.00", "description", "mercado"
        ), expected);
    }

    @Test
    void shouldUseInclusiveCompetenceDatesInsteadOfEffectiveDates() throws Exception {
        Transaction first = save(userA, "First day", "10.00", t -> {
            t.setCompetenceDate(LocalDate.of(2026, 9, 1));
            t.setEffectiveDate(LocalDate.of(2026, 10, 10));
        });
        Transaction last = save(userA, "Last day pending", "20.00", t -> {
            t.setCompetenceDate(LocalDate.of(2026, 9, 30));
            setStatus(t, TransactionStatus.PENDING);
        });
        save(userA, "Before", "30.00", t -> t.setCompetenceDate(LocalDate.of(2026, 8, 31)));
        save(userA, "After", "40.00", t -> t.setCompetenceDate(LocalDate.of(2026, 10, 1)));

        JsonNode page = search(userA, "startDate", "2026-09-01", "endDate", "2026-09-30");

        assertThat(ids(page)).containsExactly(last.getId(), first.getId());
        assertThat(page.path("totalElements").asLong()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"startDate", "endDate"})
    void shouldAcceptSingleDateBoundary(String filter) throws Exception {
        Transaction before = save(userA, "Before", "10.00", t -> t.setCompetenceDate(DATE.minusDays(1)));
        Transaction boundary = save(userA, "Boundary", "20.00");
        Transaction after = save(userA, "After", "30.00", t -> t.setCompetenceDate(DATE.plusDays(1)));

        JsonNode page = search(userA, filter, DATE.toString());

        assertThat(ids(page)).containsExactlyInAnyOrder(
                boundary.getId(), "startDate".equals(filter) ? after.getId() : before.getId()
        );
        assertThat(page.path("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void shouldFilterInclusiveMonetaryRange() throws Exception {
        save(userA, "Below", "49.99");
        Transaction minimum = save(userA, "Minimum", "50.00");
        Transaction middle = save(userA, "Middle", "100.01");
        Transaction maximum = save(userA, "Maximum", "500.00");
        save(userA, "Above", "500.01");

        JsonNode page = search(userA, "minAmount", "50.00", "maxAmount", "500.00", "sort", "amount,asc");

        assertThat(ids(page)).containsExactly(minimum.getId(), middle.getId(), maximum.getId());
        assertThat(page.path("totalElements").asLong()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"minAmount", "maxAmount"})
    void shouldAcceptSingleMonetaryBoundary(String filter) throws Exception {
        Transaction below = save(userA, "Below", "49.99");
        Transaction boundary = save(userA, "Boundary", "50.00");
        Transaction above = save(userA, "Above", "50.01");

        JsonNode page = search(userA, filter, "50.00");

        assertThat(ids(page)).containsExactlyInAnyOrder(
                boundary.getId(), "minAmount".equals(filter) ? above.getId() : below.getId()
        );
    }

    @Test
    void shouldAcceptEqualDateAndAmountLimits() throws Exception {
        Transaction expected = save(userA, "Exact", "50.00");
        save(userA, "Another amount", "50.01");
        save(userA, "Another day", "50.00", t -> t.setCompetenceDate(DATE.plusDays(1)));

        assertOnly(search(userA, "startDate", DATE.toString(), "endDate", DATE.toString(),
                "minAmount", "50.0", "maxAmount", "50.00"), expected);
    }

    @Test
    void shouldSearchDescriptionIgnoringCaseAndSurroundingWhitespace() throws Exception {
        Transaction expected = save(userA, "Compra no SuPeRmErCaDo central", "10.00");
        save(userA, "Farmacia", "10.00");

        assertOnly(search(userA, "description", "  MERCADO  "), expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "!"})
    void shouldTreatLikeWildcardsAndEscapeCharacterAsLiteralText(String literal) throws Exception {
        Transaction expected = save(userA, "Literal " + literal + " token", "10.00");
        save(userA, "Literal X token", "10.00");
        save(userA, "Literal anything token", "10.00");

        assertOnly(search(userA, "description", literal), expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldIgnoreBlankDescription(String description) throws Exception {
        Transaction expected = save(userA, "Visible", "10.00");

        assertOnly(search(userA, "description", description), expected);
    }

    @Test
    void shouldFilterExactCategoryWithoutAutomaticallyIncludingSubcategories() throws Exception {
        Category child = createCategory(userA.id(), "Child", CategoryType.EXPENSE);
        child.setParentCategoryId(userA.expenseCategoryId());
        categoryRepository.saveAndFlush(child);
        Transaction parentTransaction = save(userA, "Parent", "10.00");
        Transaction childTransaction = save(userA, "Child", "10.00", t -> t.setCategoryId(child.getId()));

        assertOnly(search(userA, "categoryId", userA.expenseCategoryId().toString()), parentTransaction);
        assertOnly(search(userA, "categoryId", child.getId().toString()), childTransaction);
    }

    @Test
    void shouldFindAccountAsSourceOrDestinationWithoutDuplicatingTransfers() throws Exception {
        Transaction expense = save(userA, "Expense", "10.00");
        Transaction income = save(userA, "Income", "20.00", t -> makeIncome(t, userA));
        Transaction transfer = save(userA, "Transfer", "30.00", t -> makeTransfer(t, userA));
        Transaction otherExpense = save(userA, "Other account", "40.00",
                t -> t.setSourceAccountId(userA.otherAccountId()));

        JsonNode firstAccount = search(userA, "accountId", userA.accountId().toString());
        JsonNode secondAccount = search(userA, "accountId", userA.otherAccountId().toString());

        assertThat(ids(firstAccount)).containsExactlyInAnyOrder(expense.getId(), income.getId(), transfer.getId());
        assertThat(firstAccount.path("totalElements").asLong()).isEqualTo(3);
        assertThat(ids(secondAccount)).containsExactlyInAnyOrder(transfer.getId(), otherExpense.getId());
        assertThat(secondAccount.path("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void shouldKeepOtherFiltersOutsideTheAccountOrGroup() throws Exception {
        save(userA, "Expense", "10.00");
        save(userA, "Completed income", "20.00", t -> makeIncome(t, userA));
        Transaction expected = save(userA, "Pending income", "30.00", t -> {
            makeIncome(t, userA);
            setStatus(t, TransactionStatus.PENDING);
        });
        save(userA, "Transfer", "40.00", t -> makeTransfer(t, userA));

        assertOnly(search(userA, "accountId", userA.accountId().toString(),
                "type", "INCOME", "status", "PENDING"), expected);
    }

    @Test
    void shouldFilterCreditCardIdWithoutImplementingCardCreationFlows() throws Exception {
        UUID cardId = createCreditCard(userA).getId();
        Transaction expected = saveCardPurchase(userA, cardId);
        saveCardPurchase(userA, createCreditCard(userA).getId());
        save(userA, "Without card", "10.00");
        saveCardPurchase(userB, createCreditCard(userB).getId());

        assertOnly(search(userA, "creditCardId", cardId.toString(),
                "type", "CREDIT_CARD_PURCHASE", "status", "COMPLETED"), expected);
    }

    @Test
    void shouldKeepHistoryAccessibleForInactiveAccountsAndCategories() throws Exception {
        Transaction expected = save(userA, "History", "10.00");
        Account account = accountRepository.findById(userA.accountId()).orElseThrow();
        account.setStatus(AccountStatus.INACTIVE);
        accountRepository.saveAndFlush(account);
        Category category = categoryRepository.findById(userA.expenseCategoryId()).orElseThrow();
        category.setStatus(CategoryStatus.INACTIVE);
        categoryRepository.saveAndFlush(category);

        assertOnly(search(userA, "accountId", account.getId().toString(),
                "categoryId", category.getId().toString()), expected);
    }

    @Test
    void shouldPaginateFilteredResultsWithoutExposingOtherUserTotals() throws Exception {
        List<Transaction> ownTransactions = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            ownTransactions.add(save(userA, "Search match", Integer.toString(index)));
        }
        save(userA, "Not included", "99.00", t -> setStatus(t, TransactionStatus.PENDING));
        for (int index = 1; index <= 7; index++) {
            save(userB, "Search match", Integer.toString(index));
        }
        List<UUID> collectedIds = new ArrayList<>();

        for (int number = 0; number < 3; number++) {
            JsonNode page = search(userA, "status", "COMPLETED", "description", "match",
                    "sort", "amount,asc", "page", Integer.toString(number), "size", "2");
            assertPage(page, number, 2, 5, 3, number == 0, number == 2);
            assertThat(ids(page)).hasSize(number == 2 ? 1 : 2);
            collectedIds.addAll(ids(page));
        }

        assertThat(collectedIds).containsExactlyElementsOf(transactionIds(ownTransactions)).doesNotHaveDuplicates();
        JsonNode otherUserPage = search(userB, "status", "COMPLETED", "description", "match", "size", "2");
        assertPage(otherUserPage, 0, 2, 7, 4, true, false);
        assertThat(ids(otherUserPage)).doesNotContainAnyElementsOf(collectedIds);
    }

    @Test
    void shouldUseDefaultPageSizeAndSortByCompetenceDateDescending() throws Exception {
        List<Transaction> transactions = new ArrayList<>();
        for (int index = 0; index < 25; index++) {
            LocalDate date = DATE.plusDays(index);
            transactions.add(save(userA, "Default page", "10.00", t -> t.setCompetenceDate(date)));
        }

        JsonNode page = search(userA);

        List<UUID> expected = transactions.reversed().subList(0, 20).stream().map(Transaction::getId).toList();
        assertThat(ids(page)).containsExactlyElementsOf(expected);
        assertPage(page, 0, 20, 25, 2, true, false);
    }

    @Test
    void shouldLimitRequestedPageSizeToOneHundred() throws Exception {
        for (int index = 0; index < 101; index++) {
            save(userA, "Limited page", "10.00");
        }

        JsonNode page = search(userA, "size", "200");

        assertThat(ids(page)).hasSize(100).doesNotHaveDuplicates();
        assertPage(page, 0, 100, 101, 2, true, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"asc", "desc"})
    void shouldSortAmountsAndUseIdAsStableTieBreaker(String direction) throws Exception {
        List<Transaction> transactions = List.of(
                save(userA, "High", "30.00"),
                save(userA, "Tie one", "10.00"),
                save(userA, "Tie two", "10.00"),
                save(userA, "Middle", "20.00")
        );
        Comparator<Transaction> amountOrder = Comparator.comparing(Transaction::getAmount);
        if ("desc".equals(direction)) {
            amountOrder = amountOrder.reversed();
        }
        // PostgreSQL orders UUIDs by their unsigned bytes, equivalent to canonical UUID text order.
        List<UUID> expected = transactions.stream()
                .sorted(amountOrder.thenComparing(t -> t.getId().toString()))
                .map(Transaction::getId).toList();
        List<UUID> actual = new ArrayList<>();
        for (int number = 0; number < 2; number++) {
            actual.addAll(ids(search(userA, "sort", "amount," + direction,
                    "page", Integer.toString(number), "size", "2")));
        }

        assertThat(actual).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
    }

    @Test
    void shouldAcceptMultipleSortParameters() throws Exception {
        Transaction earlier = save(userA, "Earlier", "10.00", t -> t.setCompetenceDate(DATE.minusDays(1)));
        Transaction later = save(userA, "Later", "10.00");
        Transaction larger = save(userA, "Larger", "20.00");

        JsonNode page = search(userA, "sort", "amount,asc", "sort", "competenceDate,desc");

        assertThat(ids(page)).containsExactly(later.getId(), earlier.getId(), larger.getId());
    }

    @Test
    void shouldSortDescriptionsIgnoringCase() throws Exception {
        Transaction alpha = save(userA, "alpha", "10.00");
        Transaction beta = save(userA, "Beta", "10.00");
        Transaction zebra = save(userA, "zebra", "10.00");

        assertThat(ids(search(userA, "sort", "description,asc,ignorecase")))
                .containsExactly(alpha.getId(), beta.getId(), zebra.getId());
    }

    @Test
    void shouldReturnEmptyPageBeyondLastPageWhilePreservingFilteredTotal() throws Exception {
        save(userA, "Own", "10.00");
        save(userB, "Other", "10.00");

        JsonNode page = search(userA, "page", "3", "size", "2");

        assertThat(ids(page)).isEmpty();
        assertPage(page, 3, 2, 1, 1, false, true);
    }

    @Test
    void shouldReturnEmptyPageWhenNoTransactionsMatch() throws Exception {
        save(userA, "Expense", "10.00");

        JsonNode page = search(userA, "type", "INCOME");

        assertThat(ids(page)).isEmpty();
        assertPage(page, 0, 20, 0, 0, true, true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"accountId", "categoryId", "creditCardId"})
    void shouldReturnSameEmptyResultForForeignAndUnknownResourceFilters(String filter) throws Exception {
        save(userA, "Own expense", "10.00");
        save(userB, "Other expense", "10.00");
        save(userB, "Other income", "20.00", t -> makeIncome(t, userB));
        UUID foreignCard = createCreditCard(userB).getId();
        saveCardPurchase(userB, foreignCard);
        UUID foreignId = switch (filter) {
            case "accountId" -> userB.accountId();
            case "categoryId" -> userB.expenseCategoryId();
            case "creditCardId" -> foreignCard;
            default -> throw new IllegalArgumentException("Unexpected filter");
        };

        for (UUID resourceId : List.of(foreignId, UUID.randomUUID())) {
            JsonNode page = search(userA, filter, resourceId.toString(), "size", "1");
            assertThat(ids(page)).isEmpty();
            assertPage(page, 0, 1, 0, 0, true, true);
        }
    }

    @Test
    void shouldIgnoreAttemptToOverrideAuthenticatedUserThroughQueryParameter() throws Exception {
        Transaction own = save(userA, "Own", "10.00");
        save(userB, "Other", "20.00");

        assertOnly(search(userA, "userId", userB.id().toString()), own);
    }

    @ParameterizedTest
    @CsvSource({
            "startDate,2026-09-30,endDate,2026-09-01",
            "minAmount,100.01,maxAmount,100.00"
    })
    void shouldRejectReversedRanges(String first, String firstValue, String second, String secondValue) throws Exception {
        perform(userA, first, firstValue, second, secondValue)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSACTION"))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
    }

    @ParameterizedTest
    @CsvSource({
            "startDate,not-a-date", "endDate,2026-02-30",
            "categoryId,not-a-uuid", "accountId,not-a-uuid", "creditCardId,not-a-uuid",
            "type,UNKNOWN", "status,UNKNOWN", "minAmount,abc", "maxAmount,abc",
            "minAmount,-0.01", "maxAmount,-1.00", "minAmount,0.001", "maxAmount,1.001",
            "minAmount,100000000000000000.00", "maxAmount,100000000000000000.00"
    })
    void shouldReturnFieldValidationErrorsForMalformedOrInvalidFilters(String filter, String value) throws Exception {
        perform(userA, filter, value)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value(ENDPOINT))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(filter));
    }

    @Test
    void shouldRejectDescriptionLongerThanAllowed() throws Exception {
        perform(userA, "description", "x".repeat(256))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("description"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown,asc", "userId,desc", "account.name,asc", "amount,asc,ignorecase"})
    void shouldRejectInvalidSort(String sort) throws Exception {
        perform(userA, "sort", sort)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSACTION"));
    }

    @Test
    void shouldAcceptZeroAsMonetaryFilterBoundary() throws Exception {
        Transaction expected = save(userA, "Positive amount", "0.01");

        assertOnly(search(userA, "minAmount", "0.00"), expected);
        assertThat(ids(search(userA, "maxAmount", "0.00"))).isEmpty();
    }

    @Test
    void shouldRequireValidJwtForListing() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get(ENDPOINT).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private JsonNode search(TestUser user, String... parameters) throws Exception {
        String json = perform(user, parameters)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json);
    }

    private ResultActions perform(TestUser user, String... parameters) throws Exception {
        if (parameters.length % 2 != 0) {
            throw new IllegalArgumentException("Query parameters must be name/value pairs");
        }
        MockHttpServletRequestBuilder request = get(ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token());
        for (int index = 0; index < parameters.length; index += 2) {
            request.queryParam(parameters[index], parameters[index + 1]);
        }
        return mockMvc.perform(request);
    }

    private List<UUID> ids(JsonNode page) {
        JsonNode content = page.path("content");
        assertThat(content.isArray()).isTrue();
        return IntStream.range(0, content.size())
                .mapToObj(index -> UUID.fromString(
                        content.path(index).path("transaction").path("id").asString()
                ))
                .toList();
    }

    private List<UUID> transactionIds(List<Transaction> transactions) {
        return transactions.stream().map(Transaction::getId).toList();
    }

    private void assertOnly(JsonNode page, Transaction transaction) {
        assertThat(ids(page)).containsExactly(transaction.getId());
        assertThat(page.path("totalElements").asLong()).isEqualTo(1);
    }

    private void assertPage(JsonNode page, int number, int size, long total, int totalPages, boolean first, boolean last) {
        assertThat(page.properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages", "first", "last");
        for (String property : List.of("page", "size", "totalElements", "totalPages")) {
            assertThat(page.path(property).isIntegralNumber()).as(property).isTrue();
        }
        assertThat(page.path("first").isBoolean()).isTrue();
        assertThat(page.path("last").isBoolean()).isTrue();
        assertThat(page.path("page").asInt()).isEqualTo(number);
        assertThat(page.path("size").asInt()).isEqualTo(size);
        assertThat(page.path("totalElements").asLong()).isEqualTo(total);
        assertThat(page.path("totalPages").asInt()).isEqualTo(totalPages);
        assertThat(page.path("first").asBoolean()).isEqualTo(first);
        assertThat(page.path("last").asBoolean()).isEqualTo(last);
    }

    private Transaction save(TestUser owner, String description, String amount) {
        return save(owner, description, amount, transaction -> { });
    }

    private Transaction save(TestUser owner, String description, String amount, Consumer<Transaction> customize) {
        Transaction transaction = new Transaction();
        transaction.setUserId(owner.id());
        transaction.setDescription(description);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCompetenceDate(DATE);
        transaction.setEffectiveDate(DATE);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setPaymentMethod(PaymentMethod.PIX);
        transaction.setSourceAccountId(owner.accountId());
        transaction.setCategoryId(owner.expenseCategoryId());
        customize.accept(transaction);
        return transactionRepository.saveAndFlush(transaction);
    }

    private Transaction saveCardPurchase(TestUser owner, UUID cardId) {
        // Persistence fixture only: the card purchase HTTP flow is outside T18.
        return save(owner, "Card purchase", "50.00", t -> {
            t.setType(TransactionType.CREDIT_CARD_PURCHASE);
            t.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            t.setCreditCardId(cardId);
            t.setSourceAccountId(null);
            t.setEffectiveDate(null);
        });
    }

    private CreditCard createCreditCard(TestUser owner) {
        CreditCard creditCard = new CreditCard();
        creditCard.setUserId(owner.id());
        creditCard.setName("Search card " + UUID.randomUUID());
        creditCard.setCreditLimit(new BigDecimal("5000.00"));
        creditCard.setAvailableLimit(new BigDecimal("5000.00"));
        creditCard.setClosingDay(10);
        creditCard.setDueDay(17);
        creditCard.setDefaultAccountId(owner.accountId());
        creditCard.setStatus(CreditCardStatus.ACTIVE);
        return creditCardRepository.saveAndFlush(creditCard);
    }

    private void makeIncome(Transaction transaction, TestUser owner) {
        transaction.setType(TransactionType.INCOME);
        transaction.setSourceAccountId(null);
        transaction.setDestinationAccountId(owner.accountId());
        transaction.setCategoryId(owner.incomeCategoryId());
    }

    private void makeTransfer(Transaction transaction, TestUser owner) {
        transaction.setType(TransactionType.TRANSFER);
        transaction.setPaymentMethod(PaymentMethod.TRANSFER);
        transaction.setDestinationAccountId(owner.otherAccountId());
        transaction.setCategoryId(null);
    }

    private void setStatus(Transaction transaction, TransactionStatus state) {
        transaction.setStatus(state);
        if (state == TransactionStatus.PENDING) {
            transaction.setEffectiveDate(null);
        }
    }

    private TestUser createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("search-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        User saved = userRepository.saveAndFlush(user);
        String token = jwtService.generateToken(userDetailsService.loadUserByUsername(saved.getEmail()));
        return new TestUser(saved.getId(), token,
                createAccount(saved.getId(), "Main").getId(),
                createAccount(saved.getId(), "Secondary").getId(),
                createCategory(saved.getId(), "Income", CategoryType.INCOME).getId(),
                createCategory(saved.getId(), "Expense", CategoryType.EXPENSE).getId());
    }

    private Account createAccount(UUID userId, String name) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName(name);
        account.setType(AccountType.CHECKING);
        account.setInitialBalance(new BigDecimal("1000.00"));
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
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
