package com.amorim.finance_manager.transaction.controller;

import com.amorim.finance_manager.transaction.dto.CreateTransactionRequest;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.dto.UpdateTransactionRequest;
import com.amorim.finance_manager.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        return ResponseEntity.ok(transactionService.update(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransactionResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.cancel(id));
    }
}
