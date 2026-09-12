package com.amorim.finance_manager.transaction.service;

import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.shared.exception.InvalidTransactionException;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionImpactService {

    private final AccountBalanceService accountBalanceService;

    public void apply(UUID userId, Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            return;
        }

        switch (transaction.getType()) {
            case INCOME -> accountBalanceService.credit(
                    userId,
                    transaction.getDestinationAccountId(),
                    transaction.getAmount()
            );

            case EXPENSE -> accountBalanceService.debit(
                    userId,
                    transaction.getSourceAccountId(),
                    transaction.getAmount()
            );

            case TRANSFER -> {
                accountBalanceService.debit(
                        userId,
                        transaction.getSourceAccountId(),
                        transaction.getAmount()
                );

                accountBalanceService.credit(
                        userId,
                        transaction.getDestinationAccountId(),
                        transaction.getAmount()
                );
            }

            default -> throw new InvalidTransactionException(
                    "Tipo de transação não suportado para alteração de saldo"
            );
        }
    }

    public void reverse(UUID userId, Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            return;
        }

        switch (transaction.getType()) {
            case INCOME -> accountBalanceService.reverseCredit(
                    userId,
                    transaction.getDestinationAccountId(),
                    transaction.getAmount()
            );

            case EXPENSE -> accountBalanceService.reverseDebit(
                    userId,
                    transaction.getSourceAccountId(),
                    transaction.getAmount()
            );

            case TRANSFER -> {
                accountBalanceService.reverseCredit(
                        userId,
                        transaction.getDestinationAccountId(),
                        transaction.getAmount()
                );

                accountBalanceService.reverseDebit(
                        userId,
                        transaction.getSourceAccountId(),
                        transaction.getAmount()
                );
            }

            default -> throw new InvalidTransactionException(
                    "Tipo de transação não suportado para reversão"
            );
        }
    }
}
