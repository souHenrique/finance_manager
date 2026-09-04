package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.security.JwtService;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import com.amorim.finance_manager.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
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
class CreditCardIntegrationTest {

    private static final String BASE = "/api/v1/credit-cards";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CreditCardRepository creditCardRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TestUser userA;
    private TestUser userB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    transactions_aud,
                    credit_cards_aud,
                    accounts_aud,
                    audit_revision,
                    transactions,
                    credit_cards,
                    categories,
                    accounts,
                    users
                CASCADE
                """
        );
        userA = createUser("Credit Card User A");
        userB = createUser("Credit Card User B");
    }

    @Test
    void shouldCreateAnActiveCardWithTheWholeLimitAvailableAndVersionZero() throws Exception {
        JsonNode response = read(mockMvc.perform(authenticated(
                post(BASE),
                userA
        ).content(createJson(
                "Main card",
                "5000.00",
                10,
                17,
                userA.accountId()
        ))), 201);

        assertThat(response.path("id").asString()).isNotBlank();
        assertThat(response.path("name").asString()).isEqualTo("Main card");
        assertMoney(response, "creditLimit", "5000.00");
        assertMoney(response, "availableLimit", "5000.00");
        assertThat(response.path("closingDay").asInt()).isEqualTo(10);
        assertThat(response.path("dueDay").asInt()).isEqualTo(17);
        assertThat(response.path("defaultAccountId").asString())
                .isEqualTo(userA.accountId().toString());
        assertThat(response.path("status").asString()).isEqualTo("ACTIVE");
        assertThat(response.path("version").asLong()).isZero();
        assertThat(response.has("userId")).isFalse();

        CreditCard persisted = creditCardRepository
                .findById(UUID.fromString(response.path("id").asString()))
                .orElseThrow();
        assertThat(persisted.getUserId()).isEqualTo(userA.id());
        assertThat(persisted.getAvailableLimit()).isEqualByComparingTo("5000.00");
    }

    @Test
    void shouldRejectEveryInvalidCreationFieldWithAStableValidationPayload() throws Exception {
        List<InvalidCreateCase> cases = List.of(
                new InvalidCreateCase(" ", "5000.00", 10, 17, "name"),
                new InvalidCreateCase("Card", "0.00", 10, 17, "creditLimit"),
                new InvalidCreateCase("Card", "-0.01", 10, 17, "creditLimit"),
                new InvalidCreateCase("Card", "5000.00", 0, 17, "closingDay"),
                new InvalidCreateCase("Card", "5000.00", 32, 17, "closingDay"),
                new InvalidCreateCase("Card", "5000.00", 10, 0, "dueDay"),
                new InvalidCreateCase("Card", "5000.00", 10, 32, "dueDay")
        );

        for (InvalidCreateCase invalid : cases) {
            mockMvc.perform(authenticated(post(BASE), userA)
                            .content(createJson(
                                    invalid.name(),
                                    invalid.limit(),
                                    invalid.closingDay(),
                                    invalid.dueDay(),
                                    userA.accountId()
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem(invalid.field())));
        }

        assertThat(creditCardRepository.count()).isZero();
    }

    @Test
    void shouldHideMissingAndForeignDefaultAccountsBehindAccountNotFound() throws Exception {
        for (UUID accountId : List.of(UUID.randomUUID(), userB.accountId())) {
            mockMvc.perform(authenticated(post(BASE), userA)
                            .content(createJson("Card", "5000.00", 10, 17, accountId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        }

        assertThat(creditCardRepository.count()).isZero();
    }

    @Test
    void shouldListOnlyTheAuthenticatedUsersCardsOrderedByName() throws Exception {
        CreditCard second = saveCard(userA, "B Card", "3000.00", "2500.00");
        CreditCard first = saveCard(userA, "A Card", "5000.00", "5000.00");
        saveCard(userB, "Foreign Card", "9999.00", "9999.00");

        JsonNode response = read(mockMvc.perform(authenticated(get(BASE), userA)), 200);

        assertThat(response.isArray()).isTrue();
        assertThat(response).hasSize(2);
        assertThat(response.get(0).path("id").asString()).isEqualTo(first.getId().toString());
        assertThat(response.get(1).path("id").asString()).isEqualTo(second.getId().toString());
        assertThat(response.valueStream().noneMatch(item -> item.has("userId"))).isTrue();
    }

    @Test
    void shouldReturnAnOwnedCardAndHideForeignOrUnknownCards() throws Exception {
        CreditCard own = saveCard(userA, "Own Card", "5000.00", "5000.00");
        CreditCard foreign = saveCard(userB, "Foreign Card", "3000.00", "3000.00");

        mockMvc.perform(authenticated(get(BASE + "/{id}", own.getId()), userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(own.getId().toString()))
                .andExpect(jsonPath("$.userId").doesNotExist());

        for (UUID hiddenId : List.of(foreign.getId(), UUID.randomUUID())) {
            mockMvc.perform(authenticated(get(BASE + "/{id}", hiddenId), userA))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_FOUND"));
        }
    }

    @Test
    void shouldUpdateAllMutableFieldsAndPreserveTheCommittedLimit() throws Exception {
        CreditCard card = saveCard(userA, "Old Card", "5000.00", "3500.00");
        String json = objectMapper.writeValueAsString(Map.of(
                "name", "Travel Card",
                "creditLimit", new BigDecimal("6500.00"),
                "closingDay", 12,
                "dueDay", 19,
                "defaultAccountId", userA.otherAccountId(),
                "status", "BLOCKED"
        ));

        JsonNode response = read(mockMvc.perform(authenticated(
                patch(BASE + "/{id}", card.getId()),
                userA
        ).content(json)), 200);

        assertThat(response.path("name").asString()).isEqualTo("Travel Card");
        assertMoney(response, "creditLimit", "6500.00");
        assertMoney(response, "availableLimit", "5000.00");
        assertThat(response.path("closingDay").asInt()).isEqualTo(12);
        assertThat(response.path("dueDay").asInt()).isEqualTo(19);
        assertThat(response.path("defaultAccountId").asString())
                .isEqualTo(userA.otherAccountId().toString());
        assertThat(response.path("status").asString()).isEqualTo("BLOCKED");
        assertThat(response.path("version").asLong()).isEqualTo(1L);
    }

    @Test
    void shouldRejectEmptyOrBlankUpdatesWithoutChangingTheCard() throws Exception {
        CreditCard card = saveCard(userA, "Original", "5000.00", "5000.00");

        mockMvc.perform(authenticated(patch(BASE + "/{id}", card.getId()), userA)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CREDIT_CARD_UPDATE"));

        mockMvc.perform(authenticated(patch(BASE + "/{id}", card.getId()), userA)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CREDIT_CARD_UPDATE"));

        CreditCard persisted = creditCardRepository.findById(card.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Original");
        assertThat(persisted.getVersion()).isZero();
    }

    @Test
    void shouldRejectLimitBelowCommittedAmountAndRollbackThePatch() throws Exception {
        CreditCard card = saveCard(userA, "Card", "5000.00", "3000.00");

        mockMvc.perform(authenticated(patch(BASE + "/{id}", card.getId()), userA)
                        .content("{\"creditLimit\":1999.99,\"name\":\"Must rollback\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CREDIT_LIMIT_CONFLICT"));

        CreditCard persisted = creditCardRepository.findById(card.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Card");
        assertThat(persisted.getCreditLimit()).isEqualByComparingTo("5000.00");
        assertThat(persisted.getAvailableLimit()).isEqualByComparingTo("3000.00");
    }

    @Test
    void shouldHideForeignCardsAndAccountsDuringUpdate() throws Exception {
        CreditCard foreign = saveCard(userB, "Foreign", "5000.00", "5000.00");

        mockMvc.perform(authenticated(patch(BASE + "/{id}", foreign.getId()), userA)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_FOUND"));

        CreditCard own = saveCard(userA, "Own", "5000.00", "5000.00");
        mockMvc.perform(authenticated(patch(BASE + "/{id}", own.getId()), userA)
                        .content("{\"defaultAccountId\":\"" + userB.accountId() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));

        assertThat(creditCardRepository.findById(foreign.getId()).orElseThrow().getName())
                .isEqualTo("Foreign");
        assertThat(creditCardRepository.findById(own.getId()).orElseThrow().getDefaultAccountId())
                .isEqualTo(userA.accountId());
    }

    @Test
    void shouldRejectUnknownStatusAndMalformedCardIdAsInvalidRequests() throws Exception {
        CreditCard card = saveCard(userA, "Card", "5000.00", "5000.00");

        mockMvc.perform(authenticated(patch(BASE + "/{id}", card.getId()), userA)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(authenticated(get(BASE + "/not-a-uuid"), userA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldRequireJwtForEveryCreditCardEndpoint() throws Exception {
        CreditCard card = saveCard(userA, "Card", "5000.00", "5000.00");
        List<MockHttpServletRequestBuilder> requests = List.of(
                post(BASE).content(createJson("Card", "5000.00", 10, 17, userA.accountId())),
                get(BASE),
                get(BASE + "/{id}", card.getId()),
                patch(BASE + "/{id}", card.getId()).content("{\"name\":\"New\"}")
        );

        for (MockHttpServletRequestBuilder request : requests) {
            mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void shouldAllowOnlyOneOfTwoConcurrentUpdatesToCommit() throws Exception {
        CreditCard card = saveCard(userA, "Concurrent", "5000.00", "5000.00");
        long initialVersion = card.getVersion();
        CountDownLatch bothLoaded = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Throwable> first = executor.submit(
                    () -> runConcurrentUpdate(card.getId(), userA.id(), "First", bothLoaded)
            );
            Future<Throwable> second = executor.submit(
                    () -> runConcurrentUpdate(card.getId(), userA.id(), "Second", bothLoaded)
            );

            List<Throwable> failures = Stream.of(
                            first.get(15, TimeUnit.SECONDS),
                            second.get(15, TimeUnit.SECONDS)
                    )
                    .filter(Objects::nonNull)
                    .toList();

            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst())
                    .isInstanceOf(OptimisticLockingFailureException.class);

            CreditCard persisted = creditCardRepository.findById(card.getId()).orElseThrow();
            assertThat(persisted.getName()).isIn("First", "Second");
            assertThat(persisted.getVersion()).isEqualTo(initialVersion + 1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Throwable runConcurrentUpdate(
            UUID cardId,
            UUID userId,
            String name,
            CountDownLatch bothLoaded
    ) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                CreditCard card = creditCardRepository
                        .findByIdAndUserId(cardId, userId)
                        .orElseThrow();
                bothLoaded.countDown();
                await(bothLoaded);
                card.setName(name);
                creditCardRepository.saveAndFlush(card);
            });
            return null;
        } catch (Throwable exception) {
            return exception;
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout waiting for concurrent transactions");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent test interrupted", exception);
        }
    }

    private CreditCard saveCard(
            TestUser owner,
            String name,
            String creditLimit,
            String availableLimit
    ) {
        CreditCard card = new CreditCard();
        card.setUserId(owner.id());
        card.setName(name);
        card.setCreditLimit(new BigDecimal(creditLimit));
        card.setAvailableLimit(new BigDecimal(availableLimit));
        card.setClosingDay(10);
        card.setDueDay(17);
        card.setDefaultAccountId(owner.accountId());
        card.setStatus(CreditCardStatus.ACTIVE);
        return creditCardRepository.saveAndFlush(card);
    }

    private TestUser createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("credit-card-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        User saved = userRepository.saveAndFlush(user);
        Account account = createAccount(saved.getId(), "Main");
        Account otherAccount = createAccount(saved.getId(), "Other");
        String token = jwtService.generateToken(
                userDetailsService.loadUserByUsername(saved.getEmail())
        );
        return new TestUser(
                saved.getId(),
                token,
                account.getId(),
                otherAccount.getId()
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

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request,
            TestUser user
    ) {
        return request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON);
    }

    private String createJson(
            String name,
            String limit,
            int closingDay,
            int dueDay,
            UUID accountId
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "creditLimit", new BigDecimal(limit),
                "closingDay", closingDay,
                "dueDay", dueDay,
                "defaultAccountId", accountId
        ));
    }

    private JsonNode read(ResultActions result, int expectedStatus) throws Exception {
        String json = result
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json);
    }

    private void assertMoney(JsonNode node, String field, String expected) {
        assertThat(node.path(field).decimalValue()).isEqualByComparingTo(expected);
    }

    private record TestUser(
            UUID id,
            String token,
            UUID accountId,
            UUID otherAccountId
    ) {
    }

    private record InvalidCreateCase(
            String name,
            String limit,
            int closingDay,
            int dueDay,
            String field
    ) {
    }
}
