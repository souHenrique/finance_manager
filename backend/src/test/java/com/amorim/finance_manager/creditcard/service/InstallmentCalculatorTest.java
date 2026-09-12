package com.amorim.finance_manager.creditcard.service;

import com.amorim.finance_manager.shared.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallmentCalculatorTest {

    private final InstallmentCalculator calculator = new InstallmentCalculator();

    @Test
    void shouldKeepTheFullAmountForOneInstallment() {
        List<BigDecimal> installments = calculator.split(new BigDecimal("100.00"), 1);

        assertThat(installments).containsExactly(new BigDecimal("100.00"));
        assertMoneyInvariant(installments, "100.00");
    }

    @Test
    void shouldSplitAnExactlyDivisibleAmountIntoTwoInstallments() {
        List<BigDecimal> installments = calculator.split(new BigDecimal("100.00"), 2);

        assertThat(installments).containsExactly(
                new BigDecimal("50.00"),
                new BigDecimal("50.00")
        );
        assertMoneyInvariant(installments, "100.00");
    }

    @Test
    void shouldPutTheResidualAmountInTheLastInstallment() {
        List<BigDecimal> installments = calculator.split(new BigDecimal("100.00"), 3);

        assertThat(installments).containsExactly(
                new BigDecimal("33.33"),
                new BigDecimal("33.33"),
                new BigDecimal("33.34")
        );
        assertMoneyInvariant(installments, "100.00");
    }

    @Test
    void shouldUseHalfEvenRoundingBeforeAdjustingTheLastInstallment() {
        List<BigDecimal> installments = calculator.split(new BigDecimal("10.05"), 2);

        assertThat(installments).containsExactly(
                new BigDecimal("5.02"),
                new BigDecimal("5.03")
        );
        assertMoneyInvariant(installments, "10.05");
    }

    @Test
    void shouldSupportManyInstallmentsWithoutLosingCents() {
        List<BigDecimal> installments = calculator.split(new BigDecimal("100.00"), 12);

        assertThat(installments).containsExactly(
                new BigDecimal("8.33"),
                new BigDecimal("8.33"),
                new BigDecimal("8.33"),
                new BigDecimal("8.33"),
                new BigDecimal("8.33"),
                new BigDecimal("8.33"),
                new BigDecimal("8.33"),
                new BigDecimal("8.33"),
                new BigDecimal("8.33"),
                new BigDecimal("8.33"),
                new BigDecimal("8.33"),
                new BigDecimal("8.37")
        );
        assertMoneyInvariant(installments, "100.00");
    }

    @Test
    void shouldRejectMissingZeroOrNegativeAmounts() {
        assertThatThrownBy(() -> calculator.split(null, 1))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Valor da compra deve ser maior que zero");
        assertThatThrownBy(() -> calculator.split(BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Valor da compra deve ser maior que zero");
        assertThatThrownBy(() -> calculator.split(new BigDecimal("-0.01"), 1))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Valor da compra deve ser maior que zero");
    }

    @Test
    void shouldRejectZeroOrNegativeInstallmentCounts() {
        assertThatThrownBy(() -> calculator.split(new BigDecimal("10.00"), 0))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Quantidade de parcelas deve ser maior que zero");
        assertThatThrownBy(() -> calculator.split(new BigDecimal("10.00"), -1))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Quantidade de parcelas deve ser maior que zero");
    }

    @Test
    void shouldRejectACombinationThatWouldCreateANonPositiveInstallment() {
        assertThatThrownBy(() -> calculator.split(new BigDecimal("0.06"), 4))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("O valor é insuficiente para a quantidade de parcelas");
    }

    private void assertMoneyInvariant(List<BigDecimal> installments, String expectedTotal) {
        assertThat(installments).allSatisfy(amount -> {
            assertThat(amount.scale()).isEqualTo(2);
            assertThat(amount).isPositive();
        });

        BigDecimal total = installments.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(total).isEqualByComparingTo(expectedTotal);
    }
}
