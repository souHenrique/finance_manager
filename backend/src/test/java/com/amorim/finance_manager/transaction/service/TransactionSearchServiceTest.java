package com.amorim.finance_manager.transaction.service;

import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.shared.exception.InvalidTransactionException;
import com.amorim.finance_manager.shared.exception.UnauthenticatedUserException;
import com.amorim.finance_manager.transaction.dto.TransactionFilterRequest;
import com.amorim.finance_manager.transaction.dto.TransactionListItemResponse;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionSearchServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final TransactionFilterRequest NO_FILTERS = filters(null, null, null, null);

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountBalanceService accountBalanceService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private TransactionImpactService transactionImpactService;
    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test
    void shouldRejectReversedDateRangeBeforeQuerying() {
        TransactionFilterRequest filters = filters(
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1), null, null
        );

        assertThatThrownBy(() -> transactionService.list(filters, PageRequest.of(0, 20)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("A data inicial não pode ser posterior à data final");

        assertNoQueryOrFinancialChanges();
    }

    @Test
    void shouldRejectReversedAmountRangeBeforeQuerying() {
        TransactionFilterRequest filters = filters(
                null, null, new BigDecimal("100.01"), new BigDecimal("100.00")
        );

        assertThatThrownBy(() -> transactionService.list(filters, PageRequest.of(0, 20)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("O valor mínimo não pode ser maior que o valor máximo");

        assertNoQueryOrFinancialChanges();
    }

    @Test
    void shouldAcceptEqualDateAndAmountBounds() {
        stubEmptyPage();
        LocalDate date = LocalDate.of(2026, 9, 1);
        TransactionFilterRequest filters = filters(
                date, date, new BigDecimal("100.0"), new BigDecimal("100.00")
        );

        assertThat(transactionService.list(filters, PageRequest.of(0, 20)).isEmpty()).isTrue();
        assertThat(capturePageable().getPageSize()).isEqualTo(20);
    }

    @Test
    void shouldRejectUnpagedQueries() {
        assertThatThrownBy(() -> transactionService.list(NO_FILTERS, Pageable.unpaged()))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("A consulta de transações deve ser paginada");

        assertNoQueryOrFinancialChanges();
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "userId", "sourceAccountId", "account.name", "amount;drop table transactions"})
    void shouldRejectUnsupportedSortProperties(String property) {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(property));

        assertThatThrownBy(() -> transactionService.list(NO_FILTERS, pageable))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Campo de ordenação inválido: " + property);

        assertNoQueryOrFinancialChanges();
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "competenceDate", "status", "id"})
    void shouldRejectIgnoreCaseForNonDescriptionProperties(String property) {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.asc(property).ignoreCase()));

        assertThatThrownBy(() -> transactionService.list(NO_FILTERS, pageable))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Ordenação ignorecase é permitida somente para description");

        assertNoQueryOrFinancialChanges();
    }

    @Test
    void shouldPreserveIgnoreCaseForDescription() {
        stubEmptyPage();
        Sort.Order order = Sort.Order.asc("description").ignoreCase();

        transactionService.list(NO_FILTERS, PageRequest.of(0, 20, Sort.by(order)));

        assertThat(capturePageable().getSort().toList())
                .containsExactly(order, Sort.Order.asc("id"));
    }

    @Test
    void shouldUseDefaultSortWhenNoSortIsProvided() {
        stubEmptyPage();

        transactionService.list(NO_FILTERS, PageRequest.of(0, 20));

        assertThat(capturePageable().getSort().toList())
                .containsExactly(Sort.Order.desc("competenceDate"), Sort.Order.asc("id"));
    }

    @Test
    void shouldLimitPageSizeAndPreservePageNumberAndRequestedSort() {
        stubEmptyPage();
        Sort sort = Sort.by(Sort.Order.desc("amount"), Sort.Order.asc("competenceDate"));

        transactionService.list(NO_FILTERS, PageRequest.of(3, 500, sort));

        Pageable actual = capturePageable();
        assertThat(actual.getPageNumber()).isEqualTo(3);
        assertThat(actual.getPageSize()).isEqualTo(100);
        assertThat(actual.getSort().toList()).containsExactly(
                Sort.Order.desc("amount"), Sort.Order.asc("competenceDate"), Sort.Order.asc("id")
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 20, 100})
    void shouldPreservePageSizesWithinLimit(int size) {
        stubEmptyPage();

        transactionService.list(NO_FILTERS, PageRequest.of(0, size));

        assertThat(capturePageable().getPageSize()).isEqualTo(size);
    }

    @Test
    void shouldNotDuplicateOrOverrideExplicitIdSort() {
        stubEmptyPage();
        Sort sort = Sort.by(Sort.Order.asc("amount"), Sort.Order.desc("id"));

        transactionService.list(NO_FILTERS, PageRequest.of(0, 20, sort));

        assertThat(capturePageable().getSort()).isEqualTo(sort);
    }

    @Test
    void shouldMapResultsWithoutChangingBalancesOrPersistingTransactions() {
        Transaction transaction = new Transaction();
        TransactionResponse response = mock(TransactionResponse.class);
        when(transactionRepository.findAll(
                ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)
        )).thenAnswer(invocation -> new PageImpl<>(List.of(transaction), invocation.getArgument(1), 5));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        Page<TransactionListItemResponse> result = transactionService.list(NO_FILTERS, PageRequest.of(1, 1));

        assertThat(result.getContent()).singleElement().satisfies(item -> {
            assertThat(item.transaction()).isEqualTo(response);
            assertThat(item.displayAmount()).isNull();
            assertThat(item.installmentPurchase()).isFalse();
        });
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(5);
        capturePageable();
        verify(transactionMapper).toResponse(transaction);
        verifyNoMoreInteractions(transactionRepository, transactionMapper);
        verifyNoInteractions(accountRepository, categoryRepository, accountBalanceService, transactionImpactService);
    }

    @Test
    void shouldNotQueryWhenAuthenticationIsUnavailable() {
        when(currentUserService.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThatThrownBy(() -> transactionService.list(NO_FILTERS, PageRequest.of(0, 20)))
                .isInstanceOf(UnauthenticatedUserException.class);

        assertNoQueryOrFinancialChanges();
    }

    private void stubEmptyPage() {
        when(transactionRepository.findAll(
                ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)
        )).thenAnswer(invocation -> Page.empty(invocation.getArgument(1)));
    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAll(
                ArgumentMatchers.<Specification<Transaction>>any(), captor.capture()
        );
        return captor.getValue();
    }

    private void assertNoQueryOrFinancialChanges() {
        verifyNoInteractions(transactionRepository, transactionMapper, accountRepository,
                categoryRepository, accountBalanceService, transactionImpactService);
    }

    private static TransactionFilterRequest filters(
            LocalDate start, LocalDate end, BigDecimal min, BigDecimal max
    ) {
        return new TransactionFilterRequest(start, end, null, null, null, null, null, min, max, null);
    }
}
