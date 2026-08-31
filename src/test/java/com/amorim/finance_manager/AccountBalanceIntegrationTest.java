package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
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
public class AccountBalanceIntegrationTest {

    @Autowired
    private AccountBalanceService accountBalanceService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    transactions,
                    categories,
                    accounts,
                    users
                CASCADE
                """
        );

        User user = createUser();
        userId = user.getId();

        Account account = createAccount(
                userId,
                "Main Account",
                new BigDecimal("100.00")
        );

        accountId = account.getId();
    }

    @Test
    void shouldDetectConcurrentAccountUpdates() throws Exception {
        Account initialAccount = accountRepository
                .findById(accountId)
                .orElseThrow();

        long initialVersion = initialAccount.getVersion();

        CountDownLatch bothTransactionsLoaded =
                new CountDownLatch(2);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<Throwable> firstUpdate = executor.submit(() ->
                    runConcurrentUpdate(
                            bothTransactionsLoaded,
                            new BigDecimal("10.00")
                    )
            );

            Future<Throwable> secondUpdate = executor.submit(() ->
                    runConcurrentUpdate(
                            bothTransactionsLoaded,
                            new BigDecimal("10.00")
                    )
            );

            Throwable firstFailure =
                    firstUpdate.get(15, TimeUnit.SECONDS);

            Throwable secondFailure =
                    secondUpdate.get(15, TimeUnit.SECONDS);

            List<Throwable> failures = Stream.of(
                            firstFailure,
                            secondFailure
                    )
                    .filter(Objects::nonNull)
                    .toList();

            assertThat(failures).hasSize(1);

            assertThat(failures.get(0))
                    .isInstanceOf(
                            OptimisticLockingFailureException.class
                    );

            Account persistedAccount = accountRepository
                    .findById(accountId)
                    .orElseThrow();

            assertThat(persistedAccount.getCurrentBalance())
                    .isEqualByComparingTo("110.00");

            assertThat(persistedAccount.getVersion())
                    .isEqualTo(initialVersion + 1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRollbackAllBalanceUpdatesWhenOperationFails() {
        Account destinationAccount = createAccount(
                userId,
                "Destination Account",
                new BigDecimal("50.00")
        );

        UUID destinationAccountId =
                destinationAccount.getId();

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        assertThatThrownBy(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    accountBalanceService.debit(
                            userId,
                            accountId,
                            new BigDecimal("40.00")
                    );

                    accountBalanceService.credit(
                            userId,
                            destinationAccountId,
                            new BigDecimal("40.00")
                    );

                    throw new IllegalStateException(
                            "Falha simulada"
                    );
                })
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha simulada");

        Account sourceAfterRollback = accountRepository
                .findById(accountId)
                .orElseThrow();

        Account destinationAfterRollback = accountRepository
                .findById(destinationAccountId)
                .orElseThrow();

        assertThat(sourceAfterRollback.getCurrentBalance())
                .isEqualByComparingTo("100.00");

        assertThat(destinationAfterRollback.getCurrentBalance())
                .isEqualByComparingTo("50.00");
    }

    private Throwable runConcurrentUpdate(
            CountDownLatch bothTransactionsLoaded,
            BigDecimal amount
    ) {
        try {
            TransactionTemplate transactionTemplate =
                    new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(status -> {
                Account account = accountRepository
                        .findByIdAndUserId(accountId, userId)
                        .orElseThrow();

                bothTransactionsLoaded.countDown();
                await(bothTransactionsLoaded);

                account.setCurrentBalance(
                        account.getCurrentBalance().add(amount)
                );

                accountRepository.saveAndFlush(account);
            });

            return null;
        } catch (Throwable exception) {
            return exception;
        }
    }

    private void await(CountDownLatch latch) {
        try {
            boolean completed =
                    latch.await(10, TimeUnit.SECONDS);

            if (!completed) {
                throw new IllegalStateException(
                        "Timeout aguardando as transações concorrentes"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Teste de concorrência interrompido",
                    exception
            );
        }
    }

    private User createUser() {
        User user = new User();
        user.setName("Balance Test User");
        user.setEmail(
                "balance-" + UUID.randomUUID() + "@example.com"
        );
        user.setPasswordHash(
                "integration-test-password-hash"
        );

        return userRepository.saveAndFlush(user);
    }

    private Account createAccount(
            UUID ownerId,
            String name,
            BigDecimal balance
    ) {
        Account account = new Account();
        account.setUserId(ownerId);
        account.setName(name);
        account.setType(AccountType.CHECKING);
        account.setInstitution("Test Bank");
        account.setInitialBalance(balance);
        account.setCurrentBalance(balance);
        account.setStatus(AccountStatus.ACTIVE);

        return accountRepository.saveAndFlush(account);
    }
}
