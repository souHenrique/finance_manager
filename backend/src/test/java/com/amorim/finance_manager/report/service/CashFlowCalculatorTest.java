package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.report.dto.CashFlowSummaryResponse;
import com.amorim.finance_manager.report.dto.CategoryCashFlowResponse;
import com.amorim.finance_manager.report.projection.CashFlowAggregate;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashFlowCalculatorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 3);
    private static final UUID CATEGORY = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final CashFlowCalculator calculator = new CashFlowCalculator();

    @Test
    void shouldReturnZeroAmountsAndEmptyCategoriesWithoutMovements() {
        var summary = calculator.summarize(List.of(), Map.of());

        assertTotals(summary, "0.00", "0.00", "0.00", "0.00");
        assertThat(summary.incomeCategories()).isEmpty();
        assertThat(summary.expenseCategories()).isEmpty();
    }

    @Test
    void shouldSumIncomeAndExpensesWithoutFloatingPointRounding() {
        var summary = calculator.summarize(List.of(
                row(TransactionType.INCOME, CATEGORY, "0.10"),
                row(TransactionType.INCOME, CATEGORY, "0.20"),
                row(TransactionType.EXPENSE, CATEGORY, "0.07")
        ), Map.of(CATEGORY, "Category"));

        assertTotals(summary, "0.30", "0.07", "0.23", "0.00");
        assertThat(summary.incomeCategories()).singleElement()
                .satisfies(category -> assertThat(category.amount()).isEqualByComparingTo("0.30"));
        assertThat(summary.expenseCategories()).singleElement()
                .satisfies(category -> assertThat(category.amount()).isEqualByComparingTo("0.07"));
    }

    @Test
    void shouldIncludeInvoicePaymentsInOutflowsExactlyOnceWithoutAttributingTheirCategories() {
        var summary = calculator.summarize(List.of(
                row(TransactionType.INCOME, CATEGORY, "5000.00"),
                row(TransactionType.EXPENSE, CATEGORY, "300.00"),
                row(TransactionType.CREDIT_CARD_PAYMENT, null, "1000.00"),
                row(TransactionType.CREDIT_CARD_PAYMENT, CATEGORY, "200.00")
        ), Map.of(CATEGORY, "Category"));

        assertTotals(summary, "5000.00", "1500.00", "3500.00", "1200.00");
        assertThat(summary.expenseCategories()).singleElement()
                .satisfies(category -> assertThat(category.amount()).isEqualByComparingTo("300.00"));
    }

    @Test
    void shouldAllowNegativeNetAndInvoiceOnlyPeriods() {
        var summary = calculator.summarize(List.of(
                row(TransactionType.CREDIT_CARD_PAYMENT, null, "230.45")
        ), Map.of());

        assertTotals(summary, "0.00", "230.45", "-230.45", "230.45");
        assertThat(summary.incomeCategories()).isEmpty();
        assertThat(summary.expenseCategories()).isEmpty();
    }

    @Test
    void shouldGroupTheSameCategoryAcrossDaysButKeepDifferentIdsWithTheSameNameSeparate() {
        UUID other = UUID.randomUUID();
        var summary = calculator.summarize(List.of(
                row(TransactionType.EXPENSE, CATEGORY, "10.00"),
                new CashFlowAggregate(DATE.plusDays(1), TransactionType.EXPENSE, CATEGORY, amount("20.00")),
                row(TransactionType.EXPENSE, other, "50.00")
        ), Map.of(CATEGORY, "Same name", other, "Same name"));

        assertThat(summary.expenseCategories()).extracting(CategoryCashFlowResponse::categoryId)
                .containsExactly(other, CATEGORY);
        assertThat(summary.expenseCategories().get(0).amount()).isEqualByComparingTo("50.00");
        assertThat(summary.expenseCategories().get(1).amount()).isEqualByComparingTo("30.00");
    }

    @Test
    void shouldRankCategoriesByAmountThenNameThenId() {
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID third = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID largest = UUID.randomUUID();
        var summary = calculator.summarize(List.of(
                row(TransactionType.INCOME, third, "20.00"),
                row(TransactionType.INCOME, second, "20.00"),
                row(TransactionType.INCOME, largest, "30.00"),
                row(TransactionType.INCOME, CATEGORY, "20.00")
        ), Map.of(CATEGORY, "Alpha", second, "Alpha", third, "Beta", largest, "Zulu"));

        assertThat(summary.incomeCategories()).extracting(CategoryCashFlowResponse::categoryId)
                .containsExactly(largest, CATEGORY, second, third);
    }

    @Test
    void shouldDistinguishMissingCategoryFromUnavailableCategoryName() {
        var summary = calculator.summarize(List.of(
                row(TransactionType.EXPENSE, null, "10.00"),
                row(TransactionType.EXPENSE, null, "20.00"),
                row(TransactionType.EXPENSE, CATEGORY, "5.00")
        ), Map.of());

        assertThat(summary.expenseCategories()).hasSize(2);
        assertThat(summary.expenseCategories().get(0).categoryId()).isNull();
        assertThat(summary.expenseCategories().get(0).name()).isEqualTo("Sem categoria");
        assertThat(summary.expenseCategories().get(0).amount()).isEqualByComparingTo("30.00");
        assertThat(summary.expenseCategories().get(1).name()).isEqualTo("Categoria indisponível");
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"TRANSFER", "CREDIT_CARD_PURCHASE", "ADJUSTMENT"})
    void shouldRejectRowsOutsideTheCashAggregationContract(TransactionType type) {
        // Filtering belongs to the repository; the calculator rejects a broken internal contract.
        assertThatThrownBy(() -> calculator.summarize(List.of(row(type, null, "10.00")), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(type.name());
    }

    @ParameterizedTest
    @CsvSource({
            "5000,2000,4500,1800,500,200,300",
            "100,200,300,50,-200,150,-350",
            "0,0,0,0,0,0,0",
            "0,0,100,30,-100,-30,-70",
            "100,30,0,0,100,30,70"
    })
    void shouldCompareCurrentMinusPreviousInMoneyNotPercentages(
            String currentIn, String currentOut, String previousIn, String previousOut,
            String differenceIn, String differenceOut, String differenceNet) {
        var comparison = calculator.compare(summary(currentIn, currentOut), summary(previousIn, previousOut));

        assertThat(comparison.inflowsDifference()).isEqualByComparingTo(differenceIn);
        assertThat(comparison.outflowsDifference()).isEqualByComparingTo(differenceOut);
        assertThat(comparison.netDifference()).isEqualByComparingTo(differenceNet);
    }

    private CashFlowAggregate row(TransactionType type, UUID categoryId, String value) {
        return new CashFlowAggregate(DATE, type, categoryId, amount(value));
    }

    private CashFlowSummaryResponse summary(String inflows, String outflows) {
        return new CashFlowSummaryResponse(amount(inflows), amount(outflows),
                amount(inflows).subtract(amount(outflows)), amount("0.00"), List.of(), List.of());
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private void assertTotals(CashFlowSummaryResponse summary, String inflows, String outflows,
                              String net, String invoices) {
        assertThat(summary.inflows()).isEqualByComparingTo(inflows);
        assertThat(summary.outflows()).isEqualByComparingTo(outflows);
        assertThat(summary.net()).isEqualByComparingTo(net);
        assertThat(summary.invoicePayments()).isEqualByComparingTo(invoices);
    }
}
