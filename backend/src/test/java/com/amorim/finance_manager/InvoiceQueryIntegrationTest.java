package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.repository.AccountRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
import java.util.List;
import java.util.UUID;

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
class InvoiceQueryIntegrationTest {

    private static final String INVOICES = "/api/v1/invoices";

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
    private InvoiceRepository invoiceRepository;
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
        userA = createUser("Invoice Query A");
        userB = createUser("Invoice Query B");
    }

    @Test
    void shouldListOnlyAuthenticatedUsersInvoicesAsSummaries() throws Exception {
        Invoice september = saveInvoice(userA.cardId(), 9, 2026, InvoiceStatus.OPEN, "250.00");
        Invoice august = saveInvoice(userA.cardId(), 8, 2026, InvoiceStatus.CLOSED, "180.00");
        saveInvoice(userB.cardId(), 9, 2026, InvoiceStatus.OPEN, "999.00");

        JsonNode page = read(mockMvc.perform(authenticated(get(INVOICES), userA)), 200);

        assertThat(ids(page)).containsExactly(september.getId(), august.getId());
        assertPage(page, 0, 20, 2, 1, true, true);
        for (JsonNode summary : page.path("content")) {
            assertThat(summary.has("transactions")).isFalse();
            assertThat(summary.path("creditCardId").asString()).isEqualTo(userA.cardId().toString());
            assertThat(summary.path("totalAmount").isNumber()).isTrue();
            assertThat(summary.path("status").asString()).isNotBlank();
        }
    }

    @Test
    void shouldApplyCardYearMonthAndStatusFiltersIndividuallyAndTogether() throws Exception {
        Invoice expected = saveInvoice(userA.cardId(), 9, 2026, InvoiceStatus.OPEN, "100.00");
        Invoice otherMonth = saveInvoice(userA.cardId(), 8, 2026, InvoiceStatus.CLOSED, "200.00");
        Invoice otherYear = saveInvoice(userA.cardId(), 9, 2025, InvoiceStatus.PAID, "300.00");
        Invoice otherCard = saveInvoice(userA.otherCardId(), 9, 2026, InvoiceStatus.OPEN, "400.00");

        assertThat(ids(search(userA, "creditCardId", userA.cardId().toString())))
                .containsExactlyInAnyOrder(expected.getId(), otherMonth.getId(), otherYear.getId());
        assertThat(ids(search(userA, "referenceYear", "2025")))
                .containsExactly(otherYear.getId());
        assertThat(ids(search(userA, "referenceMonth", "8")))
                .containsExactly(otherMonth.getId());
        assertThat(ids(search(userA, "status", "CLOSED")))
                .containsExactly(otherMonth.getId());
        assertThat(ids(search(
                userA,
                "creditCardId", userA.cardId().toString(),
                "referenceYear", "2026",
                "referenceMonth", "9",
                "status", "OPEN"
        ))).containsExactly(expected.getId()).doesNotContain(otherCard.getId());
    }

    @Test
    void shouldReturnEmptyPageWhenGeneralFilterUsesAnotherUsersCard() throws Exception {
        saveInvoice(userB.cardId(), 9, 2026, InvoiceStatus.OPEN, "500.00");

        JsonNode page = search(userA, "creditCardId", userB.cardId().toString());

        assertThat(ids(page)).isEmpty();
        assertPage(page, 0, 20, 0, 0, true, true);
    }

    @Test
    void shouldPaginateAndSortInvoicesWithoutCountingAnotherUsersData() throws Exception {
        List<Invoice> own = List.of(
                saveInvoice(userA.cardId(), 1, 2026, InvoiceStatus.OPEN, "10.00"),
                saveInvoice(userA.cardId(), 2, 2026, InvoiceStatus.OPEN, "20.00"),
                saveInvoice(userA.cardId(), 3, 2026, InvoiceStatus.OPEN, "30.00")
        );
        saveInvoice(userB.cardId(), 4, 2026, InvoiceStatus.OPEN, "40.00");

        JsonNode first = search(userA, "page", "0", "size", "2", "sort", "referenceMonth,asc");
        JsonNode second = search(userA, "page", "1", "size", "2", "sort", "referenceMonth,asc");

        assertThat(ids(first)).containsExactly(own.get(0).getId(), own.get(1).getId());
        assertThat(ids(second)).containsExactly(own.get(2).getId());
        assertPage(first, 0, 2, 3, 2, true, false);
        assertPage(second, 1, 2, 3, 2, false, true);
    }

    @Test
    void shouldListOnlyInvoicesFromOwnedCardAndApplyOptionalFilters() throws Exception {
        Invoice expected = saveInvoice(userA.cardId(), 9, 2026, InvoiceStatus.CLOSED, "350.00");
        saveInvoice(userA.cardId(), 8, 2026, InvoiceStatus.OPEN, "150.00");
        saveInvoice(userA.otherCardId(), 9, 2026, InvoiceStatus.CLOSED, "750.00");

        JsonNode page = read(mockMvc.perform(authenticated(get(
                "/api/v1/credit-cards/{id}/invoices",
                userA.cardId()
        ).param("referenceYear", "2026")
                .param("referenceMonth", "9")
                .param("status", "CLOSED"), userA)), 200);

        assertThat(ids(page)).containsExactly(expected.getId());
        assertPage(page, 0, 20, 1, 1, true, true);
    }

    @Test
    void shouldReturnInvoiceDetailsWithOnlyItsOwnedTransactionsInChronologicalOrder() throws Exception {
        Invoice invoice = saveInvoice(userA.cardId(), 9, 2026, InvoiceStatus.OPEN, "350.00");
        Invoice otherInvoice = saveInvoice(userA.cardId(), 8, 2026, InvoiceStatus.OPEN, "50.00");
        Transaction second = saveTransaction(
                userA.id(),
                userA.cardId(),
                invoice.getId(),
                "Second purchase",
                "200.00",
                LocalDate.of(2026, 9, 8)
        );
        Transaction first = saveTransaction(
                userA.id(),
                userA.cardId(),
                invoice.getId(),
                "First purchase",
                "150.00",
                LocalDate.of(2026, 9, 2)
        );
        saveTransaction(
                userA.id(),
                userA.cardId(),
                otherInvoice.getId(),
                "Other invoice",
                "50.00",
                LocalDate.of(2026, 8, 2)
        );
        saveTransaction(
                userB.id(),
                userB.cardId(),
                invoice.getId(),
                "Must stay hidden",
                "999.00",
                LocalDate.of(2026, 9, 1)
        );

        JsonNode detail = read(mockMvc.perform(authenticated(
                get(INVOICES + "/{id}", invoice.getId()),
                userA
        )), 200);

        assertThat(detail.path("id").asString()).isEqualTo(invoice.getId().toString());
        assertThat(detail.path("creditCardId").asString()).isEqualTo(userA.cardId().toString());
        assertThat(detail.path("referenceMonth").asInt()).isEqualTo(9);
        assertThat(detail.path("referenceYear").asInt()).isEqualTo(2026);
        assertThat(detail.path("totalAmount").decimalValue()).isEqualByComparingTo("350.00");
        assertThat(transactionIds(detail)).containsExactly(first.getId(), second.getId());
        for (JsonNode transaction : detail.path("transactions")) {
            assertThat(transaction.has("userId")).isFalse();
            assertThat(transaction.path("invoiceId").asString()).isEqualTo(invoice.getId().toString());
        }
    }

    @Test
    void shouldReturnEmptyTransactionListWhenInvoiceHasNoTransactions() throws Exception {
        Invoice invoice = saveInvoice(userA.cardId(), 9, 2026, InvoiceStatus.OPEN, "0.00");

        JsonNode detail = read(mockMvc.perform(authenticated(
                get(INVOICES + "/{id}", invoice.getId()),
                userA
        )), 200);

        assertThat(detail.path("transactions").isArray()).isTrue();
        assertThat(detail.path("transactions")).isEmpty();
    }

    @Test
    void shouldHideMissingAndForeignInvoicesBehindTheSameNotFoundResponse() throws Exception {
        Invoice foreign = saveInvoice(userB.cardId(), 9, 2026, InvoiceStatus.OPEN, "500.00");

        for (UUID id : List.of(UUID.randomUUID(), foreign.getId())) {
            mockMvc.perform(authenticated(get(INVOICES + "/{id}", id), userA))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("INVOICE_NOT_FOUND"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.path").value(INVOICES + "/" + id));
        }
    }

    @Test
    void shouldHideMissingAndForeignCardsBehindTheSameNotFoundResponse() throws Exception {
        for (UUID id : List.of(UUID.randomUUID(), userB.cardId())) {
            mockMvc.perform(authenticated(
                            get("/api/v1/credit-cards/{id}/invoices", id),
                            userA
                    ))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_FOUND"))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "referenceMonth=0",
            "referenceMonth=13",
            "referenceYear=0",
            "referenceYear=10000"
    })
    void shouldRejectInvalidGeneralInvoiceFilters(String query) throws Exception {
        mockMvc.perform(authenticated(get(INVOICES + "?" + query), userA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "referenceMonth=0",
            "referenceMonth=13",
            "referenceYear=0",
            "referenceYear=10000"
    })
    void shouldRejectInvalidCardInvoiceFilters(String query) throws Exception {
        mockMvc.perform(authenticated(get(
                        "/api/v1/credit-cards/{id}/invoices?" + query,
                        userA.cardId()
                ), userA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectMalformedUuidAndUnknownStatusAsInvalidRequests() throws Exception {
        mockMvc.perform(authenticated(get(INVOICES + "/not-a-uuid"), userA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(authenticated(get(INVOICES).param("status", "OVERDUE"), userA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(authenticated(get(
                        "/api/v1/credit-cards/{id}/invoices",
                        userA.cardId()
                ).param("status", "OVERDUE"), userA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldRequireJwtForEveryInvoiceEndpoint() throws Exception {
        List<MockHttpServletRequestBuilder> requests = List.of(
                get(INVOICES),
                get(INVOICES + "/{id}", UUID.randomUUID()),
                get("/api/v1/credit-cards/{id}/invoices", UUID.randomUUID())
        );

        for (MockHttpServletRequestBuilder request : requests) {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.status").value(401));
        }
    }

    private JsonNode search(TestUser user, String... parameters) throws Exception {
        MockHttpServletRequestBuilder request = get(INVOICES);
        for (int index = 0; index < parameters.length; index += 2) {
            request.param(parameters[index], parameters[index + 1]);
        }
        return read(mockMvc.perform(authenticated(request, user)), 200);
    }

    private List<UUID> ids(JsonNode page) {
        List<UUID> ids = new ArrayList<>();
        for (JsonNode item : page.path("content")) {
            ids.add(UUID.fromString(item.path("id").asString()));
        }
        return ids;
    }

    private List<UUID> transactionIds(JsonNode detail) {
        List<UUID> ids = new ArrayList<>();
        for (JsonNode item : detail.path("transactions")) {
            ids.add(UUID.fromString(item.path("id").asString()));
        }
        return ids;
    }

    private void assertPage(
            JsonNode page,
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
        assertThat(page.path("page").asInt()).isEqualTo(number);
        assertThat(page.path("size").asInt()).isEqualTo(size);
        assertThat(page.path("totalElements").asLong()).isEqualTo(totalElements);
        assertThat(page.path("totalPages").asInt()).isEqualTo(totalPages);
        assertThat(page.path("first").asBoolean()).isEqualTo(first);
        assertThat(page.path("last").asBoolean()).isEqualTo(last);
    }

    private JsonNode read(ResultActions result, int expectedStatus) throws Exception {
        String json = result
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json);
    }

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request,
            TestUser user
    ) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token());
    }

    private TestUser createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("invoice-query-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        User saved = userRepository.saveAndFlush(user);
        Account account = createAccount(saved.getId());
        CreditCard card = createCard(saved.getId(), account.getId(), "Main");
        CreditCard otherCard = createCard(saved.getId(), account.getId(), "Other");
        String token = jwtService.generateToken(
                userDetailsService.loadUserByUsername(saved.getEmail())
        );
        return new TestUser(saved.getId(), token, card.getId(), otherCard.getId());
    }

    private Account createAccount(UUID userId) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName("Invoice query account");
        account.setType(AccountType.CHECKING);
        account.setInitialBalance(new BigDecimal("1000.00"));
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        return accountRepository.saveAndFlush(account);
    }

    private CreditCard createCard(UUID userId, UUID accountId, String name) {
        CreditCard card = new CreditCard();
        card.setUserId(userId);
        card.setName(name + " " + UUID.randomUUID());
        card.setCreditLimit(new BigDecimal("5000.00"));
        card.setAvailableLimit(new BigDecimal("5000.00"));
        card.setClosingDay(10);
        card.setDueDay(17);
        card.setDefaultAccountId(accountId);
        card.setStatus(CreditCardStatus.ACTIVE);
        return creditCardRepository.saveAndFlush(card);
    }

    private Invoice saveInvoice(
            UUID creditCardId,
            int referenceMonth,
            int referenceYear,
            InvoiceStatus status,
            String totalAmount
    ) {
        LocalDate closingDate = LocalDate.of(referenceYear, referenceMonth, 10);
        Invoice invoice = new Invoice();
        invoice.setCreditCardId(creditCardId);
        invoice.setReferenceMonth(referenceMonth);
        invoice.setReferenceYear(referenceYear);
        invoice.setClosingDate(closingDate);
        invoice.setDueDate(closingDate.plusDays(7));
        invoice.setTotalAmount(new BigDecimal(totalAmount));
        invoice.setStatus(status);
        if (status == InvoiceStatus.PAID) {
            invoice.setPaidAt(Instant.parse("2026-09-17T12:00:00Z"));
        }
        return invoiceRepository.saveAndFlush(invoice);
    }

    private Transaction saveTransaction(
            UUID userId,
            UUID creditCardId,
            UUID invoiceId,
            String description,
            String amount,
            LocalDate competenceDate
    ) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setDescription(description);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCompetenceDate(competenceDate);
        transaction.setDueDate(competenceDate.plusDays(30));
        transaction.setType(TransactionType.CREDIT_CARD_PURCHASE);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        transaction.setCreditCardId(creditCardId);
        transaction.setInvoiceId(invoiceId);
        transaction.setInstallmentNumber(1);
        transaction.setInstallmentCount(1);
        return transactionRepository.saveAndFlush(transaction);
    }

    private record TestUser(
            UUID id,
            String token,
            UUID cardId,
            UUID otherCardId
    ) {
    }
}
