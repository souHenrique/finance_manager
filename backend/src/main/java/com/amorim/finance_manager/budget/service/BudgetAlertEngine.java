package com.amorim.finance_manager.budget.service;

import com.amorim.finance_manager.budget.entity.Budget;
import com.amorim.finance_manager.budget.model.BudgetAlertSnapshot;
import com.amorim.finance_manager.budget.model.BudgetAlertStatus;
import com.amorim.finance_manager.budget.projection.BudgetSpendAggregate;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetAlertEngine {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private static final BigDecimal ALERT_RATIO = new BigDecimal("0.80");

    private static final Set<TransactionType> INCLUDED_TYPES =
            Set.of(
                    TransactionType.EXPENSE,
                    TransactionType.CREDIT_CARD_PURCHASE
            );

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public BudgetAlertSnapshot evaluate(Budget budget) {
        validateBudgetLimit(budget);

        YearMonth referenceMonth = YearMonth.of(budget.getYear(), budget.getMonth());

        LocalDate periodStart = referenceMonth.atDay(1);

        LocalDate periodEndExclusive = referenceMonth.plusMonths(1).atDay(1);

        BigDecimal spentAmount = transactionRepository.sumForBudget(
                budget.getUserId(),
                budget.getCategoryId(),
                periodStart,
                periodEndExclusive,
                INCLUDED_TYPES,
                TransactionStatus.CANCELLED
        );

        return createSnapshot(budget, spentAmount);
    }

    @Transactional(readOnly = true)
    public Map<UUID, BudgetAlertSnapshot> evaluateAll(List<Budget> budgets) {
        if (budgets.isEmpty()) {
            return Map.of();
        }

        budgets.forEach(this::validateBudgetLimit);

        Budget reference = budgets.getFirst();

        boolean mixedPeriods = budgets.stream().anyMatch(budget ->
                !budget.getUserId().equals(reference.getUserId())
                        || !budget.getYear().equals(reference.getYear())
                        || !budget.getMonth().equals(reference.getMonth())
        );

        if (mixedPeriods) {
            throw new IllegalArgumentException("Os orçamentos devem pertencer ao mesmo usuário e período");
        }

        YearMonth period = YearMonth.of(reference.getYear(), reference.getMonth());

        Set<UUID> categoryIds = budgets.stream()
                .map(Budget::getCategoryId)
                .collect(Collectors.toSet());

        Map<UUID, BigDecimal> spentByCategory =
                transactionRepository.sumByCategoryForBudgets(
                                reference.getUserId(),
                                categoryIds,
                                period.atDay(1),
                                period.plusMonths(1).atDay(1),
                                INCLUDED_TYPES,
                                TransactionStatus.CANCELLED
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                BudgetSpendAggregate::categoryId,
                                BudgetSpendAggregate::spentAmount
                        ));
        return budgets.stream().collect(Collectors.toMap(
                Budget::getId,
                budget -> createSnapshot(
                        budget,
                        spentByCategory.getOrDefault(
                                budget.getCategoryId(),
                                BigDecimal.ZERO
                        )
                )
        ));
    }

    private BudgetAlertSnapshot createSnapshot(Budget budget, BigDecimal spentAmount) {

        BigDecimal normalizedSpent = spentAmount.setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal usagePercentage = normalizedSpent
                .multiply(ONE_HUNDRED)
                .divide(budget.getAmountLimit(), 2, RoundingMode.HALF_EVEN);

        return new BudgetAlertSnapshot(
                normalizedSpent,
                usagePercentage,
                classify(normalizedSpent, budget.getAmountLimit()));
    }

    private BudgetAlertStatus classify(BigDecimal spentAmount, BigDecimal amountLimit) {
        if (spentAmount.compareTo(amountLimit) >= 0) {
            return BudgetAlertStatus.LIMIT_REACHED;
        }

        BigDecimal alertAmount = amountLimit.multiply(ALERT_RATIO);

        if (spentAmount.compareTo(alertAmount) >= 0) {
            return BudgetAlertStatus.ALERT;
        }

        return BudgetAlertStatus.NORMAL;
    }

    private void validateBudgetLimit(Budget budget) {
        if (budget.getAmountLimit() == null || budget.getAmountLimit().signum() <= 0) {
            throw new IllegalStateException("O limite do orçamento deve ser maior que zero");
        }
    }
}
