package com.amorim.finance_manager.account.controller;

import com.amorim.finance_manager.account.dto.AccountResponse;
import com.amorim.finance_manager.account.dto.CreateAccountRequest;
import com.amorim.finance_manager.account.dto.UpdateAccountRequest;
import com.amorim.finance_manager.account.dto.UpdateAccountStatusRequest;
import com.amorim.finance_manager.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(accountService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> findAll() {
        return ResponseEntity.ok(accountService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AccountResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        return ResponseEntity.ok(accountService.updateStatus(id, request.status()));
    }
}
