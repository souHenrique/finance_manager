package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnnualCashFlowCalculatorTest {

    private final CashFlowCalculator calculator = new CashFlowCalculator();

    @Test
    void shouldReturnZeroTotalsWithoutAnnualRows() {
        var totals = calculator.summarizeTotals(List.of());

        assertThat(totals.inflows()).isZero();
        assertThat(totals.outflows()).isZero();
        assertThat(totals.net()).isZero();
    }

    @Test
    void shouldClassifyIncomeExpenseAndInvoicePaymentWithoutFloatingPointRounding() {
        var totals = calculator.summarizeTotals(List.of(
                row(TransactionType.INCOME, "1000.10"),
                row(TransactionType.INCOME, "0.20"),
                row(TransactionType.EXPENSE, "300.05"),
                row(TransactionType.CREDIT_CARD_PAYMENT, "200.15")
        ));

        assertThat(totals.inflows()).isEqualByComparingTo("1000.30");
        assertThat(totals.outflows()).isEqualByComparingTo("500.20");
        assertThat(totals.net()).isEqualByComparingTo("500.10");
    }

    @ParameterizedTest
    @EnumSource(
            value = TransactionType.class,
            names = {"TRANSFER", "CREDIT_CARD_PURCHASE", "ADJUSTMENT"}
    )
    void shouldRejectTypesOutsideTheAnnualCashAggregationContract(TransactionType type) {
        assertThatThrownBy(() -> calculator.summarizeTotals(List.of(row(type, "10.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(type.name());
    }

    private AnnualCashFlowAggregate row(TransactionType type, String amount) {
        return new AnnualCashFlowAggregate(
                1,
                type,
                new BigDecimal(amount)
        );
    }
}
