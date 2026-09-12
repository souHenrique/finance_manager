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
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class InvoiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CreditCardRepository creditCardRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private CreditCard creditCard;

    @BeforeEach
    void setUp() {
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

        User user = createUser();
        Account account = createAccount(user.getId());
        creditCard = createCreditCard(user.getId(), account.getId());
    }

    @Test
    void shouldPersistEveryInvoiceFieldAndInitializeVersionAtZero() {
        Invoice invoice = invoice(
                creditCard.getId(),
                9,
                2026,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 17),
                "0.00",
                InvoiceStatus.OPEN
        );

        Invoice saved = invoiceRepository.saveAndFlush(invoice);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreditCardId()).isEqualTo(creditCard.getId());
        assertThat(saved.getReferenceMonth()).isEqualTo(9);
        assertThat(saved.getReferenceYear()).isEqualTo(2026);
        assertThat(saved.getClosingDate()).isEqualTo("2026-09-10");
        assertThat(saved.getDueDate()).isEqualTo("2026-09-17");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("0.00");
        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(saved.getPaidAt()).isNull();
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void shouldEnforceOneInvoicePerCardReferenceMonthAndYear() {
        insertInvoice(
                UUID.randomUUID(),
                creditCard.getId(),
                9,
                2026,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 17),
                "0.00",
                "OPEN"
        );

        assertThatThrownBy(() -> insertInvoice(
                UUID.randomUUID(),
                creditCard.getId(),
                9,
                2026,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 17),
                "0.00",
                "OPEN"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM invoices
                WHERE credit_card_id = ?
                  AND reference_month = 9
                  AND reference_year = 2026
                """,
                Long.class,
                creditCard.getId()
        )).isEqualTo(1L);
    }

    @Test
    void shouldAllowTheSameReferenceForDifferentCreditCards() {
        Account account = accountRepository.findAll().getFirst();
        CreditCard otherCard = createCreditCard(
                creditCard.getUserId(),
                account.getId()
        );

        insertInvoice(
                UUID.randomUUID(), creditCard.getId(), 9, 2026,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 17),
                "0.00", "OPEN"
        );
        insertInvoice(
                UUID.randomUUID(), otherCard.getId(), 9, 2026,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 17),
                "0.00", "OPEN"
        );

        assertThat(invoiceRepository.count()).isEqualTo(2L);
    }

    @Test
    void shouldEnforceTheCreditCardForeignKey() {
        assertThatThrownBy(() -> insertInvoice(
                UUID.randomUUID(),
                UUID.randomUUID(),
                9,
                2026,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 17),
                "0.00",
                "OPEN"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(invoiceRepository.count()).isZero();
    }

    @Test
    void shouldEnforceReferenceAmountStatusAndDateConstraints() {
        List<InvalidInvoice> invalidInvoices = List.of(
                new InvalidInvoice(0, 2026, "0.00", "OPEN",
                        LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 17)),
                new InvalidInvoice(13, 2026, "0.00", "OPEN",
                        LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 17)),
                new InvalidInvoice(9, 0, "0.00", "OPEN",
                        LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 17)),
                new InvalidInvoice(9, 2026, "-0.01", "OPEN",
                        LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 17)),
                new InvalidInvoice(9, 2026, "0.00", "UNKNOWN",
                        LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 17)),
                new InvalidInvoice(9, 2026, "0.00", "OPEN",
                        LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10))
        );

        for (InvalidInvoice invalid : invalidInvoices) {
            assertThatThrownBy(() -> insertInvoice(
                    UUID.randomUUID(),
                    creditCard.getId(),
                    invalid.referenceMonth(),
                    invalid.referenceYear(),
                    invalid.closingDate(),
                    invalid.dueDate(),
                    invalid.totalAmount(),
                    invalid.status()
            )).isInstanceOf(DataIntegrityViolationException.class);
        }

        assertThat(invoiceRepository.count()).isZero();
    }

    @Test
    void shouldAllowOnlyOneOfTwoConcurrentInvoiceUpdatesToCommit() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        insertInvoice(
                invoiceId,
                creditCard.getId(),
                9,
                2026,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 17),
                "0.00",
                "OPEN"
        );
        CountDownLatch bothLoaded = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Throwable> first = executor.submit(
                    () -> runConcurrentUpdate(invoiceId, "100.00", bothLoaded)
            );
            Future<Throwable> second = executor.submit(
                    () -> runConcurrentUpdate(invoiceId, "200.00", bothLoaded)
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

            Invoice persisted = invoiceRepository.findById(invoiceId).orElseThrow();
            assertThat(persisted.getTotalAmount())
                    .isIn(new BigDecimal("100.00"), new BigDecimal("200.00"));
            assertThat(persisted.getVersion()).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    private Throwable runConcurrentUpdate(
            UUID invoiceId,
            String totalAmount,
            CountDownLatch bothLoaded
    ) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
                bothLoaded.countDown();
                await(bothLoaded);
                invoice.setTotalAmount(new BigDecimal(totalAmount));
                invoiceRepository.saveAndFlush(invoice);
            });
            return null;
        } catch (Throwable exception) {
            return exception;
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timeout waiting for concurrent invoice transactions"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Concurrent invoice test interrupted",
                    exception
            );
        }
    }

    private User createUser() {
        User user = new User();
        user.setName("Invoice User");
        user.setEmail("invoice-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        return userRepository.saveAndFlush(user);
    }

    private Account createAccount(UUID userId) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName("Invoice Account");
        account.setType(AccountType.CHECKING);
        account.setInstitution("Invoice Bank");
        account.setInitialBalance(new BigDecimal("1000.00"));
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        return accountRepository.saveAndFlush(account);
    }

    private CreditCard createCreditCard(UUID userId, UUID accountId) {
        CreditCard card = new CreditCard();
        card.setUserId(userId);
        card.setName("Invoice Credit Card " + UUID.randomUUID());
        card.setCreditLimit(new BigDecimal("5000.00"));
        card.setAvailableLimit(new BigDecimal("5000.00"));
        card.setClosingDay(10);
        card.setDueDay(17);
        card.setDefaultAccountId(accountId);
        card.setStatus(CreditCardStatus.ACTIVE);
        return creditCardRepository.saveAndFlush(card);
    }

    private Invoice invoice(
            UUID creditCardId,
            int referenceMonth,
            int referenceYear,
            LocalDate closingDate,
            LocalDate dueDate,
            String totalAmount,
            InvoiceStatus status
    ) {
        Invoice invoice = new Invoice();
        invoice.setCreditCardId(creditCardId);
        invoice.setReferenceMonth(referenceMonth);
        invoice.setReferenceYear(referenceYear);
        invoice.setClosingDate(closingDate);
        invoice.setDueDate(dueDate);
        invoice.setTotalAmount(new BigDecimal(totalAmount));
        invoice.setStatus(status);
        return invoice;
    }

    private void insertInvoice(
            UUID id,
            UUID creditCardId,
            int referenceMonth,
            int referenceYear,
            LocalDate closingDate,
            LocalDate dueDate,
            String totalAmount,
            String status
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO invoices (
                    id,
                    credit_card_id,
                    reference_month,
                    reference_year,
                    closing_date,
                    due_date,
                    total_amount,
                    status,
                    version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                id,
                creditCardId,
                referenceMonth,
                referenceYear,
                closingDate,
                dueDate,
                new BigDecimal(totalAmount),
                status
        );
    }

    private record InvalidInvoice(
            int referenceMonth,
            int referenceYear,
            String totalAmount,
            String status,
            LocalDate closingDate,
            LocalDate dueDate
    ) {
    }
}
