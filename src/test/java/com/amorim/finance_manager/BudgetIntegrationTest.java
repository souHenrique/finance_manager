package com.amorim.finance_manager;

import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.budget.entity.Budget;
import com.amorim.finance_manager.budget.repository.BudgetRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class BudgetIntegrationTest {

    private static final String PASSWORD = "IntegrationPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldCreateBudgetForOwnedExpenseCategory() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);

        MvcResult result = mockMvc.perform(
                        post("/api/v1/budgets")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(budgetBody(categoryId, 9, 2026, "1500.00"))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$.month").value(9))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.amountLimit").value(1500.0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andReturn();

        UUID budgetId = responseId(result);
        Budget saved = budgetRepository.findById(budgetId).orElseThrow();

        assertThat(saved.getUserId()).isEqualTo(user.id());
        assertThat(saved.getCategoryId()).isEqualTo(categoryId);
        assertThat(saved.getAmountLimit()).isEqualByComparingTo("1500.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.00", "1.001"})
    void shouldRejectInvalidAmountLimit(String amountLimit) throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);

        mockMvc.perform(
                        post("/api/v1/budgets")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(budgetBody(categoryId, 9, 2026, amountLimit))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amountLimit"));

        assertThat(budgetRepository.count()).isZero();
    }

    @Test
    void shouldRejectInvalidMonthAndYear() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);

        for (String body : new String[]{
                budgetBody(categoryId, 0, 2026, "100.00"),
                budgetBody(categoryId, 13, 2026, "100.00"),
                budgetBody(categoryId, 9, 0, "100.00"),
                budgetBody(categoryId, 9, 10000, "100.00")
        }) {
            mockMvc.perform(
                            post("/api/v1/budgets")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        assertThat(budgetRepository.count()).isZero();
    }

    @Test
    void shouldRejectIncomeCategory() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.INCOME, CategoryStatus.ACTIVE);

        mockMvc.perform(
                        post("/api/v1/budgets")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(budgetBody(categoryId, 9, 2026, "1500.00"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CATEGORY_TYPE_MISMATCH"));

        assertThat(budgetRepository.count()).isZero();
    }

    @Test
    void shouldHideCategoryOwnedByAnotherUser() throws Exception {
        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");
        String tokenA = login(userA);
        UUID categoryFromUserB = createCategory(userB.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);

        mockMvc.perform(
                        post("/api/v1/budgets")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(budgetBody(categoryFromUserB, 9, 2026, "1500.00"))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        assertThat(budgetRepository.count()).isZero();
    }

    @Test
    void shouldAllowInactiveExpenseCategoryBecauseContractOnlyRequiresExpenseType() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.INACTIVE);

        createBudget(token, categoryId, 9, 2026, "1500.00");

        assertThat(budgetRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateBudgetForSameCategoryAndPeriod() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);

        createBudget(token, categoryId, 9, 2026, "1500.00");

        mockMvc.perform(
                        post("/api/v1/budgets")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(budgetBody(categoryId, 9, 2026, "1800.00"))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUDGET_ALREADY_EXISTS"));

        assertThat(budgetRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldEnforceUniqueConstraintAtDatabaseLevel() throws Exception {
        TestUser user = registerUser("User A");
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        Budget first = budget(user.id(), categoryId, 9, 2026, "1500.00");
        Budget duplicate = budget(user.id(), categoryId, 9, 2026, "1800.00");

        budgetRepository.saveAndFlush(first);

        assertThatThrownBy(() -> budgetRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(budgetRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldListOnlyCurrentUserBudgetsInDescendingPeriodOrder() throws Exception {
        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");
        String tokenA = login(userA);
        String tokenB = login(userB);
        UUID categoryA = createCategory(userA.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID categoryB = createCategory(userB.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);

        UUID augustId = createBudget(tokenA, categoryA, 8, 2026, "900.00");
        UUID septemberId = createBudget(tokenA, categoryA, 9, 2026, "1500.00");
        createBudget(tokenB, categoryB, 10, 2026, "2000.00");

        mockMvc.perform(
                        get("/api/v1/budgets")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(septemberId.toString()))
                .andExpect(jsonPath("$[1].id").value(augustId.toString()))
                .andExpect(jsonPath("$[0].userId").doesNotExist());
    }

    @Test
    void shouldReturnBudgetOnlyToItsOwner() throws Exception {
        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");
        String tokenA = login(userA);
        String tokenB = login(userB);
        UUID categoryId = createCategory(userA.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID budgetId = createBudget(tokenA, categoryId, 9, 2026, "1500.00");

        mockMvc.perform(
                        get("/api/v1/budgets/{id}", budgetId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId.toString()));

        mockMvc.perform(
                        get("/api/v1/budgets/{id}", budgetId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BUDGET_NOT_FOUND"));
    }

    @Test
    void shouldPartiallyUpdateBudgetAmount() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID budgetId = createBudget(token, categoryId, 9, 2026, "1500.00");

        mockMvc.perform(
                        patch("/api/v1/budgets/{id}", budgetId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amountLimit\":1800.00}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$.month").value(9))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.amountLimit").value(1800.0));

        Budget updated = budgetRepository.findById(budgetId).orElseThrow();
        assertThat(updated.getAmountLimit()).isEqualByComparingTo("1800.00");
    }

    @Test
    void shouldUpdateBudgetCategoryAndPeriod() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID originalCategoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID newCategoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID budgetId = createBudget(token, originalCategoryId, 9, 2026, "1500.00");

        String body = """
                {
                  "categoryId": "%s",
                  "month": 10,
                  "year": 2027
                }
                """.formatted(newCategoryId);

        mockMvc.perform(
                        patch("/api/v1/budgets/{id}", budgetId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(newCategoryId.toString()))
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.year").value(2027))
                .andExpect(jsonPath("$.amountLimit").value(1500.0));

        Budget updated = budgetRepository.findById(budgetId).orElseThrow();
        assertThat(updated.getCategoryId()).isEqualTo(newCategoryId);
        assertThat(updated.getMonth()).isEqualTo(10);
        assertThat(updated.getYear()).isEqualTo(2027);
    }

    @Test
    void shouldRejectInvalidPatchValues() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID budgetId = createBudget(token, categoryId, 9, 2026, "1500.00");

        for (String body : new String[]{
                "{\"amountLimit\":0}",
                "{\"amountLimit\":-1.00}",
                "{\"amountLimit\":1.001}",
                "{\"month\":0}",
                "{\"month\":13}",
                "{\"year\":0}",
                "{\"year\":10000}"
        }) {
            mockMvc.perform(
                            patch("/api/v1/budgets/{id}", budgetId)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        Budget unchanged = budgetRepository.findById(budgetId).orElseThrow();
        assertThat(unchanged.getMonth()).isEqualTo(9);
        assertThat(unchanged.getYear()).isEqualTo(2026);
        assertThat(unchanged.getAmountLimit()).isEqualByComparingTo("1500.00");
    }

    @Test
    void shouldRejectIncomeAndForeignCategoriesDuringUpdate() throws Exception {
        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");
        String tokenA = login(userA);
        UUID originalCategoryId = createCategory(userA.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID incomeCategoryId = createCategory(userA.id(), CategoryType.INCOME, CategoryStatus.ACTIVE);
        UUID foreignCategoryId = createCategory(userB.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID budgetId = createBudget(tokenA, originalCategoryId, 9, 2026, "1500.00");

        mockMvc.perform(
                        patch("/api/v1/budgets/{id}", budgetId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"categoryId\":\"" + incomeCategoryId + "\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CATEGORY_TYPE_MISMATCH"));

        mockMvc.perform(
                        patch("/api/v1/budgets/{id}", budgetId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"categoryId\":\"" + foreignCategoryId + "\"}")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        assertThat(budgetRepository.findById(budgetId).orElseThrow().getCategoryId())
                .isEqualTo(originalCategoryId);
    }

    @Test
    void shouldRejectUpdateThatDuplicatesAnotherBudget() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        createBudget(token, categoryId, 9, 2026, "1500.00");
        UUID octoberId = createBudget(token, categoryId, 10, 2026, "1800.00");

        mockMvc.perform(
                        patch("/api/v1/budgets/{id}", octoberId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"month\":9}")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUDGET_ALREADY_EXISTS"));

        Budget unchanged = budgetRepository.findById(octoberId).orElseThrow();
        assertThat(unchanged.getMonth()).isEqualTo(10);
    }

    @Test
    void shouldRejectEmptyPatch() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID budgetId = createBudget(token, categoryId, 9, 2026, "1500.00");

        mockMvc.perform(
                        patch("/api/v1/budgets/{id}", budgetId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUDGET_UPDATE"))
                .andExpect(jsonPath("$.message")
                        .value("Informe ao menos um campo para atualização"));
    }

    @Test
    void shouldDeleteOwnedBudget() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);
        UUID categoryId = createCategory(user.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID budgetId = createBudget(token, categoryId, 9, 2026, "1500.00");

        mockMvc.perform(
                        delete("/api/v1/budgets/{id}", budgetId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(budgetRepository.findById(budgetId)).isEmpty();
    }

    @Test
    void shouldNotDeleteBudgetOwnedByAnotherUser() throws Exception {
        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");
        String tokenA = login(userA);
        String tokenB = login(userB);
        UUID categoryId = createCategory(userA.id(), CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        UUID budgetId = createBudget(tokenA, categoryId, 9, 2026, "1500.00");

        mockMvc.perform(
                        delete("/api/v1/budgets/{id}", budgetId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BUDGET_NOT_FOUND"));

        assertThat(budgetRepository.findById(budgetId)).isPresent();
    }

    @Test
    void shouldRejectEveryBudgetOperationWithoutJwt() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/budgets/{id}", id))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/v1/budgets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(budgetBody(UUID.randomUUID(), 9, 2026, "100.00"))
                )
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        patch("/api/v1/budgets/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amountLimit\":200.00}")
                )
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/budgets/{id}", id))
                .andExpect(status().isUnauthorized());
    }

    private TestUser registerUser(String name) throws Exception {
        String email = "budget.integration." + UUID.randomUUID() + "@example.com";
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

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new TestUser(UUID.fromString(response.get("id").asText()), email, PASSWORD);
    }

    private String login(TestUser user) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(user.email(), user.password());

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("token")
                .asText();
    }

    private UUID createCategory(
            UUID userId,
            CategoryType type,
            CategoryStatus status
    ) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName("Categoria " + UUID.randomUUID());
        category.setType(type);
        category.setStatus(status);
        return categoryRepository.saveAndFlush(category).getId();
    }

    private UUID createBudget(
            String token,
            UUID categoryId,
            int month,
            int year,
            String amountLimit
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/budgets")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(budgetBody(categoryId, month, year, amountLimit))
                )
                .andExpect(status().isCreated())
                .andReturn();

        return responseId(result);
    }

    private Budget budget(
            UUID userId,
            UUID categoryId,
            int month,
            int year,
            String amountLimit
    ) {
        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategoryId(categoryId);
        budget.setMonth(month);
        budget.setYear(year);
        budget.setAmountLimit(new BigDecimal(amountLimit));
        return budget;
    }

    private UUID responseId(MvcResult result) throws Exception {
        return UUID.fromString(
                objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .get("id")
                        .asText()
        );
    }

    private String budgetBody(
            UUID categoryId,
            int month,
            int year,
            String amountLimit
    ) {
        return """
                {
                  "categoryId": "%s",
                  "month": %d,
                  "year": %d,
                  "amountLimit": %s
                }
                """.formatted(categoryId, month, year, amountLimit);
    }

    private void cleanDatabase() {
        budgetRepository.deleteAll();

        categoryRepository.findAll()
                .stream()
                .filter(category -> category.getParentCategoryId() != null)
                .forEach(categoryRepository::delete);

        categoryRepository.flush();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    private record TestUser(
            UUID id,
            String email,
            String password
    ) {
    }
}
