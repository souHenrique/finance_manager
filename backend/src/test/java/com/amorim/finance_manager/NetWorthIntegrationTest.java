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
import com.amorim.finance_manager.networth.service.NetWorthService;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@Transactional
public class NetWorthIntegrationTest {

    @Autowired
    private NetWorthService netWorthService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = createUser("Net Worth Owner");

        when(currentUserService.getCurrentUserId())
                .thenReturn(owner.getId());
    }

    @Test
    void shouldCalculateUsingOnlyAccounts() {
        createAccount(owner.getId(), "1500.00");

        assertNetWorth("1500.00");
    }

    @Test
    void shouldPreserveNegativeAccountBalance() {
        createAccount(owner.getId(), "-250.00");

        assertNetWorth("-250.00");
    }

    @Test
    void shouldSubtractOpenInvoice() {
        Account account = createAccount(owner.getId(), "1000.00");
        CreditCard card = createCard(owner.getId(), account.getId());

        createInvoice(card.getId(), "250.00", InvoiceStatus.OPEN);

        assertNetWorth("750.00");
    }

    @Test
    void shouldSubtractClosedInvoice() {
        Account account = createAccount(owner.getId(), "1000.00");
        CreditCard card = createCard(owner.getId(), account.getId());

        createInvoice(card.getId(), "400.00", InvoiceStatus.CLOSED);

        assertNetWorth("600.00");
    }

    @Test
    void shouldNotSubtractPaidInvoice() {
        Account account = createAccount(owner.getId(), "750.00");
        CreditCard card = createCard(owner.getId(), account.getId());

        createInvoice(card.getId(), "250.00", InvoiceStatus.PAID);

        assertNetWorth("750.00");
    }

    @Test
    void shouldSubtractInvoicesFromMultipleCards() {
        Account account = createAccount(owner.getId(), "3000.00");

        CreditCard firstCard =
                createCard(owner.getId(), account.getId());

        CreditCard secondCard =
                createCard(owner.getId(), account.getId());

        createInvoice(firstCard.getId(), "600.00", InvoiceStatus.OPEN);
        createInvoice(secondCard.getId(), "400.00", InvoiceStatus.CLOSED);

        assertNetWorth("2000.00");
    }

    @Test
    void shouldSumMultipleAccounts() {
        createAccount(owner.getId(), "1000.00");
        createAccount(owner.getId(), "500.00");
        createAccount(owner.getId(), "-200.00");

        assertNetWorth("1300.00");
    }

    @Test
    void shouldIgnoreAccountsAndInvoicesFromAnotherUser() {
        createAccount(owner.getId(), "1000.00");

        User otherUser = createUser("Other User");
        Account otherAccount =
                createAccount(otherUser.getId(), "9999.00");

        CreditCard otherCard =
                createCard(otherUser.getId(), otherAccount.getId());

        createInvoice(
                otherCard.getId(),
                "5000.00",
                InvoiceStatus.OPEN
        );

        assertNetWorth("1000.00");
    }

    private User createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("net-worth-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");

        return userRepository.saveAndFlush(user);
    }

    private Account createAccount(UUID userId, String balance) {
        BigDecimal amount = new BigDecimal(balance);

        Account account = new Account();
        account.setUserId(userId);
        account.setName("Account " + UUID.randomUUID());
        account.setType(AccountType.CHECKING);
        account.setInstitution("Test Bank");
        account.setInitialBalance(amount);
        account.setCurrentBalance(amount);
        account.setStatus(AccountStatus.ACTIVE);

        return accountRepository.saveAndFlush(account);
    }

    private CreditCard createCard(UUID userId, UUID accountId) {
        CreditCard card = new CreditCard();
        card.setUserId(userId);
        card.setName("Card " + UUID.randomUUID());
        card.setCreditLimit(new BigDecimal("5000.00"));
        card.setAvailableLimit(new BigDecimal("5000.00"));
        card.setClosingDay(10);
        card.setDueDay(17);
        card.setDefaultAccountId(accountId);
        card.setStatus(CreditCardStatus.ACTIVE);

        return creditCardRepository.saveAndFlush(card);
    }

    private Invoice createInvoice(
            UUID creditCardId,
            String totalAmount,
            InvoiceStatus status
    ) {
        Invoice invoice = new Invoice();
        invoice.setCreditCardId(creditCardId);
        invoice.setReferenceMonth(9);
        invoice.setReferenceYear(2026);
        invoice.setClosingDate(LocalDate.of(2026, 9, 10));
        invoice.setDueDate(LocalDate.of(2026, 9, 17));
        invoice.setTotalAmount(new BigDecimal(totalAmount));
        invoice.setStatus(status);

        if (status == InvoiceStatus.PAID) {
            invoice.setPaidAt(Instant.parse("2026-09-15T12:00:00Z"));
        }

        return invoiceRepository.saveAndFlush(invoice);
    }

    private void assertNetWorth(String expected) {
        assertThat(netWorthService.calculate())
                .isEqualByComparingTo(expected);
    }
}
