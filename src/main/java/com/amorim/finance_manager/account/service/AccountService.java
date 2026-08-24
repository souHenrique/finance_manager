package com.amorim.finance_manager.account.service;

import com.amorim.finance_manager.account.dto.AccountResponse;
import com.amorim.finance_manager.account.dto.CreateAccountRequest;
import com.amorim.finance_manager.account.dto.UpdateAccountRequest;
import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.mapper.AccountMapper;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.shared.exception.AccountNotFoundException;
import com.amorim.finance_manager.shared.exception.InvalidAccountUpdateException;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        UUID userId = currentUserService.getCurrentUserId();

        Account account = accountMapper.toEntity(request);

        account.setUserId(userId);
        account.setCurrentBalance(request.initialBalance());
        account.setStatus(AccountStatus.ACTIVE);

        Account savedAccount = accountRepository.saveAndFlush(account);

        return accountMapper.toResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll() {
        UUID userId = currentUserService.getCurrentUserId();

        return accountRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse findById(UUID accountId) {
        Account account = findOwnedAccount(accountId);

        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse update(UUID accountId, UpdateAccountRequest request) {
        validateUpdate(request);

        Account account = findOwnedAccount(accountId);

        accountMapper.updateEntity(request, account);

        Account updatedAccount = accountRepository.saveAndFlush(account);

        return accountMapper.toResponse(updatedAccount);
    }

    private Account findOwnedAccount(UUID accountId) {
        UUID userId = currentUserService.getCurrentUserId();

        return accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);
    }

    private void validateUpdate(UpdateAccountRequest request) {
        if (request.name() == null
                && request.type() == null
                && request.institution() == null
                && request.status() == null) {
            throw new InvalidAccountUpdateException("Informe ao menos um campo para atualização");
        }

        if (request.name() != null && request.name().isBlank()) {
            throw new InvalidAccountUpdateException("Nome não pode ser vazio");
        }
    }
}
