package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.report.projection.CompetenceAggregate;
import com.amorim.finance_manager.report.repository.CompetenceReportRepository;
import com.amorim.finance_manager.shared.exception.InvalidReportPeriodException;
import com.amorim.finance_manager.shared.exception.UnauthenticatedUserException;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetenceReportServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID INCOME_CATEGORY_ID = UUID.randomUUID();
    private static final UUID EXPENSE_CATEGORY_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 30);
    private static final Set<TransactionType> INCLUDED_TYPES = Set.of(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
            TransactionType.CREDIT_CARD_PURCHASE
    );

    @Mock
    private CompetenceReportRepository reportRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CurrentUserService currentUserService;

    private CompetenceReportService service;

    @BeforeEach
    void setUp() {
        service = new CompetenceReportService(
                reportRepository,
                categoryRepository,
                currentUserService,
                new CompetenceReportCalculator()
        );
    }

    @Test
    void shouldQueryCompletedIncludedTypesForAuthenticatedUserAndResolveCategories() {
        List<CompetenceAggregate> rows = List.of(
                row(TransactionType.INCOME, INCOME_CATEGORY_ID, "2000.00"),
                row(TransactionType.EXPENSE, EXPENSE_CATEGORY_ID, "200.00"),
                row(TransactionType.CREDIT_CARD_PURCHASE, EXPENSE_CATEGORY_ID, "800.00")
        );

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(reportRepository.aggregate(
                USER_ID,
                START,
                END,
                TransactionStatus.COMPLETED,
                INCLUDED_TYPES
        )).thenReturn(rows);
        when(categoryRepository.findAllByUserIdAndIdIn(
                USER_ID,
                Set.of(INCOME_CATEGORY_ID, EXPENSE_CATEGORY_ID)
        )).thenReturn(List.of(
                category(INCOME_CATEGORY_ID, "Salário"),
                category(EXPENSE_CATEGORY_ID, "Compras")
        ));

        var response = service.generate(START, END);

        assertThat(response.totalIncome()).isEqualByComparingTo("2000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("1000.00");
        assertThat(response.result()).isEqualByComparingTo("1000.00");
        assertThat(response.incomeCategories().getFirst().name()).isEqualTo("Salário");
        assertThat(response.expenseCategories().getFirst().name()).isEqualTo("Compras");
        verify(reportRepository).aggregate(
                USER_ID,
                START,
                END,
                TransactionStatus.COMPLETED,
                INCLUDED_TYPES
        );
        verifyNoMoreInteractions(reportRepository);
        verify(categoryRepository).findAllByUserIdAndIdIn(
                USER_ID,
                Set.of(INCOME_CATEGORY_ID, EXPENSE_CATEGORY_ID)
        );
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    void shouldAvoidCategoryQueryWhenReportHasNoRows() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(reportRepository.aggregate(
                USER_ID,
                START,
                END,
                TransactionStatus.COMPLETED,
                INCLUDED_TYPES
        )).thenReturn(List.of());

        var response = service.generate(START, END);

        assertThat(response.totalIncome()).isZero();
        assertThat(response.totalExpenses()).isZero();
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void shouldSumOnlyCreditCardPurchasesFromTheReferencedInvoiceMonth() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(reportRepository.sumCreditCardPurchasesByInvoicePeriod(
                USER_ID,
                TransactionStatus.COMPLETED,
                TransactionType.CREDIT_CARD_PURCHASE,
                2026,
                9
        )).thenReturn(new BigDecimal("1000.00"));

        assertThat(service.creditCardPurchaseOutflows(2026, 9))
                .isEqualByComparingTo("1000.00");
        verify(reportRepository).sumCreditCardPurchasesByInvoicePeriod(
                USER_ID,
                TransactionStatus.COMPLETED,
                TransactionType.CREDIT_CARD_PURCHASE,
                2026,
                9
        );
        verifyNoInteractions(categoryRepository);
        verifyNoMoreInteractions(reportRepository);
    }

    @ParameterizedTest
    @MethodSource("invalidPeriods")
    void shouldRejectInvalidPeriodsBeforeAccessingAuthenticationOrRepositories(
            LocalDate startDate,
            LocalDate endDate
    ) {
        assertThatThrownBy(() -> service.generate(startDate, endDate))
                .isInstanceOf(InvalidReportPeriodException.class);

        verifyNoInteractions(currentUserService, reportRepository, categoryRepository);
    }

    @Test
    void shouldNotQueryReportWithoutAuthenticatedUser() {
        when(currentUserService.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThatThrownBy(() -> service.generate(START, END))
                .isInstanceOf(UnauthenticatedUserException.class);

        verifyNoInteractions(reportRepository, categoryRepository);
    }

    @Test
    void shouldDeclareReadOnlyRepeatableReadTransaction() {
        Transactional transactional = CompetenceReportService.class.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
        assertThat(transactional.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
    }

    static Stream<Arguments> invalidPeriods() {
        return Stream.of(
                Arguments.of(null, END),
                Arguments.of(START, null),
                Arguments.of(END, START)
        );
    }

    private CompetenceAggregate row(TransactionType type, UUID categoryId, String amount) {
        return new CompetenceAggregate(type, categoryId, new BigDecimal(amount));
    }

    private Category category(UUID id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setUserId(USER_ID);
        category.setName(name);
        return category;
    }
}
