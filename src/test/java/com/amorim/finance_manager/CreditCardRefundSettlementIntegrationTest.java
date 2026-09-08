package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.*;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.creditcard.entity.*;
import com.amorim.finance_manager.creditcard.repository.*;
import com.amorim.finance_manager.invoice.entity.*;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.security.JwtService;
import com.amorim.finance_manager.transaction.entity.*;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import com.amorim.finance_manager.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.*;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class CreditCardRefundSettlementIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant ORIGINAL_PAYMENT = Instant.parse("2026-06-28T12:00:00Z");
    private static final String REASON = "Compra devolvida ao estabelecimento";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired CreditCardRepository cards;
    @Autowired CreditCardRefundRepository refunds;
    @Autowired CreditCardRefundItemRepository refundItems;
    @Autowired JwtService jwt;
    @Autowired CustomUserDetailsService userDetails;
    @MockitoSpyBean InvoiceRepository invoices;
    @MockitoSpyBean TransactionRepository transactions;
    @MockitoSpyBean CreditCardCreditRepository credits;
    @MockitoBean(name = "financeClock") Clock clock;

    private final List<UUID> fixtureUsers = new ArrayList<>();
    private Actor owner;
    private Actor other;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZONE);
        owner = actor();
        other = actor();
    }

    @AfterEach
    void cleanOnlyFixtureData() {
        reset(invoices, transactions, credits);
        // Delete only these tests' users, in FK order. Audit history has no entity FK.
        for (UUID userId : fixtureUsers) {
            jdbc.update("DELETE FROM credit_card_credit_applications WHERE credit_id IN "
                    + "(SELECT id FROM credit_card_credits WHERE user_id = ?)", userId);
            jdbc.update("DELETE FROM credit_card_credits WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM credit_card_refund_items WHERE refund_id IN "
                    + "(SELECT id FROM credit_card_refunds WHERE user_id = ?)", userId);
            jdbc.update("DELETE FROM credit_card_refunds WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM transactions WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM invoices WHERE credit_card_id IN "
                    + "(SELECT id FROM credit_cards WHERE user_id = ?)", userId);
            jdbc.update("DELETE FROM credit_cards WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM accounts WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM users WHERE id = ?", userId);
        }
        fixtureUsers.clear();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void shouldRefundEntireMixedPurchaseFromAnyInstallment(int selected) throws Exception {
        List<Transaction> purchase = purchase(owner, "570", "100", "100", "100");
        Invoice paid = invoiceOf(purchase.get(0));
        paid.setStatus(InvoiceStatus.PAID);
        paid.setPaidAt(ORIGINAL_PAYMENT);
        invoices.saveAndFlush(paid);
        Invoice closed = invoiceOf(purchase.get(1));
        closed.setStatus(InvoiceStatus.CLOSED);
        closed.setTotalAmount(money("180"));
        invoices.saveAndFlush(closed);
        Invoice open = invoiceOf(purchase.get(2));
        open.setTotalAmount(money("250"));
        invoices.saveAndFlush(open);
        Map<String, Object> paidBefore = invoiceRow(paid.getId());
        Map<String, Object> purchaseBefore = transactionRow(purchase.get(0).getId());

        JsonNode result = refund(owner, purchase.get(selected), 200);

        assertMoney(result, "totalAmount", "300");
        assertMoney(result, "limitRestoredAmount", "200");
        assertMoney(result, "paidCompensationAmount", "100");
        assertThat(result.path("items").size()).isEqualTo(3);
        assertThat(result.path("items").get(0).path("treatment").asString())
                .isEqualTo("FUTURE_INVOICE_CREDIT");
        assertThat(result.path("items").get(1).path("creditId").isNull()).isTrue();
        assertThat(credit(result).getRemainingAmount()).isEqualByComparingTo("100");
        assertThat(card().getAvailableLimit()).isEqualByComparingTo("770");
        assertThat(account().getCurrentBalance()).isEqualByComparingTo("1000");
        assertThat(invoiceRow(paid.getId())).isEqualTo(paidBefore);
        assertThat(transactionRow(purchase.get(0).getId())).isEqualTo(purchaseBefore);
        assertThat(invoiceOf(purchase.get(1)).getTotalAmount()).isEqualByComparingTo("80");
        assertThat(invoiceOf(purchase.get(1)).getStatus()).isEqualTo(InvoiceStatus.CLOSED);
        assertThat(invoiceOf(purchase.get(2)).getTotalAmount()).isEqualByComparingTo("150");
        assertThat(invoiceOf(purchase.get(2)).getStatus()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(reload(purchase.get(1)).getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        assertThat(reload(purchase.get(2)).getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        CreditCardRefund saved = refunds.findById(UUID.fromString(result.path("id").asString())).orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(owner.id());
        assertThat(saved.getReason()).isEqualTo(REASON);
        assertThat(refundItems.findAllByRefundIdOrderByOriginalTransactionIdAsc(saved.getId())).hasSize(3);

        Snapshot after = snapshot();
        refund(owner, purchase.get((selected + 1) % 3), 409);
        assertThat(snapshot()).isEqualTo(after);
    }

    @ParameterizedTest
    @EnumSource(value = InvoiceStatus.class, names = {"OPEN", "CLOSED"})
    void shouldCancelUnpaidPurchaseWithoutLosingRoundingCents(InvoiceStatus state) throws Exception {
        List<Transaction> purchase = purchase(owner, "900", "33.34", "33.33", "33.33");
        for (Transaction transaction : purchase) {
            Invoice invoice = invoiceOf(transaction);
            invoice.setStatus(state);
            invoices.saveAndFlush(invoice);
        }
        JsonNode result = refund(owner, purchase.get(1), 200);
        assertMoney(result, "totalAmount", "100");
        assertMoney(result, "paidCompensationAmount", "0");
        assertThat(card().getAvailableLimit()).isEqualByComparingTo("1000");
        assertThat(credits.count()).isZero();
        for (Transaction transaction : purchase) {
            assertThat(reload(transaction).getStatus()).isEqualTo(TransactionStatus.CANCELLED);
            assertThat(invoiceOf(transaction).getTotalAmount()).isEqualByComparingTo("0");
            assertThat(invoiceOf(transaction).getStatus()).isEqualTo(state);
        }
    }

    @Test
    void shouldPreserveFullyPaidPurchaseAndRejectDuplicateCredit() throws Exception {
        Transaction transaction = paidPurchase(owner, "100");
        Snapshot before = snapshot();
        JsonNode result = refund(owner, transaction, 200);
        assertMoney(result, "limitRestoredAmount", "0");
        assertThat(snapshot().invoices()).isEqualTo(before.invoices());
        assertThat(snapshot().transactions()).isEqualTo(before.transactions());
        assertThat(snapshot().cards()).isEqualTo(before.cards());
        assertThat(credit(result).getOriginalAmount()).isEqualByComparingTo("100");
        Snapshot after = snapshot();
        assertThat(refund(owner, transaction, 409).path("code").asString())
                .isEqualTo("CREDIT_CARD_PURCHASE_ALREADY_REFUNDED");
        assertThat(snapshot()).isEqualTo(after);
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus.class, names = {"PENDING", "CANCELLED"})
    void shouldRejectEntireGroupWhenOneInstallmentIsIneligible(TransactionStatus status) throws Exception {
        List<Transaction> purchase = purchase(owner, "700", "100", "100", "100");
        purchase.get(1).setStatus(status);
        transactions.saveAndFlush(purchase.get(1));
        Snapshot before = snapshot();
        refund(owner, purchase.get(0), 409);
        assertThat(snapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancelledInvoice", "paidWithoutDate", "openWithDate", "totalTooSmall",
            "missingInstallment", "wrongNumber", "foreignInvoice"})
    void shouldRejectInconsistentGroupBeforeAnyEffect(String scenario) throws Exception {
        List<Transaction> purchase = purchase(owner, "700", "100", "100", "100");
        Invoice invoice = invoiceOf(purchase.get(1));
        switch (scenario) {
            case "cancelledInvoice" -> invoice.setStatus(InvoiceStatus.CANCELLED);
            case "paidWithoutDate" -> invoice.setStatus(InvoiceStatus.PAID);
            case "openWithDate" -> invoice.setPaidAt(ORIGINAL_PAYMENT);
            case "totalTooSmall" -> invoice.setTotalAmount(money("99"));
            case "missingInstallment" -> transactions.deleteById(purchase.get(1).getId());
            case "wrongNumber" -> {
                purchase.get(1).setInstallmentNumber(1);
                transactions.saveAndFlush(purchase.get(1));
            }
            case "foreignInvoice" -> {
                Invoice foreign = invoice(other, other.cardId(), 6, InvoiceStatus.OPEN, "100");
                purchase.get(1).setInvoiceId(foreign.getId());
                transactions.saveAndFlush(purchase.get(1));
            }
            default -> throw new AssertionError(scenario);
        }
        invoices.saveAndFlush(invoice);
        Snapshot before = snapshot();
        refund(owner, purchase.get(0), scenario.equals("foreignInvoice") ? 404 : 409);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldHideForeignResourcesAndWrongOwnedCard() throws Exception {
        Transaction transaction = purchase(owner, "900", "100").getFirst();
        Snapshot before = snapshot();
        refund(other, transaction, 404);
        UUID secondCard = newCard(owner.id(), owner.accountId()).getId();
        read(postAs(owner, refundPath(secondCard, transaction.getId()), Map.of("reason", REASON)), 404);
        assertThat(snapshot().transactions()).isEqualTo(before.transactions());
        assertThat(refunds.count()).isZero();
    }

    @Test
    void shouldRollbackHistoryCreditsAndBalancesAfterInvoiceFlushFails() throws Exception {
        List<Transaction> purchase = purchase(owner, "800", "100", "100", "100");
        Invoice paid = invoiceOf(purchase.get(0));
        paid.setStatus(InvoiceStatus.PAID);
        paid.setPaidAt(ORIGINAL_PAYMENT);
        invoices.saveAndFlush(paid);
        Snapshot before = snapshot();
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("Simulated failure after invoice flush");
        }).when(invoices).saveAllAndFlush(any());
        refund(owner, purchase.get(0), 500);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldConfirmOnlyOneConcurrentUnpaidRefund() throws Exception {
        Transaction transaction = purchase(owner, "900", "100").getFirst();
        synchronizeInvoiceReads(transaction.getInvoiceId());
        List<Integer> statuses = concurrent(
                () -> postAs(owner, refundPath(owner.cardId(), transaction.getId()), Map.of("reason", REASON)));
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(card().getAvailableLimit()).isEqualByComparingTo("1000");
        assertThat(refunds.count()).isEqualTo(1);
        assertThat(refundItems.count()).isEqualTo(1);
        assertThat(credits.count()).isZero();
    }

    @Test
    void shouldPreventConcurrentPaidRefundUsingDatabaseUniqueness() throws Exception {
        Transaction transaction = paidPurchase(owner, "100");
        synchronizeInvoiceReads(transaction.getInvoiceId());
        List<Integer> statuses = concurrent(
                () -> postAs(owner, refundPath(owner.cardId(), transaction.getId()), Map.of("reason", REASON)));
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(refunds.count()).isEqualTo(1);
        assertThat(refundItems.count()).isEqualTo(1);
        assertThat(credits.count()).isEqualTo(1);
        assertThat(card().getAvailableLimit()).isEqualByComparingTo("1000");
    }

    @ParameterizedTest
    @CsvSource({"50,150,50,100,0", "150,150,150,0,0", "200,150,150,0,50"})
    void shouldApplyCreditAndDebitOnlyRemainder(String issued, String total, String applied,
                                               String cash, String remaining) throws Exception {
        CreditCardCredit credit = credit(refund(owner, paidPurchase(owner, issued), 200));
        Invoice target = invoice(owner, owner.cardId(), 8, InvoiceStatus.CLOSED, total);
        available(money("1000").subtract(money(total)));
        Snapshot before = snapshot();
        Map<String, Object> body = new HashMap<>();
        body.put("expectedVersion", target.getVersion());
        if (money(cash).signum() > 0) body.put("sourceAccountId", owner.accountId());

        JsonNode result = read(postAs(owner, payPath(target), body), 200);

        assertMoney(result, "creditAppliedAmount", applied);
        assertMoney(result, "cashPaidAmount", cash);
        assertThat(account().getCurrentBalance()).isEqualByComparingTo(money("1000").subtract(money(cash)));
        assertThat(card().getAvailableLimit()).isEqualByComparingTo("1000");
        assertThat(credits.findById(credit.getId()).orElseThrow().getRemainingAmount())
                .isEqualByComparingTo(remaining);
        Invoice settled = invoices.findById(target.getId()).orElseThrow();
        assertThat(settled.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(settled.getPaidAt()).isEqualTo(NOW);
        assertThat(settled.getTotalAmount()).isEqualByComparingTo(total);
        assertThat(before.refunds()).isEqualTo(snapshot().refunds());
        JsonNode detail = read(mvc.perform(auth(get("/api/v1/invoices/{id}", target.getId()), owner))
                .andReturn(), 200);
        assertMoney(detail, "creditAppliedAmount", applied);
        if (money(cash).signum() == 0) {
            assertThat(result.path("paymentTransactionId").isNull()).isTrue();
            assertThat(paymentRows()).isEmpty();
        } else {
            Transaction payment = transactions.findById(UUID.fromString(result.path("paymentTransactionId")
                    .asString())).orElseThrow();
            assertThat(payment.getAmount()).isEqualByComparingTo(cash);
            assertThat(payment.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 9, 21));
            assertThat(payment.getSourceAccountId()).isEqualTo(owner.accountId());
        }
        JsonNode daily = read(mvc.perform(auth(get("/api/v1/reports/cash/daily")
                .param("date", "2026-09-21"), owner)).andReturn(), 200);
        assertThat(daily.toString()).contains("invoicePayments");
        JsonNode summary = daily.path("summary");
        assertMoney(summary, "outflows", cash);
        Snapshot after = snapshot();
        read(postAs(owner, payPath(target), body), 409);
        assertThat(snapshot()).isEqualTo(after);
    }

    @Test
    void shouldPayWithoutCreditsAndAllowNegativeAccountBalance() throws Exception {
        Invoice target = invoice(owner, owner.cardId(), 8, InvoiceStatus.CLOSED, "150");
        available(money("850"));
        Account account = account();
        account.setCurrentBalance(money("20"));
        accounts.saveAndFlush(account);
        JsonNode result = pay(owner, target, owner.accountId(), 200);
        assertMoney(result, "creditAppliedAmount", "0");
        assertMoney(result, "cashPaidAmount", "150");
        assertThat(account().getCurrentBalance()).isEqualByComparingTo("-130");
    }

    @Test
    void shouldSettleZeroInvoiceWithoutAccountOrPaymentTransaction() throws Exception {
        Invoice target = invoice(owner, owner.cardId(), 8, InvoiceStatus.CLOSED, "0");
        JsonNode result = pay(owner, target, null, 200);
        assertThat(result.path("paymentTransactionId").isNull()).isTrue();
        assertThat(paymentRows()).isEmpty();
        assertThat(card().getAvailableLimit()).isEqualByComparingTo("1000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "foreign", "inactive"})
    void shouldRollbackCreditUseWhenAccountIsInvalid(String scenario) throws Exception {
        refund(owner, paidPurchase(owner, "50"), 200);
        Invoice target = invoice(owner, owner.cardId(), 8, InvoiceStatus.CLOSED, "150");
        available(money("850"));
        UUID accountId = scenario.equals("missing") ? null : owner.accountId();
        if (scenario.equals("foreign")) accountId = other.accountId();
        if (scenario.equals("inactive")) {
            Account account = account();
            account.setStatus(AccountStatus.INACTIVE);
            accounts.saveAndFlush(account);
        }
        Snapshot before = snapshot();
        pay(owner, target, accountId, scenario.equals("foreign") ? 404 : 409);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldRollbackConsumedCreditAndAccountAfterPaymentTransactionFlushFails() throws Exception {
        refund(owner, paidPurchase(owner, "50"), 200);
        Invoice target = invoice(owner, owner.cardId(), 8, InvoiceStatus.CLOSED, "150");
        available(money("850"));
        Snapshot before = snapshot();
        doAnswer(call -> {
            Object saved = call.callRealMethod();
            if (((Transaction) saved).getType() == TransactionType.CREDIT_CARD_PAYMENT) {
                throw new IllegalStateException("Simulated failure after payment flush");
            }
            return saved;
        }).when(transactions).saveAndFlush(any(Transaction.class));
        pay(owner, target, owner.accountId(), 500);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldSettleInvoiceOnlyOnceUnderConcurrency() throws Exception {
        Invoice target = invoice(owner, owner.cardId(), 8, InvoiceStatus.CLOSED, "150");
        available(money("850"));
        synchronizeInvoiceReads(target.getId());
        Map<String, Object> body = Map.of("sourceAccountId", owner.accountId(), "expectedVersion", target.getVersion());
        assertThat(concurrent(() -> postAs(owner, payPath(target), body)))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(account().getCurrentBalance()).isEqualByComparingTo("850");
        assertThat(card().getAvailableLimit()).isEqualByComparingTo("1000");
        assertThat(paymentRows()).hasSize(1);
    }

    @Test
    void shouldFilterCreditsByOwnerCardPositiveRemainderAndEarlierReference() throws Exception {
        CreditCardCredit owned = credit(refund(owner, paidPurchase(owner, "100"), 200));
        refund(other, paidPurchase(other, "100"), 200);
        CreditCard anotherCard = newCard(owner.id(), owner.accountId());
        Actor sameUserOtherCard = new Actor(owner.id(), owner.accountId(), anotherCard.getId(), owner.token());
        refund(sameUserOtherCard, paidPurchase(sameUserOtherCard, "100"), 200);
        PageRequest page = PageRequest.of(0, 100);

        assertThat(credits.findEligibleCredits(owner.id(), owner.cardId(), 2026, 7, page))
                .extracting(CreditCardCredit::getId).containsExactly(owned.getId());
        assertThat(credits.findEligibleCredits(owner.id(), owner.cardId(), 2026, 6, page)).isEmpty();
        assertThat(credits.findEligibleCredits(owner.id(), owner.cardId(), 2025, 12, page)).isEmpty();
        assertThat(credits.findEligibleCredits(owner.id(), owner.cardId(), 2027, 1, page))
                .extracting(CreditCardCredit::getId).containsExactly(owned.getId());
        assertThat(credits.findEligibleCredits(other.id(), owner.cardId(), 2027, 1, page)).isEmpty();
        owned.setRemainingAmount(BigDecimal.ZERO);
        credits.saveAndFlush(owned);
        assertThat(credits.findEligibleCredits(owner.id(), owner.cardId(), 2027, 1, page)).isEmpty();
    }

    @Test
    void shouldNotSpendSameCreditOnTwoConcurrentInvoices() throws Exception {
        CreditCardCredit credit = credit(refund(owner, paidPurchase(owner, "100"), 200));
        Invoice firstInvoice = invoice(owner, owner.cardId(), 7, InvoiceStatus.CLOSED, "100");
        Invoice secondInvoice = invoice(owner, owner.cardId(), 8, InvoiceStatus.CLOSED, "100");
        available(money("800"));
        CyclicBarrier barrier = new CyclicBarrier(2);
        doAnswer(call -> {
            Object loaded = call.callRealMethod();
            barrier.await(15, TimeUnit.SECONDS);
            return loaded;
        }).when(credits).findEligibleCredits(eq(owner.id()), eq(owner.cardId()), eq(2026),
                anyInt(), any(Pageable.class));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> first = pool.submit(() -> postAs(owner, payPath(firstInvoice),
                    Map.of("expectedVersion", firstInvoice.getVersion())));
            Future<MvcResult> second = pool.submit(() -> postAs(owner, payPath(secondInvoice),
                    Map.of("expectedVersion", secondInvoice.getVersion())));
            assertThat(List.of(first.get(40, TimeUnit.SECONDS).getResponse().getStatus(),
                    second.get(40, TimeUnit.SECONDS).getResponse().getStatus()))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(List.of(invoices.findById(firstInvoice.getId()).orElseThrow().getStatus(),
                invoices.findById(secondInvoice.getId()).orElseThrow().getStatus()))
                .containsExactlyInAnyOrder(InvoiceStatus.PAID, InvoiceStatus.CLOSED);
        assertThat(credits.findById(credit.getId()).orElseThrow().getRemainingAmount()).isZero();
        assertThat(snapshot().applications()).hasSize(1);
        assertThat(account().getCurrentBalance()).isEqualByComparingTo("1000");
        assertThat(card().getAvailableLimit()).isEqualByComparingTo("900");
        assertThat(paymentRows()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = InvoiceStatus.class, names = {"OPEN", "PAID", "CANCELLED"})
    void shouldRejectPaymentForNonClosedInvoice(InvoiceStatus status) throws Exception {
        Invoice target = invoice(owner, owner.cardId(), 8, status, "100");
        Snapshot before = snapshot();
        pay(owner, target, owner.accountId(), 409);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldRejectStaleVersionAndForeignInvoice() throws Exception {
        Invoice target = invoice(owner, owner.cardId(), 8, InvoiceStatus.CLOSED, "100");
        available(money("900"));
        Snapshot before = snapshot();
        JsonNode error = read(postAs(owner, payPath(target), Map.of("expectedVersion", 999)), 409);
        assertThat(error.path("code").asString()).isEqualTo("OPTIMISTIC_LOCK_CONFLICT");
        pay(other, target, other.accountId(), 404);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldCloseAfterClosingDayAndUseReturnedVersionForPayment() throws Exception {
        Invoice target = invoice(owner, owner.cardId(), 9, InvoiceStatus.OPEN, "100");
        available(money("900"));
        JsonNode closed = read(postAs(owner, closePath(target), Map.of("expectedVersion", target.getVersion())), 200);
        assertThat(closed.path("status").asString()).isEqualTo("CLOSED");
        assertThat(closed.path("version").asLong()).isEqualTo(target.getVersion() + 1);
        assertThat(card().getAvailableLimit()).isEqualByComparingTo("900");
        assertThat(account().getCurrentBalance()).isEqualByComparingTo("1000");
        Invoice refreshed = invoices.findById(target.getId()).orElseThrow();
        read(postAs(owner, closePath(target), Map.of("expectedVersion", refreshed.getVersion())), 200);
        assertThat(invoices.findById(target.getId()).orElseThrow().getVersion()).isEqualTo(refreshed.getVersion());
        pay(owner, refreshed, owner.accountId(), 200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-09-20T23:59:59-03:00", "2026-09-19T12:00:00-03:00"})
    void shouldNotCloseBeforeEndOfClosingDayInConfiguredZone(String instant) throws Exception {
        when(clock.instant()).thenReturn(OffsetDateTime.parse(instant).toInstant());
        Invoice target = invoice(owner, owner.cardId(), 9, InvoiceStatus.OPEN, "100");
        Snapshot before = snapshot();
        read(postAs(owner, closePath(target), Map.of("expectedVersion", target.getVersion())), 409);
        assertThat(snapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @EnumSource(value = InvoiceStatus.class, names = {"PAID", "CANCELLED"})
    void shouldNotClosePaidOrCancelledInvoice(InvoiceStatus status) throws Exception {
        Invoice target = invoice(owner, owner.cardId(), 8, status, "100");
        Snapshot before = snapshot();
        read(postAs(owner, closePath(target), Map.of("expectedVersion", target.getVersion())), 409);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldRejectGenericMutationForPurchasesAndInvoicePayments() throws Exception {
        Transaction purchase = purchase(owner, "900", "100").getFirst();
        Invoice target = invoiceOf(purchase);
        target.setStatus(InvoiceStatus.CLOSED);
        invoices.saveAndFlush(target);
        JsonNode settled = pay(owner, target, owner.accountId(), 200);
        UUID paymentId = UUID.fromString(settled.path("paymentTransactionId").asString());
        Snapshot before = snapshot();
        for (UUID id : List.of(purchase.getId(), paymentId)) {
            read(postAs(owner, "/api/v1/transactions/" + id + "/cancel", Map.of()), 400);
            read(mvc.perform(auth(patch("/api/v1/transactions/{id}", id), owner)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 999}")).andReturn(), 400);
        }
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldRequireJwtAndValidateBodies() throws Exception {
        Transaction transaction = purchase(owner, "900", "100").getFirst();
        Invoice target = invoiceOf(transaction);
        for (String path : List.of(refundPath(owner.cardId(), transaction.getId()), closePath(target), payPath(target))) {
            read(mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(), 401);
            read(postAs(owner, path, Map.of()), 400);
        }
        read(postAs(owner, refundPath(owner.cardId(), transaction.getId()), Map.of("reason", " ")), 400);
        read(postAs(owner, refundPath(owner.cardId(), transaction.getId()), Map.of("reason", "a".repeat(501))), 400);
        read(postAs(owner, closePath(target), Map.of("expectedVersion", -1)), 400);
    }

    private Actor actor() {
        User user = new User();
        user.setName("Refund settlement fixture");
        user.setEmail("refund-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        user = users.saveAndFlush(user);
        fixtureUsers.add(user.getId());
        Account account = new Account();
        account.setUserId(user.getId());
        account.setName("Fixture account");
        account.setType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setInitialBalance(money("1000"));
        account.setCurrentBalance(money("1000"));
        account = accounts.saveAndFlush(account);
        CreditCard card = newCard(user.getId(), account.getId());
        String token = jwt.generateToken(userDetails.loadUserByUsername(user.getEmail()));
        return new Actor(user.getId(), account.getId(), card.getId(), token);
    }

    private CreditCard newCard(UUID userId, UUID accountId) {
        CreditCard card = new CreditCard();
        card.setUserId(userId);
        card.setName("Fixture card");
        card.setCreditLimit(money("1000"));
        card.setAvailableLimit(money("1000"));
        card.setClosingDay(20);
        card.setDueDay(28);
        card.setDefaultAccountId(accountId);
        card.setStatus(CreditCardStatus.ACTIVE);
        return cards.saveAndFlush(card);
    }

    private Invoice invoice(Actor actor, UUID cardId, int month, InvoiceStatus status, String amount) {
        Invoice invoice = new Invoice();
        invoice.setCreditCardId(cardId);
        invoice.setReferenceMonth(month);
        invoice.setReferenceYear(2026);
        invoice.setClosingDate(LocalDate.of(2026, month, 20));
        invoice.setDueDate(LocalDate.of(2026, month, 28));
        invoice.setStatus(status);
        invoice.setPaidAt(status == InvoiceStatus.PAID ? ORIGINAL_PAYMENT : null);
        invoice.setTotalAmount(money(amount));
        return invoices.saveAndFlush(invoice);
    }

    private List<Transaction> purchase(Actor actor, String available, String... amounts) {
        CreditCard card = cards.findById(actor.cardId()).orElseThrow();
        card.setAvailableLimit(money(available));
        cards.saveAndFlush(card);
        UUID group = UUID.randomUUID();
        List<Transaction> result = new ArrayList<>();
        for (int index = 0; index < amounts.length; index++) {
            Invoice invoice = invoice(actor, actor.cardId(), 6 + index, InvoiceStatus.OPEN, amounts[index]);
            Transaction transaction = new Transaction();
            transaction.setUserId(actor.id());
            transaction.setDescription("Fixture installment " + (index + 1));
            transaction.setAmount(money(amounts[index]));
            transaction.setCompetenceDate(invoice.getClosingDate().minusDays(1));
            transaction.setDueDate(invoice.getDueDate());
            transaction.setType(TransactionType.CREDIT_CARD_PURCHASE);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            transaction.setCreditCardId(actor.cardId());
            transaction.setInvoiceId(invoice.getId());
            transaction.setInstallmentGroupId(group);
            transaction.setInstallmentNumber(index + 1);
            transaction.setInstallmentCount(amounts.length);
            result.add(transactions.saveAndFlush(transaction));
        }
        return result;
    }

    private Transaction paidPurchase(Actor actor, String amount) {
        Transaction transaction = purchase(actor, "1000", amount).getFirst();
        Invoice invoice = invoiceOf(transaction);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(ORIGINAL_PAYMENT);
        invoices.saveAndFlush(invoice);
        return transaction;
    }

    private void synchronizeInvoiceReads(UUID invoiceId) {
        CyclicBarrier barrier = new CyclicBarrier(2);
        doAnswer(call -> {
            Object loaded = call.callRealMethod();
            barrier.await(15, TimeUnit.SECONDS);
            return loaded;
        }).when(invoices).findOwnedById(invoiceId, owner.id());
    }

    private List<Integer> concurrent(Callable<MvcResult> request) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> first = pool.submit(request);
            Future<MvcResult> second = pool.submit(request);
            return List.of(first.get(40, TimeUnit.SECONDS).getResponse().getStatus(),
                    second.get(40, TimeUnit.SECONDS).getResponse().getStatus());
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private JsonNode refund(Actor actor, Transaction transaction, int status) throws Exception {
        return read(postAs(actor, refundPath(transaction.getCreditCardId(), transaction.getId()),
                Map.of("reason", REASON)), status);
    }

    private JsonNode pay(Actor actor, Invoice invoice, UUID accountId, int status) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("expectedVersion", invoice.getVersion());
        if (accountId != null) body.put("sourceAccountId", accountId);
        return read(postAs(actor, payPath(invoice), body), status);
    }

    private MvcResult postAs(Actor actor, String path, Object body) throws Exception {
        return mvc.perform(auth(post(path), actor).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andReturn();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, Actor actor) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + actor.token());
    }

    private JsonNode read(MvcResult result, int expectedStatus) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(result.getResponse().getStatus()).as(body).isEqualTo(expectedStatus);
        return json.readTree(body);
    }

    private CreditCardCredit credit(JsonNode refund) {
        return credits.findById(UUID.fromString(refund.path("items").get(0).path("creditId").asString()))
                .orElseThrow();
    }

    private static String refundPath(UUID cardId, UUID transactionId) {
        return "/api/v1/credit-cards/" + cardId + "/purchase/" + transactionId + "/refund";
    }
    private static String payPath(Invoice invoice) { return "/api/v1/invoices/" + invoice.getId() + "/pay"; }
    private static String closePath(Invoice invoice) { return "/api/v1/invoices/" + invoice.getId() + "/close"; }
    private static BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
    private void assertMoney(JsonNode node, String field, String expected) {
        assertThat(node.has(field)).as("Missing monetary field %s in %s", field, node).isTrue();
        assertThat(new BigDecimal(node.path(field).asString())).isEqualByComparingTo(expected);
    }
    private Invoice invoiceOf(Transaction transaction) { return invoices.findById(transaction.getInvoiceId()).orElseThrow(); }
    private Transaction reload(Transaction transaction) { return transactions.findById(transaction.getId()).orElseThrow(); }
    private Account account() { return accounts.findById(owner.accountId()).orElseThrow(); }
    private CreditCard card() { return cards.findById(owner.cardId()).orElseThrow(); }
    private void available(BigDecimal value) {
        CreditCard card = card();
        card.setAvailableLimit(value);
        cards.saveAndFlush(card);
    }
    private Map<String, Object> invoiceRow(UUID id) { return jdbc.queryForMap("SELECT * FROM invoices WHERE id = ?", id); }
    private Map<String, Object> transactionRow(UUID id) { return jdbc.queryForMap("SELECT * FROM transactions WHERE id = ?", id); }
    private List<Map<String, Object>> paymentRows() {
        return jdbc.queryForList("SELECT * FROM transactions WHERE user_id = ? AND type = 'CREDIT_CARD_PAYMENT'", owner.id());
    }
    private Snapshot snapshot() {
        return new Snapshot(
                jdbc.queryForList("SELECT * FROM accounts ORDER BY id"),
                jdbc.queryForList("SELECT * FROM credit_cards ORDER BY id"),
                jdbc.queryForList("SELECT * FROM invoices ORDER BY id"),
                jdbc.queryForList("SELECT * FROM transactions ORDER BY id"),
                jdbc.queryForList("SELECT * FROM credit_card_refunds ORDER BY id"),
                jdbc.queryForList("SELECT * FROM credit_card_refund_items ORDER BY id"),
                jdbc.queryForList("SELECT * FROM credit_card_credits ORDER BY id"),
                jdbc.queryForList("SELECT * FROM credit_card_credit_applications ORDER BY id"));
    }
    private record Actor(UUID id, UUID accountId, UUID cardId, String token) {}
    private record Snapshot(List<Map<String, Object>> accounts, List<Map<String, Object>> cards,
                            List<Map<String, Object>> invoices, List<Map<String, Object>> transactions,
                            List<Map<String, Object>> refunds, List<Map<String, Object>> items,
                            List<Map<String, Object>> credits, List<Map<String, Object>> applications) {}
}
