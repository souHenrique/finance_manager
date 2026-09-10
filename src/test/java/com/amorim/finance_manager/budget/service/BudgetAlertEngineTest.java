package com.amorim.finance_manager.budget.service;

import com.amorim.finance_manager.budget.entity.Budget;
import com.amorim.finance_manager.budget.model.BudgetAlertSnapshot;
import com.amorim.finance_manager.budget.model.BudgetAlertStatus;
import com.amorim.finance_manager.budget.projection.BudgetSpendAggregate;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class BudgetAlertEngineTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetAlertEngine engine;

    @ParameterizedTest
    @CsvSource({
            "79.99, NORMAL",
            "80.00, ALERT",
            "99.99, ALERT",
            "100.00, LIMIT_REACHED",
            "120.00, LIMIT_REACHED"
    })
    void shouldClassifyBudgetThresholds(
            String spent,
            BudgetAlertStatus expectedStatus
    ) {
        Budget budget = new Budget();
        budget.setUserId(USER_ID);
        budget.setCategoryId(CATEGORY_ID);
        budget.setMonth(9);
        budget.setYear(2026);
        budget.setAmountLimit(new BigDecimal("100.00"));

        when(transactionRepository.sumForBudget(
                eq(USER_ID),
                eq(CATEGORY_ID),
                eq(LocalDate.of(2026, 9, 1)),
                eq(LocalDate.of(2026, 10, 1)),
                eq(Set.of(
                        TransactionType.EXPENSE,
                        TransactionType.CREDIT_CARD_PURCHASE
                )),
                eq(TransactionStatus.CANCELLED)
        )).thenReturn(new BigDecimal(spent));

        BudgetAlertSnapshot result = engine.evaluate(budget);

        assertThat(result.spentAmount())
                .isEqualByComparingTo(spent);
        assertThat(result.usagePercentage())
                .isEqualByComparingTo(spent);
        assertThat(result.alertStatus())
                .isEqualTo(expectedStatus);
    }

    @Test
    void shouldEvaluateMultipleBudgetsWithOneAggregateQuery() {
        UUID firstBudgetId = UUID.randomUUID();
        UUID secondBudgetId = UUID.randomUUID();
        UUID secondCategoryId = UUID.randomUUID();

        Budget first = budget(
                firstBudgetId,
                CATEGORY_ID,
                "100.00"
        );

        Budget second = budget(
                secondBudgetId,
                secondCategoryId,
                "200.00"
        );

        when(transactionRepository.sumByCategoryForBudgets(
                USER_ID,
                Set.of(CATEGORY_ID, secondCategoryId),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 1),
                Set.of(
                        TransactionType.EXPENSE,
                        TransactionType.CREDIT_CARD_PURCHASE
                ),
                TransactionStatus.CANCELLED
        )).thenReturn(List.of(
                new BudgetSpendAggregate(
                        CATEGORY_ID,
                        new BigDecimal("80.00")
                ),
                new BudgetSpendAggregate(
                        secondCategoryId,
                        new BigDecimal("240.00")
                )
        ));

        Map<UUID, BudgetAlertSnapshot> snapshots =
                engine.evaluateAll(List.of(first, second));

        assertThat(snapshots.get(firstBudgetId).alertStatus())
                .isEqualTo(BudgetAlertStatus.ALERT);
        assertThat(snapshots.get(secondBudgetId).alertStatus())
                .isEqualTo(BudgetAlertStatus.LIMIT_REACHED);

        verify(transactionRepository).sumByCategoryForBudgets(
                USER_ID,
                Set.of(CATEGORY_ID, secondCategoryId),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 1),
                Set.of(
                        TransactionType.EXPENSE,
                        TransactionType.CREDIT_CARD_PURCHASE
                ),
                TransactionStatus.CANCELLED
        );
    }

    private Budget budget(
            UUID id,
            UUID categoryId,
            String limit
    ) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setUserId(USER_ID);
        budget.setCategoryId(categoryId);
        budget.setMonth(9);
        budget.setYear(2026);
        budget.setAmountLimit(new BigDecimal(limit));
        return budget;
    }
}
