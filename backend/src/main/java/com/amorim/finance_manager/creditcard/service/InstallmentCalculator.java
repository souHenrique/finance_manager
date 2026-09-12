package com.amorim.finance_manager.creditcard.service;

import com.amorim.finance_manager.shared.exception.InvalidTransactionException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class InstallmentCalculator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public List<BigDecimal> split(
            BigDecimal amount,
            int installmentCount
    ) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionException(
                    "Valor da compra deve ser maior que zero"
            );
        }

        if (installmentCount <= 0) {
            throw new InvalidTransactionException(
                    "Quantidade de parcelas deve ser maior que zero"
            );
        }

        BigDecimal total = amount.setScale(
                MONEY_SCALE,
                ROUNDING_MODE
        );

        BigDecimal regularAmount = total.divide(
                BigDecimal.valueOf(installmentCount),
                MONEY_SCALE,
                ROUNDING_MODE
        );

        List<BigDecimal> amounts = new ArrayList<>(installmentCount);

        for (int index = 1; index < installmentCount; index++) {
            amounts.add(regularAmount);
        }

        BigDecimal alreadyAllocated = regularAmount.multiply(
                BigDecimal.valueOf(installmentCount - 1L)
        );

        BigDecimal lastAmount = total
                .subtract(alreadyAllocated)
                .setScale(MONEY_SCALE, ROUNDING_MODE);

        if (regularAmount.signum() <= 0
                || lastAmount.signum() <= 0) {
            throw new InvalidTransactionException(
                    "O valor é insuficiente para a quantidade de parcelas"
            );
        }

        amounts.add(lastAmount);

        return List.copyOf(amounts);
    }
}
