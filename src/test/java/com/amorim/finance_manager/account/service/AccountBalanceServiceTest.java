package com.amorim.finance_manager.account.service;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.shared.exception.AccountBalanceConflictException;
import com.amorim.finance_manager.shared.exception.AccountNotFoundException;
import com.amorim.finance_manager.shared.exception.InactiveAccountException;
import com.amorim.finance_manager.shared.exception.InvalidBalanceAmountException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountBalanceServiceTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID ACCOUNT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountBalanceService accountBalanceService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(ACCOUNT_ID);
        account.setUserId(USER_ID);
        account.setCurrentBalance(new BigDecimal("100.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setVersion(0L);
    }

    @Test
    void shouldCreditAccountBalance() {
        mockExistingAccount();

        BigDecimal result = accountBalanceService.credit(
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("25.50")
        );

        assertThat(result)
                .isEqualByComparingTo("125.50");

        assertThat(account.getCurrentBalance())
                .isEqualByComparingTo("125.50");

        verify(accountRepository)
                .saveAndFlush(account);
    }

    @Test
    void shouldDebitAccountBalance() {
        mockExistingAccount();

        BigDecimal result = accountBalanceService.debit(
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("40.00")
        );

        assertThat(result)
                .isEqualByComparingTo("60.00");

        assertThat(account.getCurrentBalance())
                .isEqualByComparingTo("60.00");

        verify(accountRepository)
                .saveAndFlush(account);
    }

    @Test
    void shouldAllowNegativeBalance() {
        mockExistingAccount();

        BigDecimal result = accountBalanceService.debit(
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("150.00")
        );

        assertThat(result)
                .isEqualByComparingTo("-50.00");

        assertThat(account.getCurrentBalance())
                .isEqualByComparingTo("-50.00");

        verify(accountRepository)
                .saveAndFlush(account);
    }

    @Test
    void shouldReverseCreditBySubtractingAmount() {
        mockExistingAccount();

        BigDecimal result = accountBalanceService.reverseCredit(
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("25.00")
        );

        assertThat(result)
                .isEqualByComparingTo("75.00");

        assertThat(account.getCurrentBalance())
                .isEqualByComparingTo("75.00");
    }

    @Test
    void shouldReverseDebitByAddingAmount() {
        mockExistingAccount();

        BigDecimal result = accountBalanceService.reverseDebit(
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("25.00")
        );

        assertThat(result)
                .isEqualByComparingTo("125.00");

        assertThat(account.getCurrentBalance())
                .isEqualByComparingTo("125.00");
    }

    @Test
    void shouldRejectInvalidAmounts() {
        assertThatThrownBy(() ->
                accountBalanceService.credit(
                        USER_ID,
                        ACCOUNT_ID,
                        null
                )
        ).isInstanceOf(InvalidBalanceAmountException.class);

        assertThatThrownBy(() ->
                accountBalanceService.credit(
                        USER_ID,
                        ACCOUNT_ID,
                        BigDecimal.ZERO
                )
        ).isInstanceOf(InvalidBalanceAmountException.class);

        assertThatThrownBy(() ->
                accountBalanceService.credit(
                        USER_ID,
                        ACCOUNT_ID,
                        new BigDecimal("-1.00")
                )
        ).isInstanceOf(InvalidBalanceAmountException.class);

        assertThatThrownBy(() ->
                accountBalanceService.credit(
                        USER_ID,
                        ACCOUNT_ID,
                        new BigDecimal("10.001")
                )
        ).isInstanceOf(InvalidBalanceAmountException.class)
                .hasMessage(
                        "O valor deve possuir no máximo duas casas decimais"
                );

        verifyNoInteractions(accountRepository);
    }

    @Test
    void shouldThrowWhenAccountDoesNotExistOrBelongsToAnotherUser() {
        when(accountRepository.findByIdAndUserId(
                ACCOUNT_ID,
                USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                accountBalanceService.credit(
                        USER_ID,
                        ACCOUNT_ID,
                        new BigDecimal("10.00")
                )
        ).isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void shouldRejectNewMovementForInactiveAccount() {
        account.setStatus(AccountStatus.INACTIVE);

        when(accountRepository.findByIdAndUserId(
                ACCOUNT_ID,
                USER_ID
        )).thenReturn(Optional.of(account));

        assertThatThrownBy(() ->
                accountBalanceService.debit(
                        USER_ID,
                        ACCOUNT_ID,
                        new BigDecimal("10.00")
                )
        ).isInstanceOf(InactiveAccountException.class);

        verify(accountRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void shouldAllowReversalForInactiveAccount() {
        account.setStatus(AccountStatus.INACTIVE);

        mockExistingAccount();

        BigDecimal result = accountBalanceService.reverseDebit(
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("20.00")
        );

        assertThat(result)
                .isEqualByComparingTo("120.00");

        verify(accountRepository)
                .saveAndFlush(account);
    }

    @Test
    void shouldConvertOptimisticLockFailureToBalanceConflict() {
        when(accountRepository.findByIdAndUserId(
                ACCOUNT_ID,
                USER_ID
        )).thenReturn(Optional.of(account));

        ObjectOptimisticLockingFailureException originalException =
                new ObjectOptimisticLockingFailureException(
                        Account.class,
                        ACCOUNT_ID
                );

        when(accountRepository.saveAndFlush(account))
                .thenThrow(originalException);

        assertThatThrownBy(() ->
                accountBalanceService.credit(
                        USER_ID,
                        ACCOUNT_ID,
                        new BigDecimal("10.00")
                )
        )
                .isInstanceOf(AccountBalanceConflictException.class)
                .hasMessage(
                        "A conta foi atualizada por outra operação. Tente novamente."
                )
                .hasCause(originalException);
    }

    private void mockExistingAccount() {
        when(accountRepository.findByIdAndUserId(
                ACCOUNT_ID,
                USER_ID
        )).thenReturn(Optional.of(account));

        when(accountRepository.saveAndFlush(account))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
