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
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.security.JwtService;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import com.amorim.finance_manager.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class CreditCardPurchaseIntegrationTest {

    private static final String PURCHASE_PATH = "/api/v1/credit-cards/{id}/purchases";
    private static final LocalDate PURCHASE_DATE = LocalDate.of(2026, 9, 11);

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
    private CreditCardRepository creditCardRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TestUser userA;
    private TestUser userB;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        userA = createUser("Purchase User A");
        userB = createUser("Purchase User B");
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    invoices_aud,
                    transactions_aud,
                    credit_cards_aud,
                    accounts_aud,
                    audit_revision,
                    transactions,
                    invoices,
                    credit_cards,
                    categories,
                    accounts,
                    users
                CASCADE
                """
        );
    }

    @Test
    void shouldCreatePurchaseInTheNextCycleAndNeverChangeTheAccountBalance() throws Exception {
        CreditCard card = createCard(userA, CreditCardStatus.ACTIVE, "1000.00", "1000.00");

        MvcResult result = mockMvc.perform(post(PURCHASE_PATH, card.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson("Supermercado", "250.00", PURCHASE_DATE, userA.expenseCategoryId())))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Supermercado"))
                .andExpect(jsonPath("$[0].amount").value(250.00))
                .andExpect(jsonPath("$[0].competenceDate").value("2026-09-11"))
                .andExpect(jsonPath("$[0].effectiveDate").doesNotExist())
                .andExpect(jsonPath("$[0].type").value("CREDIT_CARD_PURCHASE"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$[0].sourceAccountId").doesNotExist())
                .andExpect(jsonPath("$[0].destinationAccountId").doesNotExist())
                .andExpect(jsonPath("$[0].categoryId").value(userA.expenseCategoryId().toString()))
                .andExpect(jsonPath("$[0].creditCardId").value(card.getId().toString()))
                .andExpect(jsonPath("$[0].invoiceId").isNotEmpty())
                .andExpect(jsonPath("$[0].installmentGroupId").isNotEmpty())
                .andExpect(jsonPath("$[0].installmentNumber").value(1))
                .andExpect(jsonPath("$[0].installmentCount").value(1))
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)
        ).get(0);
        UUID transactionId = UUID.fromString(response.path("id").asString());
        UUID invoiceId = UUID.fromString(response.path("invoiceId").asString());

        CreditCard persistedCard = creditCardRepository.findById(card.getId()).orElseThrow();
        assertThat(persistedCard.getAvailableLimit()).isEqualByComparingTo("750.00");
        assertThat(persistedCard.getVersion()).isEqualTo(1L);

        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(invoice.getCreditCardId()).isEqualTo(card.getId());
        assertThat(invoice.getReferenceMonth()).isEqualTo(10);
        assertThat(invoice.getReferenceYear()).isEqualTo(2026);
        assertThat(invoice.getClosingDate()).isEqualTo("2026-10-10");
        assertThat(invoice.getDueDate()).isEqualTo("2026-10-17");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("250.00");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OPEN);

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();
        assertThat(transaction.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(transaction.getEffectiveDate()).isNull();
        assertThat(transaction.getType()).isEqualTo(TransactionType.CREDIT_CARD_PURCHASE);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);

        Account account = accountRepository.findById(userA.accountId()).orElseThrow();
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldCreateTwoInstallmentsInConsecutiveInvoices() throws Exception {
        CreditCard card = createCard(userA, CreditCardStatus.ACTIVE, "1000.00", "1000.00");

        MvcResult result = performPurchase(
                userA,
                card.getId(),
                "Notebook",
                "100.00",
                PURCHASE_DATE,
                2
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(50.00))
                .andExpect(jsonPath("$[1].amount").value(50.00))
                .andExpect(jsonPath("$[0].installmentNumber").value(1))
                .andExpect(jsonPath("$[1].installmentNumber").value(2))
                .andExpect(jsonPath("$[0].installmentCount").value(2))
                .andExpect(jsonPath("$[1].installmentCount").value(2))
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
        assertThat(response.get(0).path("installmentGroupId").asString())
                .isEqualTo(response.get(1).path("installmentGroupId").asString());
        assertThat(response.get(0).path("invoiceId").asString())
                .isNotEqualTo(response.get(1).path("invoiceId").asString());

        List<Invoice> invoices = invoicesFor(card.getId());
        assertThat(invoices).extracting(Invoice::getReferenceMonth)
                .containsExactly(10, 11);
        assertThat(invoices).extracting(Invoice::getReferenceYear)
                .containsOnly(2026);
        assertThat(invoices).extracting(Invoice::getTotalAmount)
                .containsExactly(new BigDecimal("50.00"), new BigDecimal("50.00"));
        assertThat(creditCardRepository.findById(card.getId()).orElseThrow().getAvailableLimit())
                .isEqualByComparingTo("900.00");
    }

    @Test
    void shouldKeepTheExactTotalWhenThreeInstallmentsAreNotEvenlyDivisible() throws Exception {
        CreditCard card = createCard(userA, CreditCardStatus.ACTIVE, "1000.00", "1000.00");

        performPurchase(
                userA,
                card.getId(),
                "Non-divisible purchase",
                "100.00",
                PURCHASE_DATE,
                3
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].amount").value(33.33))
                .andExpect(jsonPath("$[1].amount").value(33.33))
                .andExpect(jsonPath("$[2].amount").value(33.34));

        List<Transaction> installments = installmentsFor(card.getId());
        assertThat(installments).extracting(Transaction::getInstallmentNumber)
                .containsExactly(1, 2, 3);
        assertThat(installments).extracting(Transaction::getInstallmentCount)
                .containsOnly(3);
        assertThat(installments.stream()
                .map(Transaction::getInstallmentGroupId)
                .distinct())
                .hasSize(1)
                .doesNotContainNull();

        BigDecimal transactionTotal = installments.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal invoiceTotal = invoicesFor(card.getId()).stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(transactionTotal).isEqualByComparingTo("100.00");
        assertThat(invoiceTotal).isEqualByComparingTo("100.00");
        assertThat(installments.getLast().getAmount()).isEqualByComparingTo("33.34");
    }

    @Test
    void shouldCreateTwelveInstallmentsAcrossTheYearBoundary() throws Exception {
        CreditCard card = createCard(userA, CreditCardStatus.ACTIVE, "1000.00", "1000.00");
        LocalDate decemberPurchase = LocalDate.of(2026, 12, 11);

        performPurchase(
                userA,
                card.getId(),
                "Annual purchase",
                "120.00",
                decemberPurchase,
                12
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].installmentNumber").value(1))
                .andExpect(jsonPath("$[11].installmentNumber").value(12))
                .andExpect(jsonPath("$[11].installmentCount").value(12));

        List<Invoice> invoices = invoicesFor(card.getId());
        assertThat(invoices).hasSize(12);
        assertThat(invoices).extracting(Invoice::getReferenceMonth)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(invoices).extracting(Invoice::getReferenceYear)
                .containsOnly(2027);
        assertThat(invoices).extracting(Invoice::getTotalAmount)
                .containsOnly(new BigDecimal("10.00"));

        List<Transaction> installments = installmentsFor(card.getId());
        assertThat(installments).hasSize(12);
        assertThat(installments.getFirst().getCompetenceDate())
                .isEqualTo(LocalDate.of(2026, 12, 11));
        assertThat(installments.getLast().getCompetenceDate())
                .isEqualTo(LocalDate.of(2027, 11, 11));
        assertThat(creditCardRepository.findById(card.getId()).orElseThrow().getAvailableLimit())
                .isEqualByComparingTo("880.00");
        assertThat(accountRepository.findById(userA.accountId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldAccumulateSequentialPurchasesInTheSameInvoice() throws Exception {
        CreditCard card = createCard(userA, CreditCardStatus.ACTIVE, "1000.00", "1000.00");

        performPurchase(userA, card.getId(), "First", "125.25", LocalDate.of(2026, 9, 9))
                .andExpect(status().isCreated());
        performPurchase(userA, card.getId(), "Second", "74.75", LocalDate.of(2026, 9, 10))
                .andExpect(status().isCreated());

        Invoice invoice = invoiceRepository
                .findByCreditCardIdAndReferenceMonthAndReferenceYear(card.getId(), 9, 2026)
                .orElseThrow();

        assertThat(invoiceRepository.count()).isEqualTo(1L);
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("200.00");
        assertThat(transactionRepository.count()).isEqualTo(2L);
        assertThat(creditCardRepository.findById(card.getId()).orElseThrow().getAvailableLimit())
                .isEqualByComparingTo("800.00");
        assertThat(accountRepository.findById(userA.accountId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldValidateTheRequestBeforePersistingAnything() throws Exception {
        CreditCard card = createCard(userA, CreditCardStatus.ACTIVE, "1000.00", "1000.00");
        List<InvalidRequest> invalidRequests = List.of(
                new InvalidRequest(
                        "{\"description\":\"   \",\"amount\":10.00,\"purchaseDate\":\"2026-09-11\",\"categoryId\":\""
                                + userA.expenseCategoryId() + "\",\"installmentCount\":1}",
                        "description"
                ),
                new InvalidRequest(
                        "{\"description\":\"Compra\",\"amount\":0,\"purchaseDate\":\"2026-09-11\",\"categoryId\":\""
                                + userA.expenseCategoryId() + "\",\"installmentCount\":1}",
                        "amount"
                ),
                new InvalidRequest(
                        "{\"description\":\"Compra\",\"amount\":10.00,\"categoryId\":\""
                                + userA.expenseCategoryId() + "\",\"installmentCount\":1}",
                        "purchaseDate"
                ),
                new InvalidRequest(
                        "{\"description\":\"Compra\",\"amount\":10.00,\"purchaseDate\":\"2026-09-11\","
                                + "\"installmentCount\":1}",
                        "categoryId"
                ),
                new InvalidRequest(
                        "{\"description\":\"Compra\",\"amount\":10.00,\"purchaseDate\":\"2026-09-11\",\"categoryId\":\""
                                + userA.expenseCategoryId() + "\"}",
                        "installmentCount"
                ),
                new InvalidRequest(
                        "{\"description\":\"Compra\",\"amount\":10.00,\"purchaseDate\":\"2026-09-11\",\"categoryId\":\""
                                + userA.expenseCategoryId() + "\",\"installmentCount\":0}",
                        "installmentCount"
                )
        );

        for (InvalidRequest invalid : invalidRequests) {
            mockMvc.perform(post(PURCHASE_PATH, card.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalid.json()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem(invalid.field())));
        }

        assertThat(transactionRepository.count()).isZero();
        assertThat(invoiceRepository.count()).isZero();
        assertThat(creditCardRepository.findById(card.getId()).orElseThrow().getAvailableLimit())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldHideForeignCardsAndCategoriesAsNotFound() throws Exception {
        CreditCard ownCard = createCard(userA, CreditCardStatus.ACTIVE, "1000.00", "1000.00");
        CreditCard foreignCard = createCard(userB, CreditCardStatus.ACTIVE, "1000.00", "1000.00");

        performPurchase(userA, foreignCard.getId(), "Foreign card", "10.00", PURCHASE_DATE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_FOUND"));

        mockMvc.perform(post(PURCHASE_PATH, ownCard.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(
                                "Foreign category",
                                "10.00",
                                PURCHASE_DATE,
                                userB.expenseCategoryId()
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        assertThat(transactionRepository.count()).isZero();
        assertThat(invoiceRepository.count()).isZero();
    }

    @Test
    void shouldRejectInvalidCardAndCategoryStates() throws Exception {
        for (CreditCardStatus status : List.of(CreditCardStatus.INACTIVE, CreditCardStatus.BLOCKED)) {
            CreditCard card = createCard(userA, status, "1000.00", "1000.00");
            performPurchase(userA, card.getId(), "Unavailable card", "10.00", PURCHASE_DATE)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDIT_CARD_STATUS"));
        }

        CreditCard card = createCard(userA, CreditCardStatus.ACTIVE, "1000.00", "1000.00");
        mockMvc.perform(post(PURCHASE_PATH, card.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(
                                "Income category",
                                "10.00",
                                PURCHASE_DATE,
                                userA.incomeCategoryId()
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CATEGORY_TYPE_MISMATCH"));

        Category inactiveCategory = createCategory(
                userA.id(),
                "Inactive expense",
                CategoryType.EXPENSE,
                CategoryStatus.INACTIVE
        );
        mockMvc.perform(post(PURCHASE_PATH, card.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(
                                "Inactive category",
                                "10.00",
                                PURCHASE_DATE,
                                inactiveCategory.getId()
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSACTION"));

        assertThat(transactionRepository.count()).isZero();
        assertThat(invoiceRepository.count()).isZero();
    }

    @Test
    void shouldRollbackEveryChangeWhenLimitOrInvoiceStateIsInvalid() throws Exception {
        CreditCard insufficient = createCard(
                userA,
                CreditCardStatus.ACTIVE,
                "1000.00",
                "99.99"
        );
        performPurchase(userA, insufficient.getId(), "Too expensive", "100.00", PURCHASE_DATE, 12)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CREDIT_LIMIT_CONFLICT"));

        CreditCard closedInvoiceCard = createCard(
                userA,
                CreditCardStatus.ACTIVE,
                "1000.00",
                "1000.00"
        );
        createInvoice(closedInvoiceCard.getId(), InvoiceStatus.CLOSED, "50.00");
        performPurchase(userA, closedInvoiceCard.getId(), "Closed invoice", "100.00", PURCHASE_DATE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_INVOICE_STATUS"));

        CreditCard persistedInsufficient = creditCardRepository
                .findById(insufficient.getId())
                .orElseThrow();
        CreditCard persistedClosed = creditCardRepository
                .findById(closedInvoiceCard.getId())
                .orElseThrow();
        Invoice closedInvoice = invoiceRepository
                .findByCreditCardIdAndReferenceMonthAndReferenceYear(
                        closedInvoiceCard.getId(),
                        10,
                        2026
                )
                .orElseThrow();

        assertThat(persistedInsufficient.getAvailableLimit()).isEqualByComparingTo("99.99");
        assertThat(persistedInsufficient.getVersion()).isZero();
        assertThat(persistedClosed.getAvailableLimit()).isEqualByComparingTo("1000.00");
        assertThat(persistedClosed.getVersion()).isZero();
        assertThat(closedInvoice.getTotalAmount()).isEqualByComparingTo("50.00");
        assertThat(transactionRepository.count()).isZero();
        assertThat(accountRepository.findById(userA.accountId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldRollbackEarlierInstallmentsWhenAFutureInvoiceIsNotOpen() throws Exception {
        CreditCard card = createCard(userA, CreditCardStatus.ACTIVE, "1000.00", "1000.00");
        Invoice closedFutureInvoice = createInvoice(
                card.getId(),
                11,
                2026,
                InvoiceStatus.CLOSED,
                "50.00"
        );

        performPurchase(
                userA,
                card.getId(),
                "Atomic installments",
                "100.00",
                PURCHASE_DATE,
                3
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_INVOICE_STATUS"));

        CreditCard persistedCard = creditCardRepository.findById(card.getId()).orElseThrow();
        Invoice persistedClosedInvoice = invoiceRepository
                .findById(closedFutureInvoice.getId())
                .orElseThrow();

        assertThat(persistedCard.getAvailableLimit()).isEqualByComparingTo("1000.00");
        assertThat(persistedCard.getVersion()).isZero();
        assertThat(persistedClosedInvoice.getTotalAmount()).isEqualByComparingTo("50.00");
        assertThat(invoiceRepository.count()).isEqualTo(1L);
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldNeverLetTwoConcurrentPurchasesSpendTheSameLimit() throws Exception {
        CreditCard card = createCard(userA, CreditCardStatus.ACTIVE, "100.00", "100.00");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<HttpResult> first = executor.submit(
                    () -> concurrentPurchase(card.getId(), "Concurrent A", ready, start)
            );
            Future<HttpResult> second = executor.submit(
                    () -> concurrentPurchase(card.getId(), "Concurrent B", ready, start)
            );

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<HttpResult> results = List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)
            );

            assertThat(results).extracting(HttpResult::status)
                    .containsExactlyInAnyOrder(201, 409);

            HttpResult conflict = results.stream()
                    .filter(result -> result.status() == 409)
                    .findFirst()
                    .orElseThrow();
            assertThat(objectMapper.readTree(conflict.body()).path("code").asString())
                    .isIn("OPTIMISTIC_LOCK_CONFLICT", "CREDIT_LIMIT_CONFLICT");

            CreditCard persistedCard = creditCardRepository.findById(card.getId()).orElseThrow();
            assertThat(persistedCard.getAvailableLimit()).isEqualByComparingTo("20.00");
            assertThat(persistedCard.getVersion()).isEqualTo(1L);
            assertThat(transactionRepository.count()).isEqualTo(1L);
            assertThat(invoiceRepository.count()).isEqualTo(1L);
            assertThat(invoiceRepository.findAll().getFirst().getTotalAmount())
                    .isEqualByComparingTo("80.00");
            assertThat(accountRepository.findById(userA.accountId()).orElseThrow().getCurrentBalance())
                    .isEqualByComparingTo("1000.00");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRequireAuthenticationAndRejectMalformedCardIds() throws Exception {
        CreditCard card = createCard(userA, CreditCardStatus.ACTIVE, "1000.00", "1000.00");

        mockMvc.perform(post(PURCHASE_PATH, card.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(
                                "Unauthenticated",
                                "10.00",
                                PURCHASE_DATE,
                                userA.expenseCategoryId()
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/credit-cards/not-a-uuid/purchases")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(
                                "Malformed ID",
                                "10.00",
                                PURCHASE_DATE,
                                userA.expenseCategoryId()
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private org.springframework.test.web.servlet.ResultActions performPurchase(
            TestUser user,
            UUID cardId,
            String description,
            String amount,
            LocalDate date
    ) throws Exception {
        return performPurchase(user, cardId, description, amount, date, 1);
    }

    private org.springframework.test.web.servlet.ResultActions performPurchase(
            TestUser user,
            UUID cardId,
            String description,
            String amount,
            LocalDate date,
            int installmentCount
    ) throws Exception {
        return mockMvc.perform(post(PURCHASE_PATH, cardId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(purchaseJson(
                        description,
                        amount,
                        date,
                        user.expenseCategoryId(),
                        installmentCount
                )));
    }

    private HttpResult concurrentPurchase(
            UUID cardId,
            String description,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timeout waiting to start concurrent purchases");
        }

        MvcResult result = performPurchase(userA, cardId, description, "80.00", PURCHASE_DATE)
                .andReturn();

        return new HttpResult(
                result.getResponse().getStatus(),
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }

    private String purchaseJson(
            String description,
            String amount,
            LocalDate date,
            UUID categoryId
    ) throws Exception {
        return purchaseJson(description, amount, date, categoryId, 1);
    }

    private String purchaseJson(
            String description,
            String amount,
            LocalDate date,
            UUID categoryId,
            int installmentCount
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "description", description,
                "amount", new BigDecimal(amount),
                "purchaseDate", date.toString(),
                "categoryId", categoryId,
                "installmentCount", installmentCount
        ));
    }

    private List<Invoice> invoicesFor(UUID cardId) {
        return invoiceRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getCreditCardId().equals(cardId))
                .sorted(Comparator
                        .comparing(Invoice::getReferenceYear)
                        .thenComparing(Invoice::getReferenceMonth))
                .toList();
    }

    private List<Transaction> installmentsFor(UUID cardId) {
        return transactionRepository.findAll()
                .stream()
                .filter(transaction -> transaction.getCreditCardId().equals(cardId))
                .sorted(Comparator.comparing(Transaction::getInstallmentNumber))
                .toList();
    }

    private TestUser createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("purchase-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        User saved = userRepository.saveAndFlush(user);

        Account account = createAccount(saved.getId());
        Category expense = createCategory(
                saved.getId(),
                name + " expense",
                CategoryType.EXPENSE,
                CategoryStatus.ACTIVE
        );
        Category income = createCategory(
                saved.getId(),
                name + " income",
                CategoryType.INCOME,
                CategoryStatus.ACTIVE
        );
        String token = jwtService.generateToken(
                userDetailsService.loadUserByUsername(saved.getEmail())
        );

        return new TestUser(
                saved.getId(),
                token,
                account.getId(),
                expense.getId(),
                income.getId()
        );
    }

    private Account createAccount(UUID userId) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName("Purchase account " + UUID.randomUUID());
        account.setType(AccountType.CHECKING);
        account.setInitialBalance(new BigDecimal("1000.00"));
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        return accountRepository.saveAndFlush(account);
    }

    private Category createCategory(
            UUID userId,
            String name,
            CategoryType type,
            CategoryStatus status
    ) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setType(type);
        category.setStatus(status);
        return categoryRepository.saveAndFlush(category);
    }

    private CreditCard createCard(
            TestUser owner,
            CreditCardStatus status,
            String creditLimit,
            String availableLimit
    ) {
        CreditCard card = new CreditCard();
        card.setUserId(owner.id());
        card.setName("Purchase card " + UUID.randomUUID());
        card.setCreditLimit(new BigDecimal(creditLimit));
        card.setAvailableLimit(new BigDecimal(availableLimit));
        card.setClosingDay(10);
        card.setDueDay(17);
        card.setDefaultAccountId(owner.accountId());
        card.setStatus(status);
        return creditCardRepository.saveAndFlush(card);
    }

    private Invoice createInvoice(
            UUID cardId,
            InvoiceStatus status,
            String totalAmount
    ) {
        return createInvoice(cardId, 10, 2026, status, totalAmount);
    }

    private Invoice createInvoice(
            UUID cardId,
            int referenceMonth,
            int referenceYear,
            InvoiceStatus status,
            String totalAmount
    ) {
        Invoice invoice = new Invoice();
        invoice.setCreditCardId(cardId);
        invoice.setReferenceMonth(referenceMonth);
        invoice.setReferenceYear(referenceYear);
        invoice.setClosingDate(LocalDate.of(referenceYear, referenceMonth, 10));
        invoice.setDueDate(LocalDate.of(referenceYear, referenceMonth, 17));
        invoice.setTotalAmount(new BigDecimal(totalAmount));
        invoice.setStatus(status);
        return invoiceRepository.saveAndFlush(invoice);
    }

    private record TestUser(
            UUID id,
            String token,
            UUID accountId,
            UUID expenseCategoryId,
            UUID incomeCategoryId
    ) {
    }

    private record InvalidRequest(String json, String field) {
    }

    private record HttpResult(int status, String body) {
    }
}
