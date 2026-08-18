package com.amorim.finance_manager.account.service;

import com.amorim.finance_manager.account.dto.AccountResponse;
import com.amorim.finance_manager.account.dto.CreateAccountRequest;
import com.amorim.finance_manager.account.dto.UpdateAccountRequest;
import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.mapper.AccountMapper;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.user.entity.User;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AccountService {

    private final AccountRepository repository;
    private final AccountMapper mapper;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, User user) {
        Account account = mapper.toEntity(request);
        account.setUser(user);
        return mapper.toResponse(repository.save(account));
    }

    public List<AccountResponse> listAccounts(UUID userId) {
        return repository.findAllByUserId(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public AccountResponse getAccountById(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
    }

    @Transactional
    public AccountResponse updateAccount(UUID id, UpdateAccountRequest request, UUID userId) {
        return repository.findByIdAndUserId(id, userId)
                .map(account -> {
                    mapper.updateEntity(request, account);
                    return repository.save(account);
                })
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
    }

    @Transactional
    public void deactivateAccount(UUID id, UUID userId) {
        repository.findByIdAndUserId(id, userId)
                .ifPresentOrElse(
                        account -> {
                            account.setActive(false);
                            repository.save(account);
                        },
                        () -> {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada.");
                        }
                );
    }
}
