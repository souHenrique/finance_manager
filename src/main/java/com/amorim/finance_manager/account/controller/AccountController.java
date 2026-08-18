package com.amorim.finance_manager.account.controller;

import com.amorim.finance_manager.account.dto.AccountResponse;
import com.amorim.finance_manager.account.dto.CreateAccountRequest;
import com.amorim.finance_manager.account.dto.UpdateAccountRequest;
import com.amorim.finance_manager.account.service.AccountService;
import com.amorim.finance_manager.user.entity.User;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@AllArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody @Valid CreateAccountRequest request,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAccount(request, user));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listResponse(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.listAccounts(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getAccountById(id, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id,
                                                  @RequestBody @Valid UpdateAccountRequest request,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.updateAccount(id, request, user.getId()));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        service.deactivateAccount(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
