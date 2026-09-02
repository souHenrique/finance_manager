package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class AuditIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    transactions_aud,
                    accounts_aud,
                    audit_revision,
                    transactions,
                    categories,
                    accounts,
                    users
                CASCADE
                """
        );
    }

    @Test
    void shouldAuditAccountCreationAndUpdate() {
        User user = createUser();
        Account account = createAccount(user.getId());

        account.setName("Updated Account");
        accountRepository.saveAndFlush(account);

        Long revisionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts_aud WHERE id = ?",
                Long.class,
                account.getId()
        );

        assertThat(revisionCount).isEqualTo(2L);
        assertThat(accountNames(account.getId()))
                .containsExactly("Main Account", "Updated Account");
    }

    @Test
    void shouldAuditTransactionCreationAndCancellation() {
        User user = createUser();
        Account account = createAccount(user.getId());
        Category category = createCategory(user.getId());
        Transaction transaction = createTransaction(
                user.getId(),
                account.getId(),
                category.getId()
        );

        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.saveAndFlush(transaction);

        Long revisionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions_aud WHERE id = ?",
                Long.class,
                transaction.getId()
        );

        assertThat(revisionCount).isEqualTo(2L);
        assertThat(transactionStatuses(transaction.getId()))
                .containsExactly("COMPLETED", "CANCELLED");
    }

    @Test
    void shouldNotAuditRolledBackTransaction() {
        User user = createUser();
        Account account = createAccount(user.getId());
        Category category = createCategory(user.getId());
        AtomicReference<UUID> transactionId = new AtomicReference<>();
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            Transaction transaction = createTransactionEntity(
                    user.getId(),
                    account.getId(),
                    category.getId()
            );
            transactionRepository.saveAndFlush(transaction);
            transactionId.set(transaction.getId());
            throw new IllegalStateException("Force rollback");
        })).isInstanceOf(IllegalStateException.class);

        Long revisionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions_aud WHERE id = ?",
                Long.class,
                transactionId.get()
        );

        assertThat(revisionCount).isZero();
    }

    @Test
    void shouldKeepAuditSelective() {
        Long nonCriticalAuditTableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('users_aud', 'categories_aud')
                """,
                Long.class
        );

        assertThat(nonCriticalAuditTableCount).isZero();
    }

    private User createUser() {
        User user = new User();
        user.setName("Audit User");
        user.setEmail("audit-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        return userRepository.saveAndFlush(user);
    }

    private Account createAccount(UUID userId) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName("Main Account");
        account.setType(AccountType.CHECKING);
        account.setInstitution("Test Bank");
        account.setInitialBalance(new BigDecimal("100.00"));
        account.setCurrentBalance(new BigDecimal("100.00"));
        account.setStatus(AccountStatus.ACTIVE);
        return accountRepository.saveAndFlush(account);
    }

    private Category createCategory(UUID userId) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName("Audit Category");
        category.setType(CategoryType.EXPENSE);
        category.setStatus(CategoryStatus.ACTIVE);
        return categoryRepository.saveAndFlush(category);
    }

    private Transaction createTransaction(
            UUID userId,
            UUID accountId,
            UUID categoryId
    ) {
        return transactionRepository.saveAndFlush(
                createTransactionEntity(userId, accountId, categoryId)
        );
    }

    private Transaction createTransactionEntity(
            UUID userId,
            UUID accountId,
            UUID categoryId
    ) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setDescription("Audit transaction");
        transaction.setAmount(new BigDecimal("25.00"));
        transaction.setCompetenceDate(LocalDate.of(2026, 9, 1));
        transaction.setEffectiveDate(LocalDate.of(2026, 9, 1));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setPaymentMethod(PaymentMethod.DEBIT);
        transaction.setSourceAccountId(accountId);
        transaction.setCategoryId(categoryId);
        return transaction;
    }

    private List<String> accountNames(UUID accountId) {
        return jdbcTemplate.queryForList(
                "SELECT name FROM accounts_aud WHERE id = ? ORDER BY rev",
                String.class,
                accountId
        );
    }

    private List<String> transactionStatuses(UUID transactionId) {
        return jdbcTemplate.queryForList(
                "SELECT status FROM transactions_aud WHERE id = ? ORDER BY rev",
                String.class,
                transactionId
        );
    }
}
