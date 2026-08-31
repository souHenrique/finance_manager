package com.amorim.finance_manager.account.service;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.shared.exception.AccountBalanceConflictException;
import com.amorim.finance_manager.shared.exception.AccountNotFoundException;
import com.amorim.finance_manager.shared.exception.InvalidBalanceAmountException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountBalanceService {

    private final AccountRepository accountRepository;

    public BigDecimal credit(
            UUID userId,
            UUID accountId,
            BigDecimal amount
    ) {
        return changeBalance(
                userId,
                accountId,
                normalizeAmount(amount),
                true
        );
    }

    public BigDecimal debit(
            UUID userId,
            UUID accountId,
            BigDecimal amount
    ) {
        return changeBalance(
                userId,
                accountId,
                normalizeAmount(amount).negate(),
                true
        );
    }

    public BigDecimal reverseCredit(
            UUID userId,
            UUID accountId,
            BigDecimal amount
    ) {
        return changeBalance(
                userId,
                accountId,
                normalizeAmount(amount).negate(),
                false
        );
    }

    public BigDecimal reverseDebit(
            UUID userId,
            UUID accountId,
            BigDecimal amount
    ) {
        return changeBalance(
                userId,
                accountId,
                normalizeAmount(amount),
                false
        );
    }

    private BigDecimal changeBalance(
            UUID userId,
            UUID accountId,
            BigDecimal delta,
            boolean requireActiveAccount
    ) {
        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        if (requireActiveAccount) {
            account.ensureActive();
        }

        BigDecimal newBalance =
                account.getCurrentBalance().add(delta);

        account.setCurrentBalance(newBalance);

        try {
            Account updatedAccount =
                    accountRepository.saveAndFlush(account);

            return updatedAccount.getCurrentBalance();
        } catch (OptimisticLockingFailureException exception) {
            throw new AccountBalanceConflictException(exception);
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidBalanceAmountException(
                    "O valor da movimentação deve ser maior que zero"
            );
        }

        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidBalanceAmountException(
                    "O valor deve possuir no máximo duas casas decimais"
            );
        }
    }
}
