package com.amorim.finance_manager.budget.service;

import com.amorim.finance_manager.budget.dto.BudgetResponse;
import com.amorim.finance_manager.budget.dto.CreateBudgetRequest;
import com.amorim.finance_manager.budget.dto.UpdateBudgetRequest;
import com.amorim.finance_manager.budget.entity.Budget;
import com.amorim.finance_manager.budget.mapper.BudgetMapper;
import com.amorim.finance_manager.budget.model.BudgetAlertSnapshot;
import com.amorim.finance_manager.budget.model.BudgetAlertStatus;
import com.amorim.finance_manager.budget.repository.BudgetRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.shared.exception.BudgetAlreadyExistsException;
import com.amorim.finance_manager.shared.exception.BudgetNotFoundException;
import com.amorim.finance_manager.shared.exception.CategoryNotFoundException;
import com.amorim.finance_manager.shared.exception.IncompatibleCategoryTypeException;
import com.amorim.finance_manager.shared.exception.InvalidBudgetUpdateException;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BUDGET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EXPENSE_CATEGORY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OTHER_EXPENSE_CATEGORY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant CREATED_AT = Instant.parse("2026-09-08T12:00:00Z");
    private static final BudgetAlertSnapshot ALERT_SNAPSHOT = new BudgetAlertSnapshot(
            new BigDecimal("1200.00"),
            new BigDecimal("80.00"),
            BudgetAlertStatus.ALERT
    );

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetMapper budgetMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private BudgetAlertEngine budgetAlertEngine;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void shouldCreateBudgetForOwnedExpenseCategory() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                EXPENSE_CATEGORY_ID,
                9,
                2026,
                new BigDecimal("1500.00")
        );
        Category category = category(EXPENSE_CATEGORY_ID, CategoryType.EXPENSE);
        Budget budget = budget(BUDGET_ID, EXPENSE_CATEGORY_ID, 9, 2026, "1500.00");
        budget.setUserId(null);
        BudgetResponse response = response(budget);

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(categoryRepository.findByIdAndUserId(EXPENSE_CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                USER_ID,
                EXPENSE_CATEGORY_ID,
                9,
                2026
        )).thenReturn(false);
        when(budgetMapper.toEntity(request)).thenReturn(budget);
        when(budgetRepository.saveAndFlush(budget)).thenReturn(budget);
        mockResponse(budget, response);

        BudgetResponse result = budgetService.create(request);

        assertThat(result).isEqualTo(response);
        assertThat(budget.getUserId()).isEqualTo(USER_ID);
        verify(budgetRepository).saveAndFlush(budget);
    }

    @Test
    void shouldRejectCategoryThatDoesNotBelongToCurrentUser() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                EXPENSE_CATEGORY_ID,
                9,
                2026,
                new BigDecimal("1500.00")
        );

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(categoryRepository.findByIdAndUserId(EXPENSE_CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.create(request))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Categoria não encontrada");

        verifyNoInteractions(budgetMapper);
        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectIncomeCategory() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                EXPENSE_CATEGORY_ID,
                9,
                2026,
                new BigDecimal("1500.00")
        );

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(categoryRepository.findByIdAndUserId(EXPENSE_CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category(EXPENSE_CATEGORY_ID, CategoryType.INCOME)));

        assertThatThrownBy(() -> budgetService.create(request))
                .isInstanceOf(IncompatibleCategoryTypeException.class)
                .hasMessage("Tipo de categoria incompatível");

        verifyNoInteractions(budgetMapper);
        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectDuplicateBudgetBeforeSaving() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                EXPENSE_CATEGORY_ID,
                9,
                2026,
                new BigDecimal("1500.00")
        );

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(categoryRepository.findByIdAndUserId(EXPENSE_CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category(EXPENSE_CATEGORY_ID, CategoryType.EXPENSE)));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                USER_ID,
                EXPENSE_CATEGORY_ID,
                9,
                2026
        )).thenReturn(true);

        assertThatThrownBy(() -> budgetService.create(request))
                .isInstanceOf(BudgetAlreadyExistsException.class)
                .hasMessage("Já existe um orçamento para a categoria e o período informados");

        verifyNoInteractions(budgetMapper);
        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldConvertDatabaseConstraintViolationToDuplicateBudget() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                EXPENSE_CATEGORY_ID,
                9,
                2026,
                new BigDecimal("1500.00")
        );
        Budget budget = budget(BUDGET_ID, EXPENSE_CATEGORY_ID, 9, 2026, "1500.00");

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(categoryRepository.findByIdAndUserId(EXPENSE_CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category(EXPENSE_CATEGORY_ID, CategoryType.EXPENSE)));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                USER_ID,
                EXPENSE_CATEGORY_ID,
                9,
                2026
        )).thenReturn(false);
        when(budgetMapper.toEntity(request)).thenReturn(budget);
        when(budgetRepository.saveAndFlush(budget))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> budgetService.create(request))
                .isInstanceOf(BudgetAlreadyExistsException.class);
    }

    @Test
    void shouldListOnlyCurrentUserBudgetsInRepositoryOrder() {
        Budget september = budget(BUDGET_ID, EXPENSE_CATEGORY_ID, 9, 2026, "1500.00");
        Budget august = budget(UUID.randomUUID(), OTHER_EXPENSE_CATEGORY_ID, 8, 2026, "900.00");
        BudgetResponse septemberResponse = response(september);
        BudgetResponse augustResponse = response(august);

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(budgetRepository.findAllByUserIdOrderByYearDescMonthDescCreatedAtDesc(USER_ID))
                .thenReturn(List.of(september, august));
        mockResponse(september, septemberResponse);
        mockResponse(august, augustResponse);

        assertThat(budgetService.findAll())
                .containsExactly(septemberResponse, augustResponse);
    }

    @Test
    void shouldFindOwnedBudgetById() {
        Budget budget = budget(BUDGET_ID, EXPENSE_CATEGORY_ID, 9, 2026, "1500.00");
        BudgetResponse response = response(budget);

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID))
                .thenReturn(Optional.of(budget));
        mockResponse(budget, response);

        assertThat(budgetService.findById(BUDGET_ID)).isEqualTo(response);
    }

    @Test
    void shouldHideForeignBudgetAsNotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.findById(BUDGET_ID))
                .isInstanceOf(BudgetNotFoundException.class)
                .hasMessage("Orçamento não encontrado");

        verifyNoInteractions(budgetMapper);
    }

    @Test
    void shouldUpdateAmountWithoutChangingThePeriod() {
        Budget budget = budget(BUDGET_ID, EXPENSE_CATEGORY_ID, 9, 2026, "1500.00");
        UpdateBudgetRequest request = new UpdateBudgetRequest(
                null,
                null,
                null,
                new BigDecimal("1800.00")
        );
        BudgetResponse response = new BudgetResponse(
                BUDGET_ID,
                EXPENSE_CATEGORY_ID,
                9,
                2026,
                new BigDecimal("1800.00"),
                ALERT_SNAPSHOT.spentAmount(),
                ALERT_SNAPSHOT.usagePercentage(),
                ALERT_SNAPSHOT.alertStatus(),
                CREATED_AT,
                CREATED_AT
        );

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID))
                .thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(EXPENSE_CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category(EXPENSE_CATEGORY_ID, CategoryType.EXPENSE)));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYearAndIdNot(
                USER_ID,
                EXPENSE_CATEGORY_ID,
                9,
                2026,
                BUDGET_ID
        )).thenReturn(false);
        doAnswer(invocation -> {
            budget.setAmountLimit(request.amountLimit());
            return null;
        }).when(budgetMapper).updateEntity(request, budget);
        when(budgetRepository.saveAndFlush(budget)).thenReturn(budget);
        mockResponse(budget, response);

        BudgetResponse result = budgetService.update(BUDGET_ID, request);

        assertThat(result).isEqualTo(response);
        assertThat(budget.getAmountLimit()).isEqualByComparingTo("1800.00");
        assertThat(budget.getMonth()).isEqualTo(9);
        assertThat(budget.getYear()).isEqualTo(2026);
    }

    @Test
    void shouldRejectUpdateThatWouldDuplicateAnotherBudget() {
        Budget budget = budget(BUDGET_ID, EXPENSE_CATEGORY_ID, 10, 2026, "1500.00");
        UpdateBudgetRequest request = new UpdateBudgetRequest(null, 9, null, null);

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID))
                .thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(EXPENSE_CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category(EXPENSE_CATEGORY_ID, CategoryType.EXPENSE)));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYearAndIdNot(
                USER_ID,
                EXPENSE_CATEGORY_ID,
                9,
                2026,
                BUDGET_ID
        )).thenReturn(true);

        assertThatThrownBy(() -> budgetService.update(BUDGET_ID, request))
                .isInstanceOf(BudgetAlreadyExistsException.class);

        verify(budgetMapper, never()).updateEntity(any(), any());
        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectIncomeCategoryDuringUpdate() {
        Budget budget = budget(BUDGET_ID, EXPENSE_CATEGORY_ID, 9, 2026, "1500.00");
        UpdateBudgetRequest request = new UpdateBudgetRequest(
                OTHER_EXPENSE_CATEGORY_ID,
                null,
                null,
                null
        );

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID))
                .thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(OTHER_EXPENSE_CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category(OTHER_EXPENSE_CATEGORY_ID, CategoryType.INCOME)));

        assertThatThrownBy(() -> budgetService.update(BUDGET_ID, request))
                .isInstanceOf(IncompatibleCategoryTypeException.class);

        verify(budgetMapper, never()).updateEntity(any(), any());
        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectForeignCategoryDuringUpdate() {
        Budget budget = budget(BUDGET_ID, EXPENSE_CATEGORY_ID, 9, 2026, "1500.00");
        UpdateBudgetRequest request = new UpdateBudgetRequest(
                OTHER_EXPENSE_CATEGORY_ID,
                null,
                null,
                null
        );

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID))
                .thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(OTHER_EXPENSE_CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.update(BUDGET_ID, request))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(budgetMapper, never()).updateEntity(any(), any());
        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectEmptyUpdate() {
        UpdateBudgetRequest request = new UpdateBudgetRequest(null, null, null, null);

        assertThatThrownBy(() -> budgetService.update(BUDGET_ID, request))
                .isInstanceOf(InvalidBudgetUpdateException.class)
                .hasMessage("Informe ao menos um campo para atualização");

        verifyNoInteractions(currentUserService, categoryRepository, budgetMapper);
        verifyNoInteractions(budgetRepository);
    }

    @Test
    void shouldDeleteOwnedBudget() {
        Budget budget = budget(BUDGET_ID, EXPENSE_CATEGORY_ID, 9, 2026, "1500.00");

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID))
                .thenReturn(Optional.of(budget));

        budgetService.delete(BUDGET_ID);

        verify(budgetRepository).delete(budget);
        verify(budgetRepository).flush();
    }

    private Category category(UUID id, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setUserId(USER_ID);
        category.setName("Categoria");
        category.setType(type);
        category.setStatus(CategoryStatus.ACTIVE);
        return category;
    }

    private Budget budget(
            UUID id,
            UUID categoryId,
            Integer month,
            Integer year,
            String amount
    ) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setUserId(USER_ID);
        budget.setCategoryId(categoryId);
        budget.setMonth(month);
        budget.setYear(year);
        budget.setAmountLimit(new BigDecimal(amount));
        budget.setCreatedAt(CREATED_AT);
        budget.setUpdatedAt(CREATED_AT);
        return budget;
    }

    private BudgetResponse response(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategoryId(),
                budget.getMonth(),
                budget.getYear(),
                budget.getAmountLimit(),
                ALERT_SNAPSHOT.spentAmount(),
                ALERT_SNAPSHOT.usagePercentage(),
                ALERT_SNAPSHOT.alertStatus(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }

    private void mockResponse(Budget budget, BudgetResponse response) {
        when(budgetAlertEngine.evaluate(budget)).thenReturn(ALERT_SNAPSHOT);
        when(budgetMapper.toResponse(budget, ALERT_SNAPSHOT)).thenReturn(response);
    }
}
