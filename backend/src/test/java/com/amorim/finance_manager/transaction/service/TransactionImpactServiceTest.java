package com.amorim.finance_manager.transaction.service;

import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.shared.exception.InvalidTransactionException;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TransactionImpactServiceTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID SOURCE_ACCOUNT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID DESTINATION_ACCOUNT_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final BigDecimal AMOUNT = new BigDecimal("75.50");

    @Mock
    private AccountBalanceService accountBalanceService;

    @InjectMocks
    private TransactionImpactService transactionImpactService;

    @Test
    void shouldApplyCompletedIncomeAsCredit() {
        Transaction transaction = completed(TransactionType.INCOME);

        transactionImpactService.apply(USER_ID, transaction);

        verify(accountBalanceService).credit(
                USER_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT
        );
    }

    @Test
    void shouldApplyCompletedExpenseAsDebit() {
        Transaction transaction = completed(TransactionType.EXPENSE);

        transactionImpactService.apply(USER_ID, transaction);

        verify(accountBalanceService).debit(
                USER_ID,
                SOURCE_ACCOUNT_ID,
                AMOUNT
        );
    }

    @Test
    void shouldApplyCompletedTransferInDebitThenCreditOrder() {
        Transaction transaction = completed(TransactionType.TRANSFER);

        transactionImpactService.apply(USER_ID, transaction);

        InOrder ordered = inOrder(accountBalanceService);
        ordered.verify(accountBalanceService).debit(
                USER_ID,
                SOURCE_ACCOUNT_ID,
                AMOUNT
        );
        ordered.verify(accountBalanceService).credit(
                USER_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT
        );
    }

    @Test
    void shouldReverseCompletedIncome() {
        Transaction transaction = completed(TransactionType.INCOME);

        transactionImpactService.reverse(USER_ID, transaction);

        verify(accountBalanceService).reverseCredit(
                USER_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT
        );
    }

    @Test
    void shouldReverseCompletedExpense() {
        Transaction transaction = completed(TransactionType.EXPENSE);

        transactionImpactService.reverse(USER_ID, transaction);

        verify(accountBalanceService).reverseDebit(
                USER_ID,
                SOURCE_ACCOUNT_ID,
                AMOUNT
        );
    }

    @Test
    void shouldReverseCompletedTransferInCreditThenDebitOrder() {
        Transaction transaction = completed(TransactionType.TRANSFER);

        transactionImpactService.reverse(USER_ID, transaction);

        InOrder ordered = inOrder(accountBalanceService);
        ordered.verify(accountBalanceService).reverseCredit(
                USER_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT
        );
        ordered.verify(accountBalanceService).reverseDebit(
                USER_ID,
                SOURCE_ACCOUNT_ID,
                AMOUNT
        );
    }

    @Test
    void shouldNotApplyOrReversePendingTransaction() {
        Transaction transaction = completed(TransactionType.EXPENSE);
        transaction.setStatus(TransactionStatus.PENDING);

        transactionImpactService.apply(USER_ID, transaction);
        transactionImpactService.reverse(USER_ID, transaction);

        verifyNoInteractions(accountBalanceService);
    }

    @Test
    void shouldRejectUnsupportedCompletedTransactionType() {
        Transaction transaction = completed(TransactionType.ADJUSTMENT);

        assertThatThrownBy(() -> transactionImpactService.apply(USER_ID, transaction))
                .isInstanceOf(InvalidTransactionException.class);

        assertThatThrownBy(() -> transactionImpactService.reverse(USER_ID, transaction))
                .isInstanceOf(InvalidTransactionException.class);
    }

    private Transaction completed(TransactionType type) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setAmount(AMOUNT);
        transaction.setSourceAccountId(SOURCE_ACCOUNT_ID);
        transaction.setDestinationAccountId(DESTINATION_ACCOUNT_ID);
        return transaction;
    }
}
