package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@Transactional
class TransactionCsvExportIntegrationTest {

    private static final String ENDPOINT = "/api/v1/exports/transactions.csv";
    private static final String HEADER =
            "id,description,type,status,amount,competenceDate,effectiveDate,category,account";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
        userA = createUser(
                "Export User A",
                "Alimentação",
                "Conta Corrente São José"
        );
        userB = createUser(
                "Export User B",
                "Transporte",
                "Carteira B"
        );
    }

    @Test
    void shouldExportOnlyStableHeaderWhenThereAreNoTransactions() throws Exception {
        MockHttpServletResponse response = perform(userA)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        MediaType contentType = MediaType.parseMediaType(response.getContentType());

        assertThat(contentType.isCompatibleWith(MediaType.parseMediaType("text/csv")))
                .isTrue();
        assertThat(contentType.getCharset()).isEqualTo(StandardCharsets.UTF_8);
        assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"transactions.csv\"");
        assertThat(response.getContentAsByteArray())
                .isEqualTo((HEADER + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldExportOneTransactionWithTheExpectedColumns() throws Exception {
        Transaction transaction = save(
                userA,
                "Compra mensal",
                "75.90",
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 3)
        );

        assertThat(export(userA)).isEqualTo(
                HEADER + "\r\n"
                        + transaction.getId()
                        + ",Compra mensal,EXPENSE,COMPLETED,75.90,"
                        + "2026-09-02,2026-09-03,Alimentação,Conta Corrente São José\r\n"
        );
    }

    @Test
    void shouldExportSeveralTransactionsInDeterministicOrder() throws Exception {
        Transaction older = save(
                userA,
                "Compra antiga",
                "10.00",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 1)
        );
        Transaction newer = save(
                userA,
                "Compra recente",
                "20.00",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1)
        );

        String[] rows = export(userA).split("\\r\\n");

        assertThat(rows).hasSize(3);
        assertThat(rows[0]).isEqualTo(HEADER);
        assertThat(rows[1]).startsWith(newer.getId() + ",");
        assertThat(rows[2]).startsWith(older.getId() + ",");
    }

    @Test
    void shouldPreserveUtf8AccentsAndEscapeCsvSpecialCharacters() throws Exception {
        save(
                userA,
                "Pão, café e \"açúcar\"",
                "18.50",
                LocalDate.of(2026, 9, 4),
                LocalDate.of(2026, 9, 4)
        );

        String csv = export(userA);

        assertThat(csv)
                .contains("\"Pão, café e \"\"açúcar\"\"\"")
                .contains("Alimentação")
                .contains("Conta Corrente São José")
                .doesNotContain("�");
        assertThat(csv.getBytes(StandardCharsets.UTF_8))
                .containsSubsequence("açúcar".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldApplyTheSameCombinedFiltersAsTransactionSearch() throws Exception {
        Transaction expected = save(
                userA,
                "Mercado central",
                "150.00",
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 10, 1)
        );
        save(
                userA,
                "Mercado fora do período",
                "150.00",
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 1)
        );
        save(
                userA,
                "Farmácia",
                "300.00",
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 15)
        );

        String csv = export(
                userA,
                "startDate", "2026-09-01",
                "endDate", "2026-09-30",
                "categoryId", userA.categoryId().toString(),
                "accountId", userA.accountId().toString(),
                "type", "EXPENSE",
                "status", "COMPLETED",
                "minAmount", "100.00",
                "maxAmount", "200.00",
                "description", "MERCADO"
        );

        assertThat(csv).contains(expected.getId().toString());
        assertThat(csv).doesNotContain("Mercado fora do período", "Farmácia");
        assertThat(csv.split("\\r\\n")).hasSize(2);
    }

    @Test
    void shouldUseIsoDatesAndLeaveMissingEffectiveDateEmpty() throws Exception {
        Transaction pending = save(
                userA,
                "Pagamento pendente",
                "30.00",
                LocalDate.of(2026, 2, 3),
                null,
                transaction -> transaction.setStatus(TransactionStatus.PENDING)
        );

        String row = export(userA)
                .lines()
                .filter(line -> line.startsWith(pending.getId().toString()))
                .findFirst()
                .orElseThrow();
        String[] columns = row.split(",", -1);

        assertThat(columns[5]).isEqualTo("2026-02-03");
        assertThat(columns[6]).isEmpty();
    }

    @Test
    void shouldWriteBigDecimalWithoutCurrencyOrRegionalFormatting() throws Exception {
        Transaction transaction = save(
                userA,
                "Valor exato",
                "1234.56",
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 5)
        );

        String csv = export(userA);
        String row = csv.lines()
                .filter(line -> line.startsWith(transaction.getId().toString()))
                .findFirst()
                .orElseThrow();

        assertThat(row.split(",", -1)[4]).isEqualTo("1234.56");
        assertThat(csv).doesNotContain("R$", "1.234,56");
    }

    @Test
    void shouldNeverExportTransactionsFromAnotherUser() throws Exception {
        Transaction own = save(
                userA,
                "Transação própria",
                "10.00",
                LocalDate.of(2026, 9, 6),
                LocalDate.of(2026, 9, 6)
        );
        Transaction foreign = save(
                userB,
                "Transação de outro usuário",
                "999.00",
                LocalDate.of(2026, 9, 6),
                LocalDate.of(2026, 9, 6)
        );

        String csv = export(userA);

        assertThat(csv)
                .contains(own.getId().toString())
                .doesNotContain(
                        foreign.getId().toString(),
                        foreign.getDescription(),
                        "999.00"
                );
        assertThat(csv.split("\\r\\n")).hasSize(2);
    }

    private String export(TestUser user, String... parameters) throws Exception {
        byte[] bytes = perform(user, parameters)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private ResultActions perform(TestUser user, String... parameters) throws Exception {
        if (parameters.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Query parameters must be name/value pairs"
            );
        }

        MockHttpServletRequestBuilder request = get(ENDPOINT)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + user.token()
                );

        for (int index = 0; index < parameters.length; index += 2) {
            request.queryParam(parameters[index], parameters[index + 1]);
        }

        return mockMvc.perform(request);
    }

    private Transaction save(
            TestUser owner,
            String description,
            String amount,
            LocalDate competenceDate,
            LocalDate effectiveDate
    ) {
        return save(
                owner,
                description,
                amount,
                competenceDate,
                effectiveDate,
                transaction -> { }
        );
    }

    private Transaction save(
            TestUser owner,
            String description,
            String amount,
            LocalDate competenceDate,
            LocalDate effectiveDate,
            Consumer<Transaction> customize
    ) {
        Transaction transaction = new Transaction();
        transaction.setUserId(owner.id());
        transaction.setDescription(description);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCompetenceDate(competenceDate);
        transaction.setEffectiveDate(effectiveDate);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setPaymentMethod(PaymentMethod.PIX);
        transaction.setSourceAccountId(owner.accountId());
        transaction.setCategoryId(owner.categoryId());
        customize.accept(transaction);
        return transactionRepository.saveAndFlush(transaction);
    }

    private TestUser createUser(
            String name,
            String categoryName,
            String accountName
    ) {
        User user = new User();
        user.setName(name);
        user.setEmail("csv-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("integration-test-password-hash");

        User saved = userRepository.saveAndFlush(user);
        String token = jwtService.generateToken(
                userDetailsService.loadUserByUsername(saved.getEmail())
        );

        Account account = createAccount(saved.getId(), accountName);
        Category category = createCategory(saved.getId(), categoryName);

        return new TestUser(
                saved.getId(),
                token,
                account.getId(),
                category.getId()
        );
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

    private Category createCategory(UUID userId, String name) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setType(CategoryType.EXPENSE);
        category.setStatus(CategoryStatus.ACTIVE);
        return categoryRepository.saveAndFlush(category);
    }

    private record TestUser(
            UUID id,
            String token,
            UUID accountId,
            UUID categoryId
    ) {
    }
}
