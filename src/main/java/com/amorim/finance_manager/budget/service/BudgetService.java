package com.amorim.finance_manager.budget.service;

import com.amorim.finance_manager.budget.dto.BudgetResponse;
import com.amorim.finance_manager.budget.dto.CreateBudgetRequest;
import com.amorim.finance_manager.budget.dto.UpdateBudgetRequest;
import com.amorim.finance_manager.budget.entity.Budget;
import com.amorim.finance_manager.budget.mapper.BudgetMapper;
import com.amorim.finance_manager.budget.model.BudgetAlertSnapshot;
import com.amorim.finance_manager.budget.repository.BudgetRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.shared.exception.*;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetMapper budgetMapper;
    private final CurrentUserService currentUserService;
    private final BudgetAlertEngine budgetAlertEngine;

    @Transactional
    public BudgetResponse create(CreateBudgetRequest request) {
        UUID userId = currentUserService.getCurrentUserId();

        validateOwnedExpenseCategory(request.categoryId(), userId);

        ensureUniqueBudget(userId, request.categoryId(), request.month(), request.year());

        Budget budget = budgetMapper.toEntity(request);
        budget.setUserId(userId);

        return save(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> findAll() {
        UUID userId = currentUserService.getCurrentUserId();

        return budgetRepository
                .findAllByUserIdOrderByYearDescMonthDescCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse findById(UUID budgetId) {
        UUID userId = currentUserService.getCurrentUserId();

        return toResponse(findOwnedBudget(budgetId, userId));
    }

    @Transactional
    public BudgetResponse update(UUID budgetId, UpdateBudgetRequest request) {
        validateUpdate(request);

        UUID userId = currentUserService.getCurrentUserId();
        Budget budget = findOwnedBudget(budgetId, userId);

        UUID categoryId = request.categoryId() != null ? request.categoryId() : budget.getCategoryId();

        Integer month = request.month() != null ? request.month() : budget.getMonth();

        Integer year = request.year() != null ? request.year() : budget.getYear();

        validateOwnedExpenseCategory(categoryId, userId);

        ensureUniqueBudgetExceptCurrent(userId, categoryId, month, year, budgetId);

        budgetMapper.updateEntity(request, budget);

        return save(budget);
    }

    @Transactional
    public void delete(UUID budgetId) {
        UUID userId = currentUserService.getCurrentUserId();

        Budget budget = findOwnedBudget(budgetId, userId);

        budgetRepository.delete(budget);

        budgetRepository.flush();
    }

    private BudgetResponse save(Budget budget) {
        try {
            Budget saved = budgetRepository.saveAndFlush(budget);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new BudgetAlreadyExistsException();
        }
    }

    private Budget findOwnedBudget(UUID budgetId, UUID userId) {
        return budgetRepository
                .findByIdAndUserId(budgetId, userId)
                .orElseThrow(BudgetNotFoundException::new);
    }

    private void validateOwnedExpenseCategory(UUID categoryId, UUID userId) {
        Category category = categoryRepository
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);

        if (category.getType() != CategoryType.EXPENSE) {
            throw new IncompatibleCategoryTypeException();
        }
    }

    private void ensureUniqueBudget(UUID userId, UUID categoryId, Integer month, Integer year) {
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(userId, categoryId, month, year)) {
            throw new BudgetAlreadyExistsException();
        }
    }

    private void ensureUniqueBudgetExceptCurrent(
            UUID userId,
            UUID categoryId,
            Integer month,
            Integer year,
            UUID budgetId
    ) {
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYearAndIdNot(
                userId,
                categoryId,
                month,
                year,
                budgetId
        )) {
            throw new BudgetAlreadyExistsException();
        }
    }

    private void validateUpdate(UpdateBudgetRequest request) {
        if (request.categoryId() == null
            && request.month() == null
            && request.year() == null
            && request.amountLimit() == null) {
            throw new InvalidBudgetUpdateException("Informe ao menos um campo para atualização");
        }
    }

    private BudgetResponse toResponse(Budget budget) {
        BudgetAlertSnapshot snapshot = budgetAlertEngine.evaluate(budget);

        return budgetMapper.toResponse(budget, snapshot);
    }
}
