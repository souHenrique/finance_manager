package com.amorim.finance_manager.account.service;

import com.amorim.finance_manager.account.dto.AccountResponse;
import com.amorim.finance_manager.account.dto.CreateAccountRequest;
import com.amorim.finance_manager.account.dto.UpdateAccountRequest;
import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.mapper.AccountMapper;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.shared.exception.AccountNotFoundException;
import com.amorim.finance_manager.shared.exception.InvalidAccountUpdateException;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID ACCOUNT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccountWithCurrentBalanceEqualToInitialBalance() {
        BigDecimal initialBalance =
                new BigDecimal("1500.75");

        CreateAccountRequest request =
                new CreateAccountRequest(
                        "Checking Account",
                        AccountType.CHECKING,
                        "Example Bank",
                        initialBalance
                );

        Account account = new Account();
        account.setInitialBalance(initialBalance);

        AccountResponse response =
                new AccountResponse(
                        ACCOUNT_ID,
                        "Checking Account",
                        AccountType.CHECKING,
                        "Example Bank",
                        initialBalance,
                        initialBalance,
                        AccountStatus.ACTIVE,
                        0L,
                        null,
                        null
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(accountMapper.toEntity(request))
                .thenReturn(account);

        when(accountRepository.saveAndFlush(account))
                .thenReturn(account);

        when(accountMapper.toResponse(account))
                .thenReturn(response);

        AccountResponse result =
                accountService.create(request);

        assertThat(account.getUserId())
                .isEqualTo(USER_ID);

        assertThat(account.getInitialBalance())
                .isEqualByComparingTo("1500.75");

        assertThat(account.getCurrentBalance())
                .isEqualByComparingTo("1500.75");

        assertThat(account.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        assertThat(result)
                .isEqualTo(response);

        verify(currentUserService)
                .getCurrentUserId();

        verify(accountRepository)
                .saveAndFlush(account);

        verify(accountMapper)
                .toResponse(account);
    }

    @Test
    void shouldListOnlyAuthenticatedUserAccounts() {
        Account account = createAccount(
                ACCOUNT_ID,
                USER_ID,
                "Checking Account",
                new BigDecimal("500.00"),
                new BigDecimal("500.00")
        );

        AccountResponse response =
                new AccountResponse(
                        ACCOUNT_ID,
                        "Checking Account",
                        AccountType.CHECKING,
                        "Example Bank",
                        new BigDecimal("500.00"),
                        new BigDecimal("500.00"),
                        AccountStatus.ACTIVE,
                        0L,
                        null,
                        null
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(accountRepository
                .findAllByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(account));

        when(accountMapper.toResponse(account))
                .thenReturn(response);

        List<AccountResponse> result =
                accountService.findAll();

        assertThat(result)
                .containsExactly(response);

        verify(accountRepository)
                .findAllByUserIdOrderByCreatedAtDesc(USER_ID);
    }

    @Test
    void shouldFindAccountUsingIdAndUserId() {
        Account account = createAccount(
                ACCOUNT_ID,
                USER_ID,
                "Savings Account",
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00")
        );

        AccountResponse response =
                new AccountResponse(
                        ACCOUNT_ID,
                        "Savings Account",
                        AccountType.SAVINGS,
                        "Example Bank",
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        AccountStatus.ACTIVE,
                        0L,
                        null,
                        null
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(accountRepository
                .findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(account));

        when(accountMapper.toResponse(account))
                .thenReturn(response);

        AccountResponse result =
                accountService.findById(ACCOUNT_ID);

        assertThat(result)
                .isEqualTo(response);

        verify(accountRepository)
                .findByIdAndUserId(ACCOUNT_ID, USER_ID);

        verify(accountRepository, never())
                .findById(any(UUID.class));
    }

    @Test
    void shouldRejectAccountOwnedByAnotherUser() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(accountRepository
                .findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                accountService.findById(ACCOUNT_ID)
        )
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository)
                .findByIdAndUserId(ACCOUNT_ID, USER_ID);

        verify(accountMapper, never())
                .toResponse(any());
    }

    @Test
    void shouldUpdateOnlyAccountMetadata() {
        BigDecimal initialBalance =
                new BigDecimal("1200.50");

        BigDecimal currentBalance =
                new BigDecimal("950.25");

        Account account = createAccount(
                ACCOUNT_ID,
                USER_ID,
                "Old Name",
                initialBalance,
                currentBalance
        );

        UpdateAccountRequest request =
                new UpdateAccountRequest(
                        "New Name",
                        null,
                        "New Institution",
                        null
                );

        AccountResponse response =
                new AccountResponse(
                        ACCOUNT_ID,
                        "New Name",
                        AccountType.CHECKING,
                        "New Institution",
                        initialBalance,
                        currentBalance,
                        AccountStatus.ACTIVE,
                        1L,
                        null,
                        null
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(accountRepository
                .findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(account));

        doAnswer(invocation -> {
            account.setName(request.name());
            account.setInstitution(request.institution());
            return null;
        })
                .when(accountMapper)
                .updateEntity(request, account);

        when(accountRepository.saveAndFlush(account))
                .thenReturn(account);

        when(accountMapper.toResponse(account))
                .thenReturn(response);

        AccountResponse result =
                accountService.update(ACCOUNT_ID, request);

        assertThat(account.getName())
                .isEqualTo("New Name");

        assertThat(account.getInstitution())
                .isEqualTo("New Institution");

        assertThat(account.getInitialBalance())
                .isEqualByComparingTo("1200.50");

        assertThat(account.getCurrentBalance())
                .isEqualByComparingTo("950.25");

        assertThat(result)
                .isEqualTo(response);

        verify(accountRepository)
                .findByIdAndUserId(ACCOUNT_ID, USER_ID);

        verify(accountMapper)
                .updateEntity(request, account);

        verify(accountRepository)
                .saveAndFlush(account);
    }

    @Test
    void shouldKeepBalancesUnchangedWhenUpdatingMetadata() {
        Account account = createAccount(
                ACCOUNT_ID,
                USER_ID,
                "Account",
                new BigDecimal("2000.00"),
                new BigDecimal("1750.45")
        );

        BigDecimal originalInitialBalance =
                account.getInitialBalance();

        BigDecimal originalCurrentBalance =
                account.getCurrentBalance();

        UpdateAccountRequest request =
                new UpdateAccountRequest(
                        "Updated Account",
                        null,
                        null,
                        null
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(accountRepository
                .findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(account));

        doAnswer(invocation -> {
            account.setName(request.name());
            return null;
        })
                .when(accountMapper)
                .updateEntity(request, account);

        when(accountRepository.saveAndFlush(account))
                .thenReturn(account);

        accountService.update(ACCOUNT_ID, request);

        assertThat(account.getInitialBalance())
                .isEqualByComparingTo(originalInitialBalance);

        assertThat(account.getCurrentBalance())
                .isEqualByComparingTo(originalCurrentBalance);
    }

    @Test
    void shouldRejectEmptyUpdate() {
        UpdateAccountRequest request =
                new UpdateAccountRequest(
                        null,
                        null,
                        null,
                        null
                );

        assertThatThrownBy(() ->
                accountService.update(ACCOUNT_ID, request)
        )
                .isInstanceOf(InvalidAccountUpdateException.class);

        verifyNoInteractions(accountRepository);
        verifyNoInteractions(currentUserService);
        verifyNoInteractions(accountMapper);
    }

    private Account createAccount(
            UUID accountId,
            UUID userId,
            String name,
            BigDecimal initialBalance,
            BigDecimal currentBalance
    ) {
        Account account = new Account();

        account.setId(accountId);
        account.setUserId(userId);
        account.setName(name);
        account.setType(AccountType.CHECKING);
        account.setInstitution("Example Bank");
        account.setInitialBalance(initialBalance);
        account.setCurrentBalance(currentBalance);
        account.setStatus(AccountStatus.ACTIVE);
        account.setVersion(0L);

        return account;
    }
}
